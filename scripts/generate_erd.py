#!/usr/bin/env python3
"""
ER Diagram Generator for ResilientP2PTestbed
Generates a Mermaid ERD for the Room Database schema.
"""

import os
import subprocess
import json
import sys

def generate_erd_content():
    # Manual definition based on AppDatabase.kt, LogEntry.kt, PacketEntity.kt
    return """erDiagram
    LOGS {
        long id PK
        long timestamp
        string type
        string peerId
        string message
        int rssi
        long latencyMs
        int payloadSizeBytes
    }

    PACKET_QUEUE {
        string id PK
        string destId
        string type
        byte_array payload
        long timestamp
        long expiration
        string sourceId
    }

    %% Relationships can be implied or explicit if any
    %% In this simple schema, tables are independent queues/logs
"""

def run_pdf_generation(mermaid_file, output_pdf):
    print(f"Generating PDF: {output_pdf}...")
    
    config_file = "puppeteer-config-erd-temp.json"
    with open(config_file, "w") as f:
        json.dump({"args": ["--no-sandbox"]}, f)
    
    try:
        cmd = [
            "npx", "-p", "@mermaid-js/mermaid-cli", "mmdc",
            "-i", mermaid_file,
            "-o", output_pdf,
            "--pdfFit",
            "-p", config_file,
            "-t", "neutral"
        ]
        subprocess.run(cmd, check=True)
        print("ERD PDF Generation Successful.")
    except subprocess.CalledProcessError as e:
        print(f"Error generating PDF: {e}")
    except FileNotFoundError:
        print("Error: 'npx' not found.")
    finally:
        if os.path.exists(config_file):
            os.remove(config_file)

def main():
    base_dir = "diagrams"
    source_dir = os.path.join(base_dir, "source")
    output_dir = os.path.join(base_dir, "output")

    if not os.path.exists(source_dir): os.makedirs(source_dir)
    if not os.path.exists(output_dir): os.makedirs(output_dir)

    mermaid_file = os.path.join(source_dir, "database_schema.mermaid")
    pdf_file = os.path.join(output_dir, "database_schema.pdf")

    with open(mermaid_file, "w") as f:
        f.write(generate_erd_content())
    
    run_pdf_generation(mermaid_file, pdf_file)

if __name__ == "__main__":
    main()
