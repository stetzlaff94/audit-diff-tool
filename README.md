# audit-diff-tool

## Build Status

![Gradle CI](https://github.com/stetzlaff94/audit-diff-tool/actions/workflows/main.yml/badge.svg)

## Overview

The `DiffTool` is a utility designed to perform a detailed comparison between two JSON representations of objects. The purpose is to audit the changes made to the data, highlighting the differences, additions, and removals of fields or elements.

## Design Decisions

- **Null Collections** - The tool handles null collections by treating them as empty collections. There isnt an idiomatic way to show null arrays given the output format specification.

- **Null Objects** - The tool treats all subobject fields as independently null values in the diff i.e. if object A is null and Object B has 3 fields, the diff will show 3 changes with nulls in Object A.

- **Audit Key** - The tool requires that all objects have an "id" field or be annotated with `@AuditId`. This is used to identify objects in the diff. In case of @AuditKey annotation being moved, it will always track a "secret" field called id with the value of the annotatated field at the time, if @AuditId is used instead of an id field.

- **Jackson** - The tool uses Jackson to convert the input objects to `JsonNode` representations. This was chosen because it is a popular and well-supported library that handles reflection and serialization/deserialization of Java objects into a Tree Structure. It is possible there is a better way to do this without Jackson, relying simply on the reflection API, or utilizing a different library.

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
    }
}
```

## Error Handling

If a comparison is attempted on incompatible nodes or if required "id" fields are missing, the tool throws an `IllegalArgumentException` with a descriptive error message.

Since this is an internal JVM utility, although we handle null fields, we do not consider the possibility that the inputs could have fundamentally type structures, although this could happen using Tree Structures

## Conclusion
Altough I though it would save me time and increase re-usability in a service driven context, 
my approach of utilizing jackson may not have been an ideal implementation, both because of the time spent wrangling the Serializer 
when I could have just handled the reflection myself, and because of the potential for errors in the future if the jackson library is updated.

In any case this is a functional utility, and would benefit in a service-driven context by having the jackson integration.