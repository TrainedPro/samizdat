#!/usr/bin/env python3
"""
Kotlin Class Structure Extractor & Diagram Generator

This script parses Kotlin source code to generate a Mermaid Class Diagram.
It adheres to strict UML standards:
  - Inheritance (<|--)
  - Realization (<|..)
  - Composition (*--) for Data Classes and Entities
  - Aggregation (o--) for Collections
  - Association (-->) for standard references
  - Dependency (..>) for method parameters/returns

Usage:
    python3 scripts/extract_structure.py [source_dir] [output_dir]

    Example:
    python3 scripts/extract_structure.py app/src/main/java documents/diagrams

Requirements:
    - Python 3+
    - Node.js & npm (for PDF generation via mermaid-cli)
    - Internet connection (to fetch mmdc via npx)

Notes:
    - This script automates that generation of the PDF using `npx`.
    - It creates and cleans up temporary config files (`puppeteer-config.json`).
    - The final output will be a PDF and a .mermaid file in the output directory.
"""

import os
import re
import sys
import json
import shutil
import subprocess
from collections import defaultdict
import argparse

# --- Data Structures ---

class ClassInfo:
    def __init__(self, name, kind, package):
        self.name = name
        self.kind = kind # class, interface, object
        self.package = package
        self.parents = [] 
        self.properties = [] # (vis, name, type)
        self.methods = [] # (vis, name, args, return_type)
        self.stereotypes = []
        self.is_data_class = False
        self.is_entity = False

    def __repr__(self):
        return f"<{self.kind} {self.name}>"

# --- Globals ---

classes_registry = {} 

# --- Regex Patterns ---

package_pattern = re.compile(r'^package\s+([a-zA-Z0-9_.]+)')
class_start_pattern = re.compile(r'\b(abstract|open|enum|data|sealed|inner)?\s*(class|interface|object)\s+([a-zA-Z0-9_]+)')
prop_line_pattern = re.compile(r'\b(val|var)\s+([a-zA-Z0-9_]+)\s*(?::\s*([^=]+))?(?:\s*=\s*(.*))?')
method_pattern = re.compile(r'\b(fun)\s+([a-zA-Z0-9_]+)\s*\(([^)]*)\)\s*(?::\s*([^{=]+))?')
annotation_pattern = re.compile(r'@([a-zA-Z0-9_]+)')

def get_visibility(text):
    if "private " in text: return "-"
    if "protected " in text: return "#"
    if "internal " in text: return "~"
    return "+"

def clean_type(t):
    if not t: return "Any"
    return t.strip()

def infer_type(value_str):
    if not value_str: return "Any"
    val = value_str.strip()
    if val == "true" or val == "false": return "Boolean"
    if val.startswith('"'): return "String"
    if val.endswith('L') and val[:-1].isdigit(): return "Long"
    if val.isdigit(): return "Int"
    if "listOf" in val: return "List"
    if "mapOf" in val: return "Map"
    if "mutableListOf" in val: return "MutableList"
    if "mutableMapOf" in val: return "MutableMap"
    ctor_match = re.match(r'([A-Z][a-zA-Z0-9_.]*(?:<.+>)?)(\(.*\))?', val)
    if ctor_match:
         return ctor_match.group(1)
    if "List" in val: return "List"
    if "Map" in val: return "Map"
    return "Any"

# --- Parsing Logic ---

