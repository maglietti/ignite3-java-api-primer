# Apache Ignite 3 Java API Primer - Project Documentation

## Project Overview

This is a comprehensive documentation project for Apache Ignite 3's Java API, designed as a practical learning primer. The project implements a dual-domain strategy using the Chinook music dataset as the primary reference data to ensure consistency and reduce cognitive load for learners.

## Project Status Summary

**Current Phase**: Phase 2A - Reference Applications Implementation  
**Phase 1 Progress**: 100% complete (Documentation)  
**Phase 2 Progress**: 35% complete (Modules 01-05 Implementation Complete)  
**Last Updated**: December 13, 2024  
**Project Start**: June 9, 2025  
**Total Sessions**: 15+ development sessions

## Technical Standards

### Your Behavior

- **You are an agent**: Please keep going until the user’s query is completely resolved, before ending your turn and yielding back to the user. Only terminate your turn when you are sure that the problem is solved.
- If you are **not sure** about file content or codebase structure pertaining to the user’s request, use your tools to read files and gather the relevant information: **do NOT guess or make up an answer**.
- You **MUST plan extensively** before each function call, and reflect extensively on the outcomes of the previous function calls. DO NOT do this entire process by making function calls only, as this can impair your ability to solve the problem and think insightfully.

### Code Style Requirements

- **Consistent Naming**: PascalCase for Chinook fields (ArtistId, AlbumId)
- **Resource Management**: Always use try-with-resources
- **Error Handling**: Comprehensive exception handling patterns
- **Async Patterns**: CompletableFuture for non-blocking operations
- **Code Structure**: We are writing educational code, reference apps do need the same rigor as enterprise code. Apply the KISS principle.
- **Educational Comments**: For reference applications, include comprehensive comments explaining:
  - **Class-level JavaDoc**: Purpose, Ignite 3 concepts demonstrated, usage examples
  - **Method-level JavaDoc**: Parameters, return values, and why specific patterns are used
  - **Inline Comments**: Distributed systems concepts, colocation strategies, performance considerations
  - **Learning Context**: Why certain approaches are recommended for production Ignite 3 applications

### Documentation Standards

- **Concise Writing**: Avoid subjective quality statements
- **Practical Examples**: Real-world scenarios using Chinook data
- **Progressive Complexity**: Simple → Advanced within each section
- **Consistent Entities**: 80% Chinook, 20% business domain
- **Cross-References**: Clear section references where needed

## User Preferences & Constraints

### Writing Style

- **Narrative**: The writing should tell a story
- **Remove**: Subjective qualifiers (comprehensive, simplified, etc.)
- **Prefer**: Objective, factual descriptions
- **Avoid**: Marketing language, unnecessary adjectives, literal explanations
- **Avoid Emoji**: Avoid using emoji in documentation
- **Focus**: Practical implementation guidance

### Technical Approach

- **Consistency**: Single reference dataset (music store sample data) for cognitive load reduction
- **Real Data**: Meaningful examples over generic placeholders
- **Performance**: Group performance-related topics together
- **Practicality**: Follow real-world development patterns
- **Standalone**: Repository should have no external dependencies

### Project Management

- **Systematic**: Complete sections thoroughly before moving on
- **Documentation**: Track all changes and decisions
- **Quality**: Prefer thorough work over speed
- **Flexibility**: Adapt based on user feedback and new insights

### Additional Standards

#### Documentation Standards

**Code Documentation Requirements**:

- **JavaDoc**: Comprehensive API documentation with examples
- **README Files**: Module-specific setup and usage instructions
- **Inline Comments**: Explain Ignite-specific concepts and patterns
- **Code Examples**: Self-contained, runnable examples in each class

**Learning-Focused Design**:

- **Progressive Complexity**: Start simple, build to advanced patterns
- **Real-World Context**: Music store business scenarios
- **Best Practices**: Demonstrate proper resource management and error handling
- **Performance Tips**: Show optimization techniques and common pitfalls

**Integration Strategy for Reference Apps**:

