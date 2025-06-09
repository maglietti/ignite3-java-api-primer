# 2. Getting Started

## Setup & Dependencies

### Maven Dependencies

```xml
<dependency>
    <groupId>org.apache.ignite</groupId>
    <artifactId>ignite-client</artifactId>
    <version>3.0.0</version>
</dependency>
```

### Gradle Dependencies

```groovy
implementation 'org.apache.ignite:ignite-client:3.0.0'
```

### Basic Project Setup

*[To be completed with specific setup instructions]*

## Connection Patterns

### Thin Client Connection

```java
try (IgniteClient client = IgniteClient.builder()
        .addresses("127.0.0.1:10800")
        .build()) {
    // Work with client
}
```

### Embedded Server Setup

```java
IgniteServer server = IgniteServer.start(nodeName, configPath, workDir);
```

### Configuration Basics

- SSL configuration
- Authentication setup
- Timeout configuration
- Connection pooling

## First Steps

*[To be completed with "Hello World" example and basic table creation]*