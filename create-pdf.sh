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
cat > "$TEMP_FILE" << 'EOF'
---
title: "Apache Ignite 3 Java API Primer"
subtitle: "A Comprehensive Guide to Distributed Data Programming"
author: "Apache Ignite Community"
date: "`date +%Y-%m-%d`"
geometry: margin=1in
fontsize: 11pt
mainfont: "Times New Roman"
monofont: "Courier New"
colorlinks: true
linkcolor: blue
urlcolor: blue
toccolor: black
---

\newpage
\tableofcontents
\newpage

EOF

# Add main introduction
echo ">>> Adding main documentation overview..."
cat README.md >> "$TEMP_FILE"
echo -e "\n\n\\newpage\n" >> "$TEMP_FILE"

# Module 00 - Reference (Architecture overview first)
echo ">>> Adding Module 00: Reference..."
echo -e "# Module 00: Reference Architecture\n" >> "$TEMP_FILE"
cat 00-reference/README.md >> "$TEMP_FILE"
echo -e "\n\n" >> "$TEMP_FILE"

for file in 00-reference/IGNITE3-ARCH.md 00-reference/JAVA-API-ARCH.md 00-reference/SQL-ENGINE-ARCH.md 00-reference/STORAGE-SYSTEM-ARCH.md 00-reference/TECHNICAL_FEATURES.md; do
    if [ -f "$file" ]; then
        echo ">>> Adding $file..."
        cat "$file" >> "$TEMP_FILE"
        echo -e "\n\n\\newpage\n" >> "$TEMP_FILE"
    fi
done

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

echo "--- Generating PDF with Pandoc..."

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