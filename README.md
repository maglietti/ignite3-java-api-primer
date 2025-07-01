# Apache Ignite 3 Java API Primer

Learn Apache Ignite 3's Java API through structured learning modules and working code examples. This primer covers distributed data management, schema design, transactions, and performance patterns. All examples use a music store dataset for consistency across modules.

## Target Audience

Java developers new to Ignite 3 interested in learning high velocity distributed data management patterns through practical, runnable examples.

## Quick Start

**Prerequisites**: Java 17+, Maven 3.8+, Docker

- **Start Ignite 3 Cluster**:

```bash
cd ignite3-reference-apps/00-docker
./init-cluster.sh
```

- **Initialize Sample Data**:

```bash
cd ../01-sample-data-setup
mvn compile exec:java
```

- **Start Learning**: Follow the structured learning path below or jump to specific topics.

## IDE Setup (Optional)

> [!NOTE]
> All reference applications run from command line using Maven. IDE setup is provided for developers who prefer integrated development environments.

### IntelliJ IDEA

**Import Project**:

1. File → New → Select project root directory
2. Choose "Import project from external model" → Maven
3. Accept default Maven settings and wait for indexing to complete

Alternatively, the project can be imported directly from git: New -> Project From Version Control if you do not want to clone the project locally.

**Configure JDK**:

- File → Project Structure → Project Settings → Project
- Set Project SDK to Java 17+
- Set Project language level to 17

**Run Reference Applications**:

- Each reference app has a main class (e.g., `TableAPIDemo.java`, `SQLAPIDemo.java`)
- Navigate to the main class in Project Explorer
- Right-click main class → Run to create run configuration
- Verify working directory is set to the specific app folder

**Maven Integration**:

- Maven tool window should auto-open (View → Tool Windows → Maven)
- Use Maven lifecycle goals: `compile`, `exec:java` for running applications
- Dependencies download automatically during import

### Visual Studio Code

**Required Extensions**:

- Extension Pack for Java (includes Language Support, Debugger, Test Runner, Maven, and Project Manager)
- Install via: Extensions → Search "Extension Pack for Java" → Install

**Open Project**:

1. File → Open Folder → Select project root directory
2. VS Code will detect Maven project and prompt to reload
3. Wait for Java extension to initialize and download dependencies

**Configure Java**:

- Open Command Palette (Ctrl/Cmd + Shift + P)
- Run "Java: Configure Java Runtime"
- Set Java 17+ as project JDK
- Verify in status bar (bottom right) shows correct Java version

**Run Reference Applications**:

- Navigate to main class (e.g., `TableAPIDemo.java`) in Explorer
- Click "Run" CodeLens above the `main` method
- Or use Run and Debug panel (Ctrl/Cmd + Shift + D) to create launch configurations
- Terminal will show application output

**Maven Integration**:

- Maven commands available in Command Palette: "Java: Run Maven Commands"
- Use integrated terminal for Maven goals: `mvn compile exec:java`

---

## Self-Paced Learning

**Phase 1: Foundations**

- **[Module 01: Foundation](./docs/01-foundation/)** - Essential distributed systems concepts
  - Introduction and Architecture → Getting Started → Data Fundamentals
  - **Reference App**: [`02-getting-started-app/`](./ignite3-reference-apps/02-getting-started-app/)

- **[Module 02: Schema Design](./docs/02-schema-design/)** - Schema-as-code implementation
  - Basic Annotations → Relationships → Patterns → Evolution
  - **Reference App**: [`03-schema-annotations-app/`](./ignite3-reference-apps/03-schema-annotations-app/)

**Phase 2: Core APIs**

- **[Module 03: Data Access APIs](./docs/03-data-access-apis/)** - Data manipulation patterns
  - Table API → SQL API → Selection Guide
  - **Reference Apps**: [`04-table-api-app/`](./ignite3-reference-apps/04-table-api-app/), [`05-sql-api-app/`](./ignite3-reference-apps/05-sql-api-app/)

- **[Module 04: Distributed Operations](./docs/04-distributed-operations/)** - Transaction and compute operations
  - Transaction Fundamentals → Patterns → Compute API
  - **Reference Apps**: [`06-transactions-app/`](./ignite3-reference-apps/06-transactions-app/), [`07-compute-api-app/`](./ignite3-reference-apps/07-compute-api-app/)

**Phase 3: Performance**

- **[Module 05: Performance & Scalability](./docs/05-performance-scalability/)** - Production patterns
  - Data Streaming → Caching Strategies → Query Performance
  - **Reference Apps**: [`08-data-streaming-app/`](./ignite3-reference-apps/08-data-streaming-app/), [`09-caching-patterns-app/`](./ignite3-reference-apps/09-caching-patterns-app/), [`10-file-streaming-app/`](./ignite3-reference-apps/10-file-streaming-app/)

### Problem-Focused Path

- **Need to connect and store data?** → [Module 01: Foundation](./docs/01-foundation/)
- **Building data models?** → [Module 02: Schema Design](./docs/02-schema-design/)
- **Querying data?** → [Module 03: Data Access APIs](./docs/03-data-access-apis/)
- **Managing consistency?** → [Module 04: Distributed Operations](./docs/04-distributed-operations/)
- **Performance tuning?** → [Module 05: Performance & Scalability](./docs/05-performance-scalability/)

### Reference Materials

- **[Technical Reference](./docs/00-reference/)** - Architecture patterns and API design principles

## Reference Applications

All reference applications are located in [`ignite3-reference-apps/`](./ignite3-reference-apps/) and use the same music store dataset. Each application demonstrates the concepts from its corresponding documentation module through working, runnable code.

### Foundation Infrastructure

- **Docker Setup**: [`00-docker/`](./ignite3-reference-apps/00-docker/) - 3-node cluster with initialization scripts
- **Foundation Data**: [`01-sample-data-setup/`](./ignite3-reference-apps/01-sample-data-setup/) - Sample data and schema setup

### Application Series

- **Getting Started**: [`02-getting-started-app/`](./ignite3-reference-apps/02-getting-started-app/) - Basic operations and connections
- **Schema Design**: [`03-schema-annotations-app/`](./ignite3-reference-apps/03-schema-annotations-app/) - Schema-as-code patterns
- **Table API**: [`04-table-api-app/`](./ignite3-reference-apps/04-table-api-app/) - Object-oriented data access
- **SQL API**: [`05-sql-api-app/`](./ignite3-reference-apps/05-sql-api-app/) - SQL operations and analytics
- **Transactions**: [`06-transactions-app/`](./ignite3-reference-apps/06-transactions-app/) - ACID transaction patterns
- **Compute API**: [`07-compute-api-app/`](./ignite3-reference-apps/07-compute-api-app/) - Distributed processing
- **Data Streaming**: [`08-data-streaming-app/`](./ignite3-reference-apps/08-data-streaming-app/) - High-throughput ingestion
- **Caching Patterns**: [`09-caching-patterns-app/`](./ignite3-reference-apps/09-caching-patterns-app/) - Cache strategies
- **File Streaming**: [`10-file-streaming-app/`](./ignite3-reference-apps/10-file-streaming-app/) - File processing with backpressure