def parse_file(file_path):
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
    except Exception as e:
        sys.stderr.write(f"Error reading {file_path}: {e}\n")
        return

    content = re.sub(r'/\*.*?\*/', '', content, flags=re.DOTALL)
    lines = content.split('\n')
    package_name = "default"
    class_stack = [] 
    current_brace_level = 0
    pending_annotations = []
    
    i = 0
    while i < len(lines):
        line = lines[i]
        line_stripped = line.strip()
        if "//" in line:
             line_stripped = line.split("//")[0].strip()
        if not line_stripped:
            i += 1
            continue

        open_braces = line_stripped.count('{')
        close_braces = line_stripped.count('}')
        
        pkg_match = package_pattern.search(line_stripped)
        if pkg_match:
            package_name = pkg_match.group(1)
            i += 1
            continue
            
        if line_stripped.startswith("@"):
            anns = annotation_pattern.findall(line_stripped)
            pending_annotations.extend(anns)
        
        class_match = class_start_pattern.search(line_stripped)
        if class_match:
            mod, kind, name = class_match.groups()
            new_class = ClassInfo(name, kind, package_name)
            classes_registry[name] = new_class
            
            # Stereotypes & Flags
            for ann in set(pending_annotations):
                 if ann in ["Entity", "Dao", "Database"]:
                     new_class.stereotypes.append(ann)
                     if ann == "Entity": new_class.is_entity = True
            
            if "@" in line_stripped:
                 anns = annotation_pattern.findall(line_stripped)
                 for ann in anns:
                     if ann in ["Entity", "Dao", "Database"] and ann not in new_class.stereotypes:
                         new_class.stereotypes.append(ann)
                         if ann == "Entity": new_class.is_entity = True
            
            pending_annotations = []
            if mod:
                if "enum" in mod: new_class.stereotypes.append("enumeration")
                if "data" in mod: 
                    new_class.stereotypes.append("data")
                    new_class.is_data_class = True
                if "abstract" in mod: new_class.stereotypes.append("abstract")
                if "sealed" in mod: new_class.stereotypes.append("sealed")
            if "interface" in kind:
                new_class.stereotypes.append("interface")

            if ":" in line_stripped:
                parts = line_stripped.split(":")
                if len(parts) > 1:
                    inheritance_part = parts[-1].split('{')[0].strip()
                    parents = [p.strip().split('(')[0] for p in inheritance_part.split(',')]
                    for p in parents:
                         if p and p != "constructor": new_class.parents.append(p)
            
            # Multi-line Primary Constructor Support
            full_sig = line_stripped
            if '(' in full_sig and "constructor" not in full_sig: 
                bracket_count = full_sig.count('(') - full_sig.count(')')
                j = i + 1
                while bracket_count > 0 and j < len(lines):
                    next_line = lines[j].strip()
                    full_sig += " " + next_line
                    bracket_count += next_line.count('(') - next_line.count(')')
                    j += 1
                
                try:
                    start = full_sig.find('(') + 1
                    end = full_sig.rfind(')')
                    if start < end:
                        args_str = full_sig[start:end]
                        safe_args = []
                        current_arg = ""
                        b_count = 0
                        for char in args_str:
                            if char == '<' or char == '(': b_count += 1
                            elif char == '>' or char == ')': b_count -= 1
                            if char == ',' and b_count == 0:
                                safe_args.append(current_arg)
                                current_arg = ""
                            else:
                                current_arg += char
                        safe_args.append(current_arg)
                        
                        for arg in safe_args:
                            arg = arg.strip()
                            if 'val ' in arg or 'var ' in arg:
                                p_match = prop_line_pattern.search(arg)
                                if p_match:
                                    _, p_name, p_type, _ = p_match.groups()
                                    p_type = clean_type(p_type)
                                    vis = get_visibility(arg)
                                    new_class.properties.append((vis, p_name, p_type))
                except: pass

            class_stack.append({'info': new_class, 'level': current_brace_level})
        
        if class_stack:
            active_class = class_stack[-1]['info']
            if ('val ' in line_stripped or 'var ' in line_stripped) and not class_match:
                 prop_match = prop_line_pattern.search(line_stripped)
                 if prop_match:
                     _, p_name, p_type_hint, p_val = prop_match.groups()
                     p_type = clean_type(p_type_hint) if p_type_hint else infer_type(p_val)
                     vis = get_visibility(line_stripped)
                     active_class.properties.append((vis, p_name, p_type))
            if 'fun ' in line_stripped:
                method_match = method_pattern.search(line_stripped)
                if method_match:
                     _, m_name, m_args, m_ret = method_match.groups()
                     m_ret = m_ret.strip() if m_ret else "Unit"
                     active_class.methods.append((get_visibility(line_stripped), m_name, m_args.strip(), m_ret))
        
        current_brace_level += (open_braces - close_braces)
        while class_stack and current_brace_level <= class_stack[-1]['level']:
             class_stack.pop()
             
        i += 1

# --- Generation ---