- **Model Reuse**: Re-implement or copy but DO NOT import from `sample-data-setup` module within same repository
- **Shared Dependencies**: Common Maven parent POM manages Ignite dependencies
- **Data Consistency**: All apps use same sample dataset and configuration
- **Progressive Complexity**: Start with simple models, build to advanced patterns

**Consistent Usage Patterns**:

- **Primary Entities**: Artist (1) → Album (N) → Track (N) hierarchy
- **Business Entities**: Customer → Invoice workflows
- **Colocation Strategy**: All related data colocated by ArtistId/CustomerId
- **Naming Conventions**: PascalCase fields (ArtistId, AlbumId, TrackId)
- **Zone Configuration**:
  - Primary entities: `@Zone(value = "MusicStore", storageProfiles = "default")` (2 replicas)
  - Reference data: `@Zone(value = "MusicStoreReplicated", storageProfiles = "default")` (3 replicas)
- **Terminology**: "Sample data", "music store data", "sample dataset" (not "Chinook")

#### Testing Strategy

**Multi-Level Testing Approach**:

1. **Unit Tests**: Individual method and component testing
2. **Integration Tests**: End-to-end API workflows with embedded Ignite
3. **Performance Tests**: Benchmarking examples with JMH
4. **Documentation Tests**: Verify all code examples compile and run

**Test Infrastructure**:

- **Testcontainers**: Dockerized Ignite clusters for integration testing
- **Embedded Mode**: In-memory clusters for fast unit testing
- **Test Data**: Predictable Chinook datasets for consistent testing
- **Assertions**: Custom matchers for Ignite-specific operations

### Implementation Requirements

**Technical Constraints**:

- **Source Code Authority**: Must use actual Ignite 3 APIs from `/Users/maglietti/Code/ignite/ignite-3`
- **Sample Data Authority**: Extract essential components from `/Users/maglietti/Code/magliettiGit/ignite3-chinook-demo`
- **Standalone Repository**: No external dependencies, all necessary code included
- **No Internet Dependencies**: Don't search web for API usage - use source code
- **Java 17 Compliance**: Modern Java features and best practices
- **Production Ready**: Include proper error handling, logging, and resource management
- **Educational Focus**: Optimize for learning rather than production completeness

**Quality Standards**:

- **Code Style**: Consistent formatting and naming conventions
- **Error Handling**: Comprehensive exception handling patterns
- **Resource Management**: Proper cleanup and lifecycle management
- **Educational Documentation**:
  - Comprehensive JavaDoc for all classes and public methods
  - Inline comments explaining Ignite 3 concepts (colocation, zones, transactions)
  - Learning-focused comments that explain WHY patterns are used
  - Examples in comments showing proper usage
  - Production best practices documented in code

## Communication Notes

### User Working Style

- Prefers direct, actionable feedback
- Values systematic, thorough approach
- Appreciates detailed progress tracking
- Requests continuation when satisfied with direction

### Project Continuation Plan

- This CLAUDE.md file serves as project memory
- Update this file as work progresses and when compacting context
- Reference completed work to maintain consistency
- Use for context when resuming after breaks

## Phase 2: Reference Applications Development (January 2025)

### Phase 2 Overview

Phase 2 focuses on creating a standalone Java project that houses all reference applications by section, transforming the documentation examples into runnable, well-documented learning tools with no external dependencies.

### Phase 2 Goals

1. **Cohesive Story Line**: Unified voice and writing style across all sections and reference applications
2. **Developer Knowledge Transfer**: Comprehensive educational comments explaining concepts and best practices
3. **Common Dataset Usage**: Consistent music store sample dataset implementation
4. **Contextual API Demonstration**: Reference apps that show API usage in realistic scenarios
5. **Standalone Operation**: No external dependencies or references to other repositories

### Phase 2 Implementation Summary

**Phase 2A Tasks Completed**:

**Planning & Design (Session 12)**:

