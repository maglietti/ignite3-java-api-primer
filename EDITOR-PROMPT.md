# Top-to-Bottom Module Editor Prompt

## Mission: Create a Cohesive Apache Ignite 3 Java API Learning Journey

You are tasked with editing modules 01-10 in the `/docs` directory to create a cohesive storyline that guides developers through a complete Apache Ignite 3 Java API learning experience. The work builds on the foundation established in CLAUDE.md and should transform 10 independent modules into a unified, progressive learning narrative.

## Core Requirements

### BEFORE STARTING ANY EDITS:

1. **Read CLAUDE.md completely** - All editing must follow the writing standards and user preferences exactly
2. **Review 00-arch.md** - Use the API Information Architecture as your source of truth for all API references
3. **Analyze current modules 01-10** - Understand the existing content structure and progression
4. **Plan the cohesive storyline** - Map out how each module builds upon previous ones

### PRIMARY OBJECTIVES:

**1. Cohesive Storyline**: Create a narrative arc that flows naturally from module 01 through 10, where each module builds meaningfully on previous concepts while remaining standalone-capable.

**2. Module 02 as Quickstart**: Ensure Module 02 can function as a complete quickstart guide for developers who want immediate hands-on experience.

**3. Standalone Module Capability**: Each module should work independently - a developer should be able to work through Module 07 without having read Module 04, then return to Module 04 later.

**4. Progressive Complexity**: Build complexity systematically across modules while maintaining accessibility.

**5. Consistent Voice**: Maintain unified tone, terminology, and educational approach across all modules.

## Module-Specific Focus Areas

### Module 01: Foundation & Setup
- **Role**: Project initialization and sample data foundation
- **Key Elements**: Music store schema introduction, data loading utilities, zone configuration
- **Story Context**: "Setting up your distributed music store" - establishes the business scenario
- **Standalone Elements**: Complete setup guide, troubleshooting, verification steps

### Module 02: Getting Started (Special - Quickstart Capable)  
- **Role**: Complete quickstart that can stand alone while fitting the larger narrative
- **Key Elements**: HelloWorld → BasicSetup → Connections progression
- **Story Context**: "Your first distributed application" - immediate success experience
- **Quickstart Requirements**: 
  - Must work without requiring Module 01 (use simple Book entity)
  - Include all necessary setup and teardown
  - Provide complete working examples
  - Reference but don't require the music store dataset

### Module 03: Schema Design
- **Role**: Entity modeling and annotation patterns
- **Key Elements**: @Table, @Column, @Id, colocation, zones
- **Story Context**: "Designing your distributed schema" - builds on Module 01's schema
- **Standalone Elements**: Annotation reference, design patterns, evolution strategies

### Module 04: Table API
- **Role**: Object-oriented data access patterns
- **Key Elements**: KeyValueView, RecordView, async operations
- **Story Context**: "Managing your music catalog" - CRUD operations with music entities
- **Standalone Elements**: Complete API reference, performance patterns, error handling

### Module 05: SQL API
- **Role**: Relational data access and complex queries
- **Key Elements**: DDL, DML, complex joins, batch operations
- **Story Context**: "Analytics and reporting" - business intelligence scenarios
- **Standalone Elements**: SQL reference, query optimization, integration patterns

### Module 06: Transactions
- **Role**: ACID operations and consistency patterns
- **Key Elements**: Transaction API, isolation levels, error handling
- **Story Context**: "Customer purchase workflows" - multi-table business transactions
- **Standalone Elements**: Transaction patterns, troubleshooting, performance considerations

### Module 07: Compute API
- **Role**: Distributed processing and job execution
- **Key Elements**: Job submission, colocation, MapReduce patterns
- **Story Context**: "Music recommendation engine" - distributed analytics
- **Standalone Elements**: Compute patterns, monitoring, scaling strategies

### Module 08: Data Streaming
- **Role**: High-throughput data ingestion
- **Key Elements**: DataStreamer, backpressure, reactive patterns
- **Story Context**: "Real-time event processing" - streaming music events
- **Standalone Elements**: Streaming patterns, performance tuning, error recovery

### Module 09: Caching Patterns
- **Role**: Performance optimization through caching
- **Key Elements**: Cache-aside, write-through, write-behind
- **Story Context**: "Optimizing user experience" - catalog and user data caching
- **Standalone Elements**: Pattern selection guide, monitoring, troubleshooting

### Module 10: Schema & Catalog Management
- **Role**: Dynamic schema operations and introspection
- **Key Elements**: Catalog API, schema evolution, zone management
- **Story Context**: "Managing your growing platform" - operational database management
- **Standalone Elements**: Management operations, migration strategies, monitoring

## Narrative Arc Design

### Overall Story: "Building a Distributed Music Platform"

**Act I - Foundation (Modules 01-03)**:
- Module 01: "Setting up the infrastructure" - data foundation and configuration
- Module 02: "Your first success" - immediate hands-on experience (quickstart)  
- Module 03: "Designing for scale" - distributed schema patterns

**Act II - Core Operations (Modules 04-06)**:
- Module 04: "Managing your data" - object-oriented operations
- Module 05: "Analyzing your business" - SQL analytics and reporting
- Module 06: "Ensuring consistency" - transactional business processes