def generate_mermaid_content():
    lines = []
    lines.append("%%{init: {'theme': 'neutral'} }%%")
    lines.append("classDiagram")
    lines.append("    direction LR")
    
    relations = {} 
    
    def add_relation(source, target, rel_type, priority):
        if source == target: return # No self-reference
        if target in ["String", "Int", "Boolean", "Long", "Float", "Double", "Unit", "Any", "List", "Map", "MutableList", "MutableMap", "Context", "Intent", "Bundle"]:
             return # Exclude primitives/platform types unless we decide otherwise
        
        key = (source, target)
        if key in relations:
            existing_type, existing_prio = relations[key]
            if priority > existing_prio:
                relations[key] = (rel_type, priority)
        else:
            relations[key] = (rel_type, priority)

    packages = defaultdict(list)
    for cls in classes_registry.values():
        packages[cls.package].append(cls)
        
    for pkg, classes in packages.items():
        lines.append(f"namespace {pkg} {{")
        for cls in classes:
            lines.append(f"    class {cls.name} {{")
            for st in cls.stereotypes:
                lines.append(f"        <<{st}>>")
            for vis, name, type_ in cls.properties:
                lines.append(f"        {vis}{name} : {type_}")
            for vis, name, args, ret in cls.methods:
                lines.append(f"        {vis}{name}({args}) {ret}")
            lines.append("    }")
        lines.append("}")
        
    for cls in classes_registry.values():
        # 1. Inheritance / Realization (Priority 5)
        for parent in cls.parents:
            if parent in classes_registry:
                parent_info = classes_registry[parent]
                if parent_info.kind == "interface":
                     add_relation(cls.name, parent, "<|..", 5) # Realization
                else:
                     add_relation(cls.name, parent, "<|--", 5) # Inheritance
        
        # 2. Properties (Composition / Aggregation / Association)
        for vis, name, type_ in cls.properties:
            potential_references = re.findall(r'([a-zA-Z0-9_]+)', type_)
            for ref in potential_references:
                if ref in classes_registry and ref != cls.name:
                    # Decide Type
                    if cls.is_data_class or cls.is_entity:
                        # Priority 4: Composition
                        add_relation(cls.name, ref, "*--", 4)
                    elif "List" in type_ or "Map" in type_ or "Array" in type_ or "Set" in type_:
                        # Priority 3: Aggregation
                        add_relation(cls.name, ref, "o--", 3)
                    else:
                        # Priority 2: Association
                        add_relation(cls.name, ref, "-->", 2)
        
        # 3. Methods (Dependency) (Priority 1)
        for vis, name, args, ret in cls.methods:
             # Return types
             potential_references = re.findall(r'([a-zA-Z0-9_]+)', ret)
             for ref in potential_references:
                if ref in classes_registry and ref != cls.name:
                    add_relation(cls.name, ref, "..>", 1)
             
             # Arg types
             potential_references_args = re.findall(r'([a-zA-Z0-9_]+)', args)
             for ref in potential_references_args:
                if ref in classes_registry and ref != cls.name:
                    add_relation(cls.name, ref, "..>", 1)

    # Print relationships
    for (source, target), (rel_type, priority) in sorted(relations.items()):
        if "<|" in rel_type: # Reverse for inheritance: Parent <|-- Child
             lines.append(f"    {target} {rel_type} {source}")
        else:
             lines.append(f"    {source} {rel_type} {target}")
             
    return "\n".join(lines)

def run_pdf_generation(mermaid_file, output_pdf):
    print(f"Generating PDF: {output_pdf}...")
    
    # Create temp config for sandbox
    config_file = "puppeteer-config-temp.json"
    with open(config_file, "w") as f:
        json.dump({"args": ["--no-sandbox"]}, f)
    
    try:
        cmd = [
            "npx", "-p", "@mermaid-js/mermaid-cli", "mmdc",
            "-i", mermaid_file,
            "-o", output_pdf,
            "--pdfFit",
            "-p", config_file
        ]
        subprocess.run(cmd, check=True)
        print("PDF Generation Successful.")
    except subprocess.CalledProcessError as e:
        print(f"Error generating PDF: {e}")
    except FileNotFoundError:
        print("Error: 'npx' not found. Please install Node.js/npm.")
    finally:
        if os.path.exists(config_file):
            os.remove(config_file)

def main():
    parser = argparse.ArgumentParser(description="Extract Kotlin Class Structure and Generate Mermaid Diagram")
    parser.add_argument("src", nargs="?", default="app/src/main/java", help="Source directory containing Kotlin files")
    parser.add_argument("out", nargs="?", default="documents/diagrams", help="Output directory for diagrams")
    parser.add_argument("--no-pdf", action="store_true", help="Skip PDF generation")
    
    args = parser.parse_args()
    
    src_dir = args.src
    out_dir = args.out
    
    if not os.path.exists(src_dir):
        print(f"Source directory not found: {src_dir}")
        sys.exit(1)
        
    print(f"Scanning {src_dir}...")
    for root, dirs, files in os.walk(src_dir):
        for file in files:
            if file.endswith(".kt"):
                parse_file(os.path.join(root, file))
    
    mermaid_content = generate_mermaid_content()
    
    if not os.path.exists(out_dir):
        os.makedirs(out_dir)
        
    mermaid_file = os.path.join(out_dir, "class_structure.mermaid")
    pdf_file = os.path.join(out_dir, "class_structure.pdf")
    
    with open(mermaid_file, "w", encoding="utf-8") as f:
        f.write(mermaid_content)
    print(f"Mermaid file saved to: {mermaid_file}")
    
    if not args.no_pdf:
        run_pdf_generation(mermaid_file, pdf_file)

if __name__ == "__main__":
    main()