- ✅ **Documentation Analysis**: Identified 14 sections requiring reference applications
- ✅ **Ignite 3 Source Review**: Analyzed actual API structure in `/Users/maglietti/Code/ignite/ignite-3`
- ✅ **Project Structure Design**: Planned multi-module Maven project architecture
- ✅ **Sample Data Integration Plan**: Defined how music store sample dataset integrates with reference apps
- ✅ **Section-App Mapping**: Mapped each documentation section to specific application modules
- ✅ **Build System Plan**: Designed Maven structure with proper dependencies
- ✅ **Testing Strategy**: Planned comprehensive testing approach for reference applications
- ✅ **Documentation Standards**: Defined code documentation and README requirements

**Module 02 Implementation (Session 13 - June 12, 2025)**:

- ✅ **Documentation Enhancement**: Completely rewrote Module 02 with comprehensive narrative flow, Mermaid diagrams, and educational progression
- ✅ **Reference App Creation**: Built complete getting-started-app module with three working applications:
  - **HelloWorldApp**: Simple 5-step demonstration using Book entity (40 lines, perfect for beginners)
  - **BasicSetupDemo**: Author/Book relationship with transactions and JOIN queries (135 lines)
  - **ConnectionExamples**: Basic, multi-node, and performance testing connection patterns (90 lines)
- ✅ **API Resolution**: Solved complex column mapping and annotation issues using Ignite 3 source code analysis
- ✅ **Testing & Validation**: All three applications compile, run, and work correctly with real Ignite cluster
- ✅ **Technical Documentation**: Each application includes comprehensive JavaDoc and inline comments explaining Ignite 3 concepts
- ✅ **Educational Design**: Applications demonstrate progressive complexity from simple operations to advanced patterns

**Modules 01-05 Complete Implementation (Session 15 - December 13, 2024)**:

- ✅ **Module 01**: Sample Data Setup - Complete music store schema and data loading utilities
- ✅ **Module 02**: Getting Started - Three progressive reference applications (HelloWorld, BasicSetup, Connections)
- ✅ **Module 03**: Schema and Annotations - Entity modeling with Ignite 3 annotations and colocation patterns
- ✅ **Module 04**: Table API - Key-value operations, RecordView, and async table patterns
- ✅ **Module 05**: SQL API - Comprehensive Java interface usage with 1,262-line documentation and reference applications
- ✅ **API-First Approach**: All modules focus on Java interface patterns rather than theoretical concepts
- ✅ **Educational Structure**: Progressive complexity building from basic operations to advanced patterns
- ✅ **Production Patterns**: Demonstrated transactions, async operations, batch processing, and error handling
- ✅ **Writing Standards Compliance**: Applied objective language and avoided subjective qualifiers across all modules

**Prompting Pattern Enhancement (Session 15 - December 13, 2024)**:

- ✅ **Process Improvement**: Developed 6-element prompting template to prevent scope creep
- ✅ **Source Code Integration**: Added mandatory Ignite 3 source code review to all module prompts
- ✅ **Writing Standards Enforcement**: Built-in CLAUDE.md User Preferences compliance checking
- ✅ **Module-Specific Prompts**: Created detailed prompts for modules 06-14 with proper constraints
- ✅ **Scope Boundary Definition**: Clear guidance on what to focus on vs avoid for each module
- ✅ **Quality Assurance**: Prompts now include mandatory steps to prevent common development issues

**Implementation Highlights**:

- **Real API Usage**: All applications use authentic Ignite 3 Java APIs with proper error handling and resource management
- **Distinct Sample Data**: Each application uses different entities (SimpleBook, Author/Book, connection testing) to avoid schema conflicts
- **Production Patterns**: Demonstrates try-with-resources, transactions, zone management, and SQL integration
- **Educational Focus**: Code includes learning-focused comments explaining distributed systems concepts
- **Compilation Success**: All applications compile cleanly and execute successfully against live Ignite cluster
- **Java-First Approach**: Module 05 demonstrates Java interface patterns rather than SQL syntax
- **Progressive Learning**: Each module builds complexity systematically from basic to advanced patterns
- **Source Code Authority**: All implementations based on actual Ignite 3 source code analysis
- **Writing Standards**: Consistent objective language without subjective qualifiers across all modules

### Reference Applications Architecture

#### Multi-Module Maven Project Structure

```text
ignite3-reference-apps/
├── pom.xml (parent POM)
├── README.md (project overview and quick start)
├── sample-data-setup/
│   ├── pom.xml (standalone, no external dependencies)
│   └── src/main/java/
│       └── com/apache/ignite/examples/setup/
│           ├── model/ (Complete music store domain - 11 POJOs)
│           │   ├── Artist.java          (Music artists)
│           │   ├── Album.java           (Albums with artist relationships)
│           │   ├── Track.java           (Songs with album/genre/media type refs)
│           │   ├── Genre.java           (Music genres - reference data)
│           │   ├── MediaType.java       (Audio formats - reference data)
│           │   ├── Customer.java        (Music store customers)
│           │   ├── Employee.java        (Store employees with hierarchy)
│           │   ├── Invoice.java         (Customer purchases)
│           │   ├── InvoiceLine.java     (Purchase line items)
│           │   ├── Playlist.java        (User playlists)
│           │   └── PlaylistTrack.java   (Playlist-track associations)
│           ├── config/ (Sample data configurations)
│           │   ├── IgniteConfiguration.java
│           │   └── MusicStoreZoneConfiguration.java
│           ├── util/ (Data loading utilities)
│           │   ├── DataSetupUtils.java       (Connection and basic operations)
│           │   ├── BulkDataLoader.java       (SQL script execution)
│           │   ├── TableCreationUtils.java   (Schema creation and management)
│           │   ├── DataLoadingUtils.java     (Sample data creation)
│           │   └── ReportingUtils.java       (Query and analysis utilities)
│           └── app/
│               ├── ProjectInitializationApp.java  (Main setup application)
│               ├── DataLoadingApp.java           (Data population)
│               ├── SchemaCreationApp.java        (Schema-only setup)
│               └── SampleAnalyticsApp.java       (Demo queries and reports)
│   └── src/main/resources/
│       ├── music-store-schema.sql    (Complete DDL for all 11 tables)
│       ├── sample-data.sql          (Rich sample music store data)
│       ├── additional-albums.sql    (Extended sample data)
│       └── log4j2.xml              (Logging configuration)
├── getting-started-app/
│   ├── pom.xml
│   └── src/main/java/
│       └── com/apache/ignite/examples/gettingstarted/
│           ├── HelloWorldApp.java
│           ├── BasicSetupDemo.java
│           └── ConnectionExamples.java
├── schema-annotations-app/
│   ├── pom.xml
│   └── src/main/java/
│       └── com/apache/ignite/examples/schema/
│           ├── AnnotatedEntitiesDemo.java
│           ├── ColocationExamples.java
│           └── SchemaEvolutionDemo.java
├── table-api-app/
│   ├── pom.xml
│   └── src/main/java/
│       └── com/apache/ignite/examples/table/
│           ├── KeyValueOperations.java
│           ├── RecordViewOperations.java
│           └── AsyncTableOperations.java
├── sql-api-app/
│   ├── pom.xml
│   └── src/main/java/
│       └── com/apache/ignite/examples/sql/
│           ├── DDLOperations.java
│           ├── DMLOperations.java
│           ├── QueryOperations.java
│           └── PojoMappingDemo.java
├── transactions-app/
│   ├── pom.xml
│   └── src/main/java/
│       └── com/apache/ignite/examples/transactions/
│           ├── BasicTransactionDemo.java
│           ├── TransactionIsolationDemo.java
│           └── TransactionPatterns.java
├── compute-api-app/
│   ├── pom.xml
│   └── src/main/java/
│       └── com/apache/ignite/examples/compute/
│           ├── BasicComputeJobs.java
│           ├── MapReduceExamples.java
│           ├── JobTargetingDemo.java
│           └── jobs/
│               ├── ArtistAnalysisJob.java
│               └── TrackProcessingJob.java
├── data-streaming-app/
│   ├── pom.xml
│   └── src/main/java/
│       └── com/apache/ignite/examples/streaming/
│           ├── DataStreamerDemo.java
│           ├── BackpressureHandling.java
│           └── ErrorHandlingPatterns.java
├── caching-patterns-app/
│   ├── pom.xml
│   └── src/main/java/
│       └── com/apache/ignite/examples/caching/
│           ├── CacheAsidePattern.java
│           ├── WriteThroughPattern.java
│           └── CacheWarmupStrategies.java
├── catalog-management-app/
│   ├── pom.xml
│   └── src/main/java/
│       └── com/apache/ignite/examples/catalog/
│           ├── SchemaOperations.java
│           ├── CatalogManagement.java
│           └── ZoneConfiguration.java
├── advanced-topics-app/
│   ├── pom.xml
│   └── src/main/java/
│       └── com/apache/ignite/examples/advanced/
│           ├── ErrorHandlingPatterns.java
│           ├── BatchOperations.java
│           ├── CircuitBreakerDemo.java
│           └── MusicRecommendationEngine.java
├── integration-patterns-app/
│   ├── pom.xml
│   └── src/main/java/
│       └── com/apache/ignite/examples/integration/
│           ├── spring/
│           │   ├── SpringBootIgniteApp.java
│           │   ├── IgniteAutoConfiguration.java
│           │   └── ArtistService.java
│           ├── jpa/
│           │   ├── JPAIntegrationDemo.java
│           │   └── entities/
│           └── microservices/
│               ├── ArtistMicroservice.java
│               └── RecommendationService.java
├── best-practices-app/
│   ├── pom.xml
│   └── src/main/java/
│       └── com/apache/ignite/examples/bestpractices/
│           ├── ResourceManagement.java
│           ├── PerformancePatterns.java
│           └── TestingStrategies.java
└── troubleshooting-app/
    ├── pom.xml
    └── src/main/java/
        └── com/apache/ignite/examples/troubleshooting/
            ├── DiagnosticTools.java
            ├── PerformanceAnalysis.java
            └── MusicStoreTroubleshooting.java
```

#### Technology Stack & Dependencies

**Core Dependencies**:

- **Java**: 17 (LTS)
- **Apache Ignite**: 3.0.0+ (latest stable)
- **Build Tool**: Maven 3.8+
- **Testing**: JUnit 5, AssertJ, Testcontainers
- **Logging**: SLF4J with Logback
- **Spring**: Spring Boot 3.x (for integration examples)

**Additional Tools**:

- **Code Quality**: SpotBugs, CheckStyle, PMD
- **Documentation**: JavaDoc with custom templates
- **Testing**: Integration tests with embedded Ignite clusters

#### Sample Dataset Integration Strategy

**Standalone Repository Approach** (No External Dependencies):

- **Self-Contained**: All necessary models, data, and utilities included in this repository
- **Extracted Components**: Essential elements extracted from music store reference implementation
- **No External Dependencies**: Repository operates independently without external references
- **Consistent Terminology**: Use "sample data" or "music store sample data" instead of specific dataset names
- **Learning-Focused**: Models and data optimized for educational purposes

**Project Initialization Application (Module 1)**:

```text
sample-data-setup/
├── src/main/java/com/apache/ignite/examples/setup/
│   ├── model/ (Extracted and refined POJOs)
│   │   ├── Artist.java         (Music artists)
│   │   ├── Album.java          (Music albums with artist relationship)
│   │   ├── Track.java          (Individual tracks with album relationship)
│   │   ├── Customer.java       (Music store customers)
│   │   └── Invoice.java        (Customer purchases)
│   ├── config/ (Ignite configurations)
│   │   ├── IgniteConfiguration.java
│   │   └── MusicStoreZoneConfiguration.java
│   ├── util/ (Setup and loading utilities)
│   │   ├── DataSetupUtils.java      (Connection and basic operations)
│   │   ├── BulkDataLoader.java      (SQL script execution)
│   │   └── TableCreationUtils.java  (Schema creation)
│   └── app/ (Initialization applications)
│       ├── ProjectInitializationApp.java  (Main setup application)
│       └── DataLoadingApp.java           (Data loading application)
└── src/main/resources/ (Data and configuration)
    ├── music-store-schema.sql    (Table creation DDL)
    ├── sample-data.sql          (Sample music store data)
    └── log4j2.xml              (Logging configuration)
```

**Complete Sample Data Architecture** (Extracted from Reference Source):

**11 Entity Model Classes** (All with Ignite 3 Annotations):

**Core Music Entities** (MusicStore Zone - 2 replicas):

- **Artist**: Music artists and bands (root entity)
- **Album**: Music albums (colocated by ArtistId)
- **Track**: Individual songs (colocated by AlbumId, complex entity with 9 fields)
- **Playlist**: User-created playlists
- **PlaylistTrack**: Many-to-many playlist-track relationships (colocated by PlaylistId)

**Business Entities** (MusicStore Zone - 2 replicas):

- **Customer**: Store customers (13 fields including address, support rep)
- **Employee**: Store employees with hierarchy (15 fields, self-referencing ReportsTo)
- **Invoice**: Customer purchases (colocated by CustomerId, 8 fields)
- **InvoiceLine**: Purchase line items (colocated by InvoiceId)

**Reference Data** (MusicStoreReplicated Zone - 3 replicas):

- **Genre**: Music genres (lookup table)
- **MediaType**: Audio file formats (lookup table)

**Advanced Ignite 3 Features Demonstrated**:

- **Distribution Zones**: Two zones with different replica strategies
- **Colocation**: Hierarchical data placement (Artist→Album→Track, Customer→Invoice→InvoiceLine)
- **Composite Primary Keys**: Multi-field keys (Album, Track, Invoice, InvoiceLine, PlaylistTrack)
- **Indexes**: Foreign key indexes for performance
- **Data Types**: String, Integer, BigDecimal, LocalDate support
- **Annotations**: Complete @Table, @Column, @Id, @Zone, @ColumnRef, @Index usage

**Utility Classes for Complete Functionality**:

- **DataSetupUtils**: Connection management, cluster operations
- **TableCreationUtils**: Zone creation, table management, schema operations
- **BulkDataLoader**: SQL script parsing and execution
- **DataLoadingUtils**: Programmatic sample data creation with transactions
- **ReportingUtils**: Complex queries, joins, analytics

**Application Patterns**:

- **Schema Creation**: POJO-based table creation using annotations
- **Data Loading**: Transactional batch operations
- **SQL Integration**: Both DDL and DML operations
- **Analytics**: Complex joins across the full entity hierarchy

**SQL Scripts and Data**:

- **Complete DDL**: All 11 tables with proper zones and colocation
- **Rich Sample Data**: Realistic music store data with relationships
- **Extended Data Sets**: Additional albums and tracks for testing

---

## Project Status Summary

**Phase 1 Status**: ✅ **COMPLETED** - All 14 documentation sections now have comprehensive music store sample data integration, creating a cohesive and practical learning experience for Apache Ignite 3 Java API users.

**Phase 2 Status**: 🚀 **IN PROGRESS** - Modules 01-05 completed and tested. Foundation modules provide complete learning progression from setup through advanced SQL API patterns.

**Project Summary**: Successfully created a comprehensive Apache Ignite 3 Java API primer with 100% consistent music store sample data examples. Phase 1 covers all major API areas from basic operations to advanced microservices patterns in documentation form. Phase 2 implementation is progressing well with Modules 01-05 complete, providing the foundational infrastructure and core API coverage. This includes sample data setup, getting started applications, schema modeling, table operations, and comprehensive SQL API usage. All applications compile and run successfully against live Ignite clusters, providing developers with practical, executable examples of Ignite 3 Java API usage patterns. Enhanced prompting patterns ensure consistent quality and prevent scope creep in future module development.
