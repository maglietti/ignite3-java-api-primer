# IDE Setup Guide

Reference applications run from the command line using Maven. IDE configuration enables code completion, debugging, and integrated build tools.

## IntelliJ IDEA

### Import Project

1. File → New → Select project root directory
2. Choose "Import project from external model" → Maven
3. Accept default Maven settings and wait for indexing

Optionally, you can import from version control: New → Project From Version Control

### Configure JDK

1. File → Project Structure → Project Settings → Project
2. Set Project SDK to Java 17+
3. Set Project language level to 17

### Run Applications

Each reference app contains a main class (e.g., `TableAPIDemo.java`, `SQLAPIDemo.java`):

1. Navigate to main class in Project Explorer
2. Right-click main class → Run
3. Verify working directory matches the app folder

### Maven Integration

Maven tool window opens automatically (View → Tool Windows → Maven). Use lifecycle goals `compile` and `exec:java` to run applications. Dependencies download during import.

## Visual Studio Code

### Required Extensions

Install Extension Pack for Java (includes Language Support, Debugger, Test Runner, Maven, and Project Manager):

Extensions → Search "Extension Pack for Java" → Install

### Open Project

1. File → Open Folder → Select project root directory
2. Accept Maven project reload prompt
3. Wait for Java extension initialization

### Configure Java

1. Open Command Palette (Ctrl/Cmd + Shift + P)
2. Run "Java: Configure Java Runtime"
3. Set Java 17+ as project JDK
4. Verify Java version in status bar

### Run Applications

Navigate to main class (e.g., `TableAPIDemo.java`) in Explorer:

- Click "Run" CodeLens above the `main` method
- Use Run and Debug panel (Ctrl/Cmd + Shift + D) for launch configurations
- View output in integrated terminal

### Maven Integration

Access Maven commands via Command Palette: "Java: Run Maven Commands"

Use integrated terminal: `mvn compile exec:java`

## Command Line Alternative

All applications run without IDE configuration:

```bash
cd ignite3-reference-apps/[app-name]
mvn compile exec:java
```

IDE configuration provides code completion, debugging, and integrated build tools.