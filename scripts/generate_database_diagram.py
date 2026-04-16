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
    # Complete schema: AppDatabase v9 — 9 entities
    return """erDiagram
    logs {
        Long id PK "autoGenerate"
        Long timestamp "Indexed"
        String message
        LogLevel level "enum"
        LogType logType "enum"
        String peerId "nullable"
        Int rssi "nullable"
        Long latencyMs "nullable"
        Int payloadSizeBytes "nullable"
    }

    packet_queue {
        String id PK "UUID dedup"
        String destId "Indexed"
        String type "PacketType name"
        ByteArray payload
        Long timestamp
        Long expiration "Indexed"
        String sourceId
    }

    chat_messages {
        Long id PK "autoGenerate"
        Long timestamp "Indexed"
        String peerId "Indexed"
        Boolean isOutgoing
        MessageType type "enum TEXT IMAGE FILE SYSTEM"
        String text "nullable"
        String fileName "nullable"
        String filePath "nullable"
        String mimeType "nullable"
        Long fileSize
        Int transferProgress "0-100 or -1"
        Boolean isBroadcast
    }

    chat_groups {
        String groupId PK "UUID"
        String name "Unique Index"
        String createdBy
        Long createdAt
        String members "comma-separated"
    }

    group_messages {
        Long id PK "autoGenerate"
        String groupId FK "Indexed"
        String senderName
        String text
        Long timestamp "Indexed"
        String packetId "Unique Index"
    }

    telemetry_events {
        Long id PK "autoGenerate"
        Long timestamp "Indexed"
        String deviceId
        TelemetryEventType eventType "enum"
        String payload "JSON"
        Boolean uploaded "Indexed"
        Int uploadAttempts
    }

    shared_files {
        String sha256 PK "SHA-256 hash"
        String fileName
        String mimeType
        Long fileSize
        Int chunkSize "32KB default"
        Int totalChunks
        String sharedBy "Indexed"
        Long announcedAt "Indexed"
        String localPath "nullable"
        Int downloadedChunks
    }

    downloaded_chunks {
        String sha256 PK "composite FK"
        Int chunkIndex PK "composite"
    }

    encounter_log {
        Long id PK "autoGenerate"
        String localPeer
        String remotePeer "Indexed"
        Long startTime "Indexed"
        Long endTime
        Int packetsExchanged
        Long bytesExchanged
        Int rssi
    }

    %% Enforced relationships
    shared_files ||--o{ downloaded_chunks : "sha256"
    chat_groups ||--o{ group_messages : "groupId"

    %% Application-level associations (not FK-enforced)
    chat_messages }o--|| packet_queue : "peerId (implicit)"
    encounter_log }o--|| logs : "peer references (implicit)"
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
    base_dir = "documents/diagrams"
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
