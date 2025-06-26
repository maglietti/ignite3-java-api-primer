#!/bin/bash

# Apache Ignite 3 Java API Primer - PDF Assembly Script
# Creates a single PDF from all documentation modules

set -e

echo "=== Apache Ignite 3 Java API Primer PDF Generation ==="

# Change to docs directory
cd docs

# Create temporary combined markdown file
TEMP_FILE="../combined-book.md"
OUTPUT_PDF="../ignite3-java-api-primer.pdf"

echo "--- Assembling markdown files..."

# Start with title page and main README
cat > "$TEMP_FILE" << EOF
---
title: "Apache Ignite 3 Java API Primer"
subtitle: "Practical Patterns for Distributed Data Programming"
author: "Apache Ignite Community"
date: "$(date +%Y-%m-%d)"
geometry: margin=1in
fontsize: 11pt
mainfont: "Times New Roman"
monofont: "Courier New"
colorlinks: true
linkcolor: blue
urlcolor: blue
toccolor: black
---

\\newpage
\\tableofcontents
\\newpage

EOF

# Add main introduction
echo ">>> Adding main documentation overview..."
cat README.md >> "$TEMP_FILE"
echo -e "\n\n\\newpage\n" >> "$TEMP_FILE"

# Start with learning modules as main content

# Module 01 - Foundation
echo ">>> Adding Module 01: Foundation..."
echo -e "# Module 01: Foundation\n" >> "$TEMP_FILE"
cat 01-foundation/README.md >> "$TEMP_FILE"
echo -e "\n\n" >> "$TEMP_FILE"

for file in 01-foundation/01-introduction-and-architecture.md 01-foundation/02-getting-started.md 01-foundation/03-distributed-data-fundamentals.md; do
    if [ -f "$file" ]; then
        echo ">>> Adding $file..."
        cat "$file" >> "$TEMP_FILE"
        echo -e "\n\n\\newpage\n" >> "$TEMP_FILE"
    fi
done

# Module 02 - Schema Design
echo ">>> Adding Module 02: Schema Design..."
echo -e "# Module 02: Schema Design\n" >> "$TEMP_FILE"
cat 02-schema-design/README.md >> "$TEMP_FILE"
echo -e "\n\n" >> "$TEMP_FILE"

for file in 02-schema-design/01-basic-annotations.md 02-schema-design/02-relationships-and-colocation.md 02-schema-design/03-advanced-annotations.md 02-schema-design/04-schema-evolution.md; do
    if [ -f "$file" ]; then
        echo ">>> Adding $file..."
        cat "$file" >> "$TEMP_FILE"
        echo -e "\n\n\\newpage\n" >> "$TEMP_FILE"
    fi
done

# Module 03 - Data Access APIs
echo ">>> Adding Module 03: Data Access APIs..."
echo -e "# Module 03: Data Access APIs\n" >> "$TEMP_FILE"
cat 03-data-access-apis/README.md >> "$TEMP_FILE"
echo -e "\n\n" >> "$TEMP_FILE"

for file in 03-data-access-apis/01-table-api-operations.md 03-data-access-apis/02-sql-api-analytics.md 03-data-access-apis/03-sql-api-selection-guide.md; do
    if [ -f "$file" ]; then
        echo ">>> Adding $file..."
        cat "$file" >> "$TEMP_FILE"
        echo -e "\n\n\\newpage\n" >> "$TEMP_FILE"
    fi
done

# Module 04 - Distributed Operations
echo ">>> Adding Module 04: Distributed Operations..."
echo -e "# Module 04: Distributed Operations\n" >> "$TEMP_FILE"
cat 04-distributed-operations/README.md >> "$TEMP_FILE"
echo -e "\n\n" >> "$TEMP_FILE"

for file in 04-distributed-operations/01-transaction-fundamentals.md 04-distributed-operations/02-advanced-transaction-patterns.md 04-distributed-operations/03-compute-api-processing.md; do
    if [ -f "$file" ]; then
        echo ">>> Adding $file..."
        cat "$file" >> "$TEMP_FILE"
        echo -e "\n\n\\newpage\n" >> "$TEMP_FILE"
    fi
done

# Module 05 - Performance and Scalability
echo ">>> Adding Module 05: Performance and Scalability..."
echo -e "# Module 05: Performance and Scalability\n" >> "$TEMP_FILE"
cat 05-performance-scalability/README.md >> "$TEMP_FILE"
echo -e "\n\n" >> "$TEMP_FILE"

for file in 05-performance-scalability/01-data-streaming.md 05-performance-scalability/02-caching-strategies.md 05-performance-scalability/03-query-performance.md; do
    if [ -f "$file" ]; then
        echo ">>> Adding $file..."
        cat "$file" >> "$TEMP_FILE"
        echo -e "\n\n\\newpage\n" >> "$TEMP_FILE"
    fi
