# Getting Started - Apache Ignite 3 Reference

**Basic Ignite 3 operations and connection patterns**

📖 **Related Documentation**: [Getting Started Guide](../../docs/02-getting-started.md)

## Overview

Learn the fundamentals of Apache Ignite 3 through practical examples using the music store dataset. This module covers essential operations that every Ignite 3 developer needs to know.

## What You'll Learn

- **Client Connections**: Establish and manage connections to Ignite clusters
- **Basic CRUD Operations**: Create, read, update, and delete data
- **Table Access**: Work with tables using simple patterns
- **Error Handling**: Handle common connection and operation errors
- **Resource Management**: Properly manage client resources

## Prerequisites

**Required**: Complete [sample-data-setup](../sample-data-setup/) first to create the music store schema and load sample data.

```bash
cd sample-data-setup
mvn exec:java  # Run ProjectInitializationApp
```

## Coming Soon

This reference application is in development. It will demonstrate:

### Connection Patterns
- Single cluster connections
- Multi-node cluster connections  
- Connection pooling and lifecycle management
- Authentication and security basics

### Basic Operations
- Simple table lookups by primary key
- Basic insert, update, delete operations
- Working with the Artist and Album tables
- Handling connection errors gracefully

### Code Examples
- Hello World with Ignite 3
- Connect and query sample data
- Basic table operations
- Resource cleanup patterns

## Development Status

🚧 **In Development** - This module will be implemented as part of Phase 2B of the reference applications project.

## Related Modules

- **Prerequisites**: [sample-data-setup](../sample-data-setup/) - Create sample data first
- **Next Steps**: [schema-annotations-app](../schema-annotations-app/) - Learn schema definition
- **See Also**: [table-api-app](../table-api-app/) - Object-oriented data access