**Act III - Advanced Capabilities (Modules 07-10)**:
- Module 07: "Intelligence at scale" - distributed processing and recommendations
- Module 08: "Real-time responsiveness" - streaming data and events
- Module 09: "Performance optimization" - caching for user experience
- Module 10: "Operational excellence" - schema management and evolution

## Writing Standards Enforcement

### CRITICAL COMPLIANCE REQUIREMENTS:

**From CLAUDE.md User Preferences - MUST FOLLOW EXACTLY**:

1. **Narrative Writing**: Every module must tell a story that engages and educates
2. **Remove Subjective Qualifiers**: No "comprehensive", "simplified", "robust", etc.
3. **Objective, Factual Descriptions**: Focus on what the API does, not quality judgments
4. **Avoid Marketing Language**: No unnecessary adjectives or promotional tone
5. **No Emoji**: Avoid emoji in documentation completely
6. **Practical Implementation Guidance**: Focus on how to use the APIs effectively

**Additional Standards**:

7. **Consistent Entities**: Use music store entities consistently (80% of examples)
8. **Progressive Complexity**: Build from simple to advanced within each module
9. **Cross-References**: Clear navigation between related concepts in other modules
10. **Real-World Context**: Business scenarios that demonstrate practical value

### Code and API Standards:

1. **Source Code Authority**: Use only APIs from the actual Ignite 3 source code
2. **API Accuracy**: Reference 00-arch.md for correct interface usage
3. **Resource Management**: Always demonstrate try-with-resources patterns
4. **Error Handling**: Show proper exception handling in examples
5. **Async Patterns**: Demonstrate both sync and async variants where applicable

## Editing Process

### Phase 0: Archive and Prepare (REQUIRED FIRST STEP)
**BEFORE editing any module, you MUST:**

1. **Archive Original**: Copy the existing module to `./archive/` directory to preserve the original
   ```bash
   cp docs/XX-module-name.md archive/
   ```
2. **Verify Archive**: Confirm the original is safely preserved
3. **Identify Correct Version**: If duplicates exist, determine which is the current/correct version
4. **Clean Duplicates**: Remove any duplicate or outdated versions from docs/
5. **Document Changes**: Note any files archived or removed

### Phase 1: Analysis and Planning
1. Read and analyze all current modules 01-10
2. Map existing content to the narrative arc
3. Identify gaps, inconsistencies, and improvement opportunities
4. Create detailed editing plan for each module

### Phase 2: Module-by-Module Editing
For each module:

1. **Preserve Working Examples**: Keep all functional code examples
2. **Enhance Narrative Flow**: Improve storytelling and progression
3. **Standardize Voice**: Ensure consistent tone and terminology
4. **Add Cross-References**: Link to related concepts in other modules
5. **Verify API Accuracy**: Check all code against 00-arch.md
6. **Remove Writing Violations**: Eliminate subjective qualifiers and marketing language

### Phase 3: Cohesion Review
1. **End-to-End Reading**: Review the complete 01-10 sequence
2. **Narrative Consistency**: Ensure story arc flows naturally
3. **Standalone Validation**: Verify each module can work independently
4. **Module 02 Quickstart Test**: Confirm Module 02 works as complete quickstart

## Success Criteria

### Cohesive Storyline Success:
- [ ] Modules 01-10 tell a complete, engaging story of building a distributed music platform
- [ ] Each module builds naturally on previous concepts while introducing new capabilities
- [ ] Narrative voice is consistent and professional throughout
- [ ] Business context is clear and motivating

### Module 02 Quickstart Success:
- [ ] Module 02 works as a complete quickstart without requiring other modules
- [ ] Includes all necessary setup, examples, and teardown
- [ ] Provides immediate success experience for new developers
- [ ] References but doesn't require the full music store dataset

### Standalone Module Success:
- [ ] Any module can be read and understood independently
- [ ] Each module includes necessary context and background
- [ ] Cross-references are helpful but not required for comprehension
- [ ] Examples are self-contained and runnable

### Writing Standards Success:
- [ ] No subjective qualifiers or marketing language
- [ ] Consistent use of music store entities and business context
- [ ] All API references are accurate per 00-arch.md
- [ ] Progressive complexity within and across modules
- [ ] Engaging narrative that educates while informing

## Output Requirements

For each edited module:

1. **Clear Change Summary**: Document what was changed and why
2. **Narrative Enhancement Details**: Explain how storytelling was improved
3. **API Accuracy Verification**: Confirm all code examples are correct
4. **Cross-Reference Updates**: List new connections to other modules
5. **Standalone Capability Confirmation**: Verify independent usability

## Quality Assurance

Before completing any module edit:

1. **CLAUDE.md Compliance Check**: Verify adherence to all writing standards
2. **API Accuracy Review**: Cross-check all examples against 00-arch.md
3. **Narrative Flow Test**: Read in context of overall story arc
4. **Standalone Functionality Test**: Verify module works independently
5. **Writing Standards Audit**: Eliminate any subjective language or marketing tone

---

**Remember**: This is not just editing - you're crafting a complete learning experience that guides developers from their first connection to advanced distributed application patterns. Every word should serve the dual purpose of teaching and engaging while maintaining the highest technical accuracy.