done

# Appendix - Reference Documentation
echo ">>> Adding Reference Documentation as Appendix..."
echo -e "\\appendix\n" >> "$TEMP_FILE"
echo -e "# Reference Architecture\n" >> "$TEMP_FILE"
cat 00-reference/README.md >> "$TEMP_FILE"
echo -e "\n\n" >> "$TEMP_FILE"

for file in 00-reference/IGNITE3-ARCH.md 00-reference/JAVA-API-ARCH.md 00-reference/SQL-ENGINE-ARCH.md 00-reference/STORAGE-SYSTEM-ARCH.md 00-reference/TECHNICAL_FEATURES.md; do
    if [ -f "$file" ]; then
        echo ">>> Adding $file..."
        cat "$file" >> "$TEMP_FILE"
        echo -e "\n\n\\newpage\n" >> "$TEMP_FILE"
    fi
done

echo "--- Generating PDF with Pandoc..."

# Remove emojis and style violations to comply with writing guidelines
echo ">>> Filtering emojis and style violations..."
sed -i '' -E 's/[✅❌⚠️✓✗📝🎵💰📊📈🎭🚀🎉]/[X]/g' "$TEMP_FILE"
sed -i '' -E 's/comprehensive/practical/gi' "$TEMP_FILE"
sed -i '' -E 's/simplified/streamlined/gi' "$TEMP_FILE"

# Remove any control characters that might cause LaTeX issues
echo ">>> Cleaning control characters..."
tr -d '\000-\010\013\014\016-\037' < "$TEMP_FILE" > "$TEMP_FILE.clean"
mv "$TEMP_FILE.clean" "$TEMP_FILE"

# Convert markdown links to LaTeX cross-references
echo ">>> Converting internal links..."
sed -i '' -E 's/\[([^]]+)\]\(\.\/([^)]+)\.md\)/\1 (see Section \\ref{\2})/g' "$TEMP_FILE"
sed -i '' -E 's/\[([^]]+)\]\(([^)]+)\.md\)/\1 (see Section \\ref{\2})/g' "$TEMP_FILE"

# Process mermaid diagrams if CLI is available
echo ">>> Checking for mermaid diagram support..."
if command -v mmdc >/dev/null 2>&1; then
    echo ">>> Converting mermaid diagrams to images..."
    
    # Create temp directory for mermaid images
    MERMAID_DIR="../mermaid-images"
    mkdir -p "$MERMAID_DIR"
    
    # Extract mermaid blocks and convert to images
    diagram_count=0
    temp_output="$TEMP_FILE.mermaid_processed"
    
    # Use awk to process mermaid blocks
    awk '
    BEGIN { in_mermaid = 0; diagram_count = 0 }
    /^```mermaid/ {
        in_mermaid = 1
        diagram_count++
        diagram_file = "'$MERMAID_DIR'/diagram_" diagram_count ".mmd"
        image_file = "'$MERMAID_DIR'/diagram_" diagram_count ".png"
        system("rm -f " diagram_file)
        print "![Diagram " diagram_count "](" image_file ")"
        next
    }
    /^```$/ && in_mermaid {
        in_mermaid = 0
        # Convert mermaid to PNG using system Chrome
        cmd = "PUPPETEER_EXECUTABLE_PATH=\"/Applications/Google Chrome.app/Contents/MacOS/Google Chrome\" mmdc -i " diagram_file " -o " image_file " -b white -t neutral 2>/dev/null"
        if (system(cmd) != 0) {
            print "    Warning: Failed to convert diagram " diagram_count > "/dev/stderr"
        }
        next
    }
    in_mermaid {
        print $0 >> diagram_file
        next
    }
    { print }
    ' "$TEMP_FILE" > "$temp_output"
    
    mv "$temp_output" "$TEMP_FILE"
    echo "    Converted $diagram_count mermaid diagrams"
else
    echo "!!! Mermaid diagrams will appear as code blocks"
    echo "!!! Install mermaid-cli for diagram rendering: npm install -g @mermaid-js/mermaid-cli"
fi

# Generate PDF using Pandoc with XeLaTeX
pandoc "$TEMP_FILE" \
    -f markdown \
    -t pdf \
    -o "$OUTPUT_PDF" \
    --pdf-engine=xelatex \
    --toc \
    --toc-depth=3 \
    --number-sections \
    --highlight-style=tango \
    --variable=geometry:margin=1in \
    --variable=fontsize:11pt \
    --variable=documentclass:book \
    --variable=classoption:openany

# Clean up temporary file
rm "$TEMP_FILE"

echo "<<< PDF created successfully: $OUTPUT_PDF"
echo "<<< File size: $(ls -lh "$OUTPUT_PDF" | awk '{print $5}')"

cd ..