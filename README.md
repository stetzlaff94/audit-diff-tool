# audit-diff-tool

## Build Status

![Gradle CI](https://github.com/stetzlaff94/audit-diff-tool/actions/workflows/main.yml/badge.svg)

## Overview

The `DiffTool` is a utility designed to perform a detailed comparison between two JSON representations of objects. The purpose is to audit the changes made to the data, highlighting the differences, additions, and removals of fields or elements.

## How It Works

- **Main Interface**: The primary interface is `DiffTool`, which exposes a `diff` method to compare any two objects and returns a list of `ChangeType`.

- **Default Implementation**: The `DefaultDiffTool` class implements the `DiffTool` interface and is the central component of the audit tool.

- **Conversion to JsonNode**: The `diff` method in `DefaultDiffTool` converts the input objects to `JsonNode` representations using Jackson's `ObjectMapper`.
  - Jackson was chosen because it is a popular and well-supported library that handles reflection and serialization/deserialization of Java objects into a Tree Structure.

- **Comparison**: The tool then compares the two `JsonNode` trees recursively, identifying differences between them. The comparison can handle ObjectNodes, ArrayNodes, and ValueNodes.

- **ChangeLog Generation**: The tool accumulates the differences in a `changeLog` and categorizes them as different types of `ChangeType`, e.g., `PropertyUpdated`, `ListUpdate`.

- **Field Comparison**: The `ObjectNode.diff` method iterates over all unique field names from both ObjectNodes, and for each field, it recursively performs a comparison and accumulates changes.

- **Array Comparison**: The tool supports comparison of both primitive and object arrays. For object arrays, the objects should have an "id" field or be annotated with `@AuditId`.

## How to Use

### 1. Implement DiffTool

Create an instance of `DefaultDiffTool`:

```kotlin
val diffTool: DiffTool = DefaultDiffTool()
```

### 2. Perform Comparison

Invoke the `diff` method on the `diffTool` instance, passing the old and new objects to be compared:

```kotlin
val changes: List<ChangeType> = diffTool.diff(oldItem, newItem)
```

### 3. Analyze Changes

Iterate through the `changes` list to analyze the differences between the two objects. The changes are represented as instances of `ChangeType`, and they can be of different specific types such as `PropertyUpdated`, `ListUpdate`.

```kotlin
for (change in changes) {
    when (change) {
        is PropertyUpdated -> // Handle property update
        is ListUpdate -> // Handle list update
        // Handle other change types as needed
    }
}
```

## Error Handling

If a comparison is attempted on incompatible nodes or if required "id" fields are missing, the tool throws an `IllegalArgumentException` with a descriptive error message.

Since this is an internal JVM utility, although we handle null fields, we do not consider the possibility that the inputs could have fundamentally type structures, although this could happen using Tree Structures

## Conclusion
Altough I though it would save me time and increase reusability in a service context, 
my approach of utilizing jackson may not have been an ideal implementation, both because of the time spent wrangling the Serializer 
when I could have just handled the reflection myself, and because of the potential for errors in the future if the jackson library is updated.