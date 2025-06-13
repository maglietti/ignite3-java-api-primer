# Module Development Prompts

## Prompt Guide

Improved Prompt Template

Here's a template for future modules:

Please develop module [X] focusing on [specific Java API/interface].

GOAL: Teach developers how to use [specific interface] effectively in Ignite 3. This is a Java API primer, not a [concept] theory guide.

BEFORE STARTING: Review CLAUDE.md User Preferences section and follow writing standards exactly.

SCOPE:
- Focus on: Java interface usage patterns, practical code examples, real-world scenarios
- Avoid: Theoretical explanations, subjective qualifiers, marketing language

WRITING STANDARDS:
- Remove: "comprehensive", "essential", "crucial", "effectively"
- Use: Objective, factual descriptions
- Focus: Practical implementation guidance

Please review Ignite 3 source code for [specific API] and create both documentation and reference applications following the established patterns.

Specific Module Prompts

For Transactions:
"Please develop module 06 focusing on Ignite 3 Java Transaction API. GOAL: Teach developers how to use the Transaction interface, not transaction theory. Review CLAUDE.md writing standards first - avoid subjective qualifiers, focus on Java API patterns. Show how to begin/commit/rollback transactions, handle exceptions, and integrate with Table/SQL APIs."

For Compute API:
"Please develop module 07 focusing on Ignite 3 Java Compute API. GOAL: Teach developers how to use compute interfaces for distributed processing, not distributed computing theory. Review CLAUDE.md writing standards first. Show job submission, result handling, and colocation patterns using Java interfaces."

This approach frontloads the constraints, makes the goal explicit, and prevents scope creep into theoretical territory.

---

## Improved Prompting Pattern

Based on Module 05 development, use this pattern to avoid scope creep and maintain writing standards:

1. **Lead with Core Constraints** - State requirements upfront
2. **Explicit Goal Reinforcement** - Always restate the primary objective  
3. **Reference Standards Document** - Make CLAUDE.md review mandatory
4. **Source Code Review** - Make Ignite 3 source code review mandatory
5. **Scope Boundaries** - Be explicit about what NOT to focus on
6. **Writing Style Checklist** - Include quick reminder

## Module Prompts (06-14)

### Module 06: Transactions

Please develop module 06 focusing on Ignite 3 Java Transaction API patterns.

GOAL: Teach developers how to use the Transaction interface, not transaction theory. This is a Java API primer, not a transaction concepts guide.

BEFORE STARTING: 
1. Review CLAUDE.md User Preferences section and follow writing standards exactly
2. Review Ignite 3 source code for Transaction interfaces to understand actual API structure

SCOPE:
- Focus on: Transaction interface usage patterns, begin/commit/rollback code, exception handling, integration with Table/SQL APIs
- Avoid: ACID theory, transaction isolation concepts unrelated to API usage

WRITING STANDARDS:
- Remove: "comprehensive", "essential", "crucial", "effectively", "robust"
- Use: Objective, factual descriptions
- Focus: Practical implementation guidance

Please review Ignite 3 source code for Transaction interfaces and create both documentation and reference applications showing transaction usage patterns with the music store dataset.

---

### Module 07: Compute API Distributed Processing

Please develop module 07 focusing on Ignite 3 Java Compute API interfaces.

GOAL: Teach developers how to use compute interfaces for distributed job execution, not distributed computing theory. This is a Java API primer, not a compute concepts guide.

BEFORE STARTING: 
1. Review CLAUDE.md User Preferences section and follow writing standards exactly
2. Review Ignite 3 source code for Compute interfaces to understand actual API structure

SCOPE:
- Focus on: Compute interface usage, job submission patterns, result handling code, colocation with data
- Avoid: Distributed computing theory, job scheduling concepts unrelated to API usage

WRITING STANDARDS:
- Remove: "comprehensive", "essential", "crucial", "effectively", "powerful"
- Use: Objective, factual descriptions
- Focus: Practical implementation guidance

Please review Ignite 3 source code for Compute interfaces and create both documentation and reference applications demonstrating job execution patterns using the music store dataset.

---

### Module 08: Data Streaming

Please develop module 08 focusing on Ignite 3 Java Data Streaming API.

GOAL: Teach developers how to use streaming interfaces for bulk data operations, not data streaming theory. This is a Java API primer, not a streaming concepts guide.

BEFORE STARTING: 
1. Review CLAUDE.md User Preferences section and follow writing standards exactly
2. Review Ignite 3 source code for DataStreamer and related interfaces to understand actual API structure

SCOPE:
- Focus on: DataStreamer interface usage patterns, bulk loading code examples, backpressure handling
- Avoid: Streaming theory, performance explanations unrelated to Java API usage

WRITING STANDARDS:
- Remove: "comprehensive", "essential", "crucial", "effectively", "powerful"
- Use: Objective, factual descriptions
- Focus: Practical implementation guidance

Please review Ignite 3 source code for DataStreamer and related interfaces, then create both documentation and reference applications showing how to stream data into Ignite tables using the music store dataset.

---

### Module 09: Caching Patterns

Please develop module 09 focusing on Ignite 3 Java caching pattern implementations.

GOAL: Teach developers how to implement caching patterns using Ignite 3 Java APIs, not caching theory. This is a Java API primer, not a caching concepts guide.

BEFORE STARTING: 
1. Review CLAUDE.md User Preferences section and follow writing standards exactly
2. Review Ignite 3 source code for caching patterns using Table/SQL APIs to understand actual implementation approaches

SCOPE:
- Focus on: Cache-aside, write-through, write-behind patterns using Table/SQL APIs, Java implementation patterns
- Avoid: Caching theory, cache performance concepts unrelated to API usage

WRITING STANDARDS:
- Remove: "comprehensive", "essential", "crucial", "effectively", "robust"
- Use: Objective, factual descriptions
- Focus: Practical implementation guidance

Please review how caching patterns are implemented using Ignite 3 Java APIs and create both documentation and reference applications demonstrating cache-aside, write-through, and write-behind patterns with the music store dataset.

---

### Module 10: Schema and Catalog Management

Please develop module 10 focusing on Ignite 3 Java Schema and Catalog Management APIs.

GOAL: Teach developers how to use schema management interfaces for catalog operations, not database schema theory. This is a Java API primer, not a schema design guide.

BEFORE STARTING: 
1. Review CLAUDE.md User Preferences section and follow writing standards exactly
2. Review Ignite 3 source code for Catalog, Schema, and Zone management interfaces to understand actual API structure

SCOPE:
- Focus on: Catalog interface usage, schema introspection APIs, zone management interfaces, Java code patterns
- Avoid: Database design theory, schema optimization concepts unrelated to API usage

WRITING STANDARDS:
- Remove: "comprehensive", "essential", "crucial", "effectively", "advanced"
- Use: Objective, factual descriptions  
- Focus: Practical implementation guidance

Please review Ignite 3 source code for Catalog, Schema, and Zone management interfaces, then create both documentation and reference applications showing schema introspection and catalog operations using Java APIs.

---

### Module 11: Advanced Topics

Please develop module 11 focusing on Ignite 3 Java APIs for advanced scenarios.

GOAL: Teach developers how to use Java APIs for complex scenarios like error handling, retry patterns, and performance monitoring, not advanced distributed systems theory. This is a Java API primer, not an advanced concepts guide.

BEFORE STARTING: 
1. Review CLAUDE.md User Preferences section and follow writing standards exactly
2. Review Ignite 3 source code for exception handling, monitoring, and advanced API patterns to understand actual implementation approaches

SCOPE:
- Focus on: Exception handling APIs, retry mechanisms using Java interfaces, monitoring APIs, complex query patterns
- Avoid: Distributed systems theory, performance tuning concepts unrelated to API usage

WRITING STANDARDS:
- Remove: "comprehensive", "essential", "crucial", "effectively", "sophisticated"
- Use: Objective, factual descriptions
- Focus: Practical implementation guidance

Please review Ignite 3 source code for exception handling, monitoring, and advanced API patterns, then create both documentation and reference applications demonstrating complex scenarios using the music store dataset.

---

### Module 12: Integration Patterns  

Please develop module 12 focusing on Ignite 3 Java integration with other frameworks.

GOAL: Teach developers how to integrate Ignite 3 Java APIs with Spring, microservices, and JPA, not integration architecture theory. This is a Java API primer, not an integration patterns guide.

BEFORE STARTING: 
1. Review CLAUDE.md User Preferences section and follow writing standards exactly
2. Review Ignite 3 source code for exception handling, monitoring, and advanced API patterns to understand actual implementation approaches

SCOPE:
- Focus on: Spring Boot integration code, microservices API usage, JPA integration patterns using Java interfaces
- Avoid: Architecture theory, integration best practices unrelated to API usage

WRITING STANDARDS:
- Remove: "comprehensive", "essential", "crucial", "effectively", "seamless"
- Use: Objective, factual descriptions
- Focus: Practical implementation guidance

Please review Ignite 3 integration capabilities and create both documentation and reference applications showing Spring Boot configuration, microservices integration, and JPA usage with Ignite 3 APIs using the music store dataset.

---

### Module 13: Best Practices Common Patterns

Please develop module 13 focusing on Ignite 3 Java API usage patterns and common implementations.

GOAL: Teach developers common Java API usage patterns and implementations, not best practices theory. This is a Java API primer, not a best practices guide.

BEFORE STARTING: 
1. Review CLAUDE.md User Preferences section and follow writing standards exactly
2. Review Ignite 3 source code for exception handling, monitoring, and advanced API patterns to understand actual implementation approaches

SCOPE:
- Focus on: Common API usage patterns, resource management code, error handling implementations, performance patterns using Java interfaces
- Avoid: Best practices theory, design principles unrelated to API usage

WRITING STANDARDS:
- Remove: "comprehensive", "essential", "crucial", "effectively", "optimal"
- Use: Objective, factual descriptions
- Focus: Practical implementation guidance

Please review common Ignite 3 Java API usage patterns and create both documentation and reference applications demonstrating resource management, error handling, and performance patterns using the music store dataset.

---

### Module 14: Troubleshooting Guide

Please develop module 14 focusing on Ignite 3 Java API troubleshooting using Java interfaces.

GOAL: Teach developers how to use Java APIs for diagnostics and troubleshooting, not troubleshooting methodology. This is a Java API primer, not a troubleshooting theory guide.

BEFORE STARTING: 
1. Review CLAUDE.md User Preferences section and follow writing standards exactly
2. Review Ignite 3 source code for exception handling, monitoring, and advanced API patterns to understand actual implementation approaches

SCOPE:
- Focus on: Diagnostic APIs, logging interfaces, monitoring Java code, debugging API usage patterns
- Avoid: Troubleshooting methodology, diagnostic theory unrelated to API usage

WRITING STANDARDS:
- Remove: "comprehensive", "essential", "crucial", "effectively", "thorough"
- Use: Objective, factual descriptions
- Focus: Practical implementation guidance

Please review Ignite 3 diagnostic and monitoring APIs and create both documentation and reference applications demonstrating how to use Java interfaces for troubleshooting, logging, and diagnostics with the music store dataset.

---

## Previous Module Prompts (Reference)

### 04

I'm satisfied with modules 01 02, and 03 for now. Please begin working on module 04 based on what you have learned building the previous modules. Remember that there are good docs and code to use as models in the ignite3-chinook-demo project. Always start by reviewing that project before working on modules in this project.

You also have access to the Ignite 3 source code. Be sure to review the API source before writing this section. Always use the API to perform an action in SQL. This module is specifically easy to fall into SQL rather than using the API.

Remember that the goal for this project is to use the music store data set for consistency in all modules. Module 02 was an exception because it can be a quick start module that stands alone.

Please develop the module and reference code for module 04.

---

### 05

Please begin working on the next module based on what you have learned building the previous modules.  

Always start by reviewing the Ignite 3 source before working on modules in this project. Be sure to carefully review the API and SQL engine source before writing this section. Since this section is intended to focus on Ignite SQL capabilities, it is ok to not use the Java API object oriented capabilities in your examples. Please be as thorough as possible about the Ignite SQL engine.

Remember that the goal for this project is to use the music store data set for consistency in all modules. Module 02 was an exception because it can be a quick start module that stands alone.

Please ensure that code examples have adequate explanations. Remember that we are writing a training document that is intended to educate developers of all abilities about Ignite 3 and how to use it successfully. the narrative is as important as the code.

The 05 doc should continue on in the same style, tone, and voice as previous modules.

Please develop the module and reference code for module 05.

---

### 05 second try

Always start by reviewing the CLAUDE.md file and Ignite 3 source before working on modules in this project. Be sure to carefully review the API and SQL engine source before writing this section. Since this section is intended to focus on Ignite SQL capabilities, it is ok to not use the Java API object oriented capabilities in your examples. Please be as thorough as possible about the Ignite SQL engine.

Please ensure that code examples have adequate explanations. Remember that we are writing a training document that is intended to educate developers of all abilities about Ignite 3 and how to use it successfully. the narrative is as important as the code.

I think that my last instructions may have sent us down the wrong path for module 05. Both you and I lost track of our goal for this primer which is to teach a developer how to use the Ignite 3 Java API, and instead we developed a SQL guide inside of the Java API primer.

Please abandon the work that you've done on module 05 and the related reference application and start over. Review the Ignite 3 SQL API and SQL implementation. Replan and build a focused guide that covers the SQL API using valid Ignite 3 SQL examples.  

