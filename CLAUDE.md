# Apache Ignite 3 Java API Primer - Project Documentation

## Project Overview

This is a comprehensive documentation project for Apache Ignite 3's Java API, designed as a practical learning primer. The project implements a dual-domain strategy using the Chinook music dataset as the primary reference data to ensure consistency and reduce cognitive load for learners.

## Project Status Summary

**Current Phase**: Phase 2 Planning - Reference Applications Development  
**Phase 1 Progress**: 100% complete (Documentation)  
**Phase 2 Progress**: 0% complete (Reference Applications - Planning Stage)  
**Last Updated**: January 11, 2025  
**Project Start**: June 9, 2025  
**Total Sessions**: 12+ development sessions

### Major Milestones Completed ✅

1. **Project Foundation** (June 9, 2025) - Created initial structure with architecture analysis and 14-section outline
2. **Content Development** (June 9-10, 2025) - Developed comprehensive content for all 14 sections
3. **Content Quality Improvement** (June 9, 2025) - Removed subjective language and problematic phrasing (15 edits)
4. **Reference Data Strategy** (June 10, 2025) - Implemented dual-domain approach with Chinook dataset
5. **Chinook Integration** (June 10, 2025) - Established reference to external Chinook dataset in ignite3-chinook-demo repository
6. **Section Updates (1-8)** (January 6, 2025) - Updated all foundational sections to use consistent Chinook entities
7. **Section Reorganization** (January 6, 2025) - Moved Caching (formerly section 14) to section 9 for better flow
8. **Advanced Sections Integration** (January 6, 2025) - Updated sections 9-11 fully and section 12 partially with Chinook entities
9. **Documentation Structure** - Established clear learning progression with performance continuity
10. **Sections 13-14 Completion** (January 11, 2025) - Updated Best Practices and Troubleshooting with music domain examples
11. **Section 12 Final Completion** (January 11, 2025) - Completed all remaining integration patterns with comprehensive music domain examples

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

- **Remove**: Subjective qualifiers (comprehensive, simplified, etc.)
- **Prefer**: Objective, factual descriptions
- **Avoid**: Marketing language, unnecessary adjectives
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

### Phase 2 Planning Summary (Session 12 - January 11, 2025)

**Planning Tasks Completed**:

- ✅ **Documentation Analysis**: Identified 14 sections requiring reference applications
- ✅ **Ignite 3 Source Review**: Analyzed actual API structure in `/Users/maglietti/Code/ignite/ignite-3`
- ✅ **Project Structure Design**: Planned multi-module Maven project architecture
- ✅ **Sample Data Integration Plan**: Defined how music store sample dataset integrates with reference apps
- ✅ **Section-App Mapping**: Mapped each documentation section to specific application modules
- ✅ **Build System Plan**: Designed Maven structure with proper dependencies
- ✅ **Testing Strategy**: Planned comprehensive testing approach for reference applications
- ✅ **Documentation Standards**: Defined code documentation and README requirements

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

**Integration Strategy for Reference Apps**:

- **Model Reuse**: Import from `sample-data-setup` module within same repository
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

### Phase 2 Success Criteria

1. **Runnable Examples**: All documentation examples converted to executable applications
2. **Learning Experience**: Clear progression from basic to advanced concepts
3. **Consistency**: Uniform Chinook dataset usage across all applications
4. **Production Quality**: Enterprise-ready code patterns and practices
5. **Developer Onboarding**: New developers can learn Ignite 3 APIs effectively

### Next Steps for Phase 2 Implementation

1. **Extract Sample Data**: Create standalone sample-data-setup module with extracted components
2. **Project Initialization App**: Build Module 1 initialization application for easy setup
3. **Core Applications**: Build getting-started, schema, table-api, and sql-api modules using shared models
4. **Advanced Features**: Implement compute, streaming, and integration patterns
5. **Testing & Documentation**: Add comprehensive tests and learning-focused documentation

### Sample Data Integration Approach

**Phase 2A: Extract and Setup**

1. **Component Extraction**: Extract essential POJOs, utilities, and SQL scripts from reference source
2. **Terminology Update**: Replace domain-specific names with generic "sample data" terminology
3. **Standalone Implementation**: Ensure no external dependencies in extracted components
4. **Project Initialization**: Create user-friendly setup application for Module 1

**Phase 2B: Reference App Development**

1. **Shared Foundation**: Use `sample-data-setup` module as dependency within same repository
2. **Import Strategy**: `import com.apache.ignite.examples.setup.model.Artist` from local module
3. **Configuration Reuse**: Leverage setup module zone and storage configurations
4. **Progressive Examples**: Build from simple initialization to advanced patterns

**Phase 2C: Module 1 Integration**

1. **Documentation Update**: Update Module 1 (Getting Started) to reference initialization app
2. **Setup Instructions**: "Run ProjectInitializationApp to set up complete music store schema and sample data"
3. **Alternative Workflows**: 
   - "Use SchemaCreationApp for schema-only setup"
   - "Use DataLoadingApp to populate existing schema with sample data"
   - "Use SampleAnalyticsApp to explore sample data with queries"
4. **Verification Steps**: Include steps to verify successful setup and explore the 11-table schema

**Sample Data Complexity Levels**:
1. **Basic Setup**: Core entities (Artist, Album, Track) for simple examples
2. **Business Setup**: Add Customer, Invoice, InvoiceLine for transaction examples
3. **Complete Setup**: All 11 entities for advanced scenarios (playlists, employees, hierarchy)
4. **Analytics Ready**: Rich sample data for reporting and complex query examples

---

## Project Status Summary

**Phase 1 Status**: ✅ **COMPLETED** - All 14 documentation sections now have comprehensive music store sample data integration, creating a cohesive and practical learning experience for Apache Ignite 3 Java API users.

**Phase 2 Status**: 📋 **PLANNED** - Reference applications architecture designed with complete standalone sample data integration strategy. Ready for implementation with full 11-entity music store model.

**Project Summary**: Successfully created a comprehensive Apache Ignite 3 Java API primer with 100% consistent music store sample data examples. Phase 1 covers all major API areas from basic operations to advanced microservices patterns in documentation form. Phase 2 will provide runnable reference applications that demonstrate these concepts in executable code, using a complete 11-entity music store model extracted from the reference source, creating a standalone repository with no external dependencies and comprehensive coverage of Ignite 3 features including distribution zones, colocation, complex relationships, and advanced query patterns.
