# deferred

[![Build](https://github.com/c-fraser/deferred/workflows/build/badge.svg)](https://github.com/c-fraser/deferred/actions)
[![Release](https://img.shields.io/github/v/release/c-fraser/deferred?logo=github&sort=semver)](https://github.com/c-fraser/deferred/releases)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.c-fraser/deferred-gradle-plugin.svg)](https://search.maven.org/artifact/io.github.c-fraser/deferred-gradle-plugin)
[![Apache License 2.0](https://img.shields.io/badge/License-Apache2-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Kotlin](https://img.shields.io/badge/kotlin-1.6.10-blue.svg?logo=kotlin)](http://kotlinlang.org)

A compiler plugin to enable [defer statements](https://go.dev/ref/spec#Defer_statements) in
the [Kotlin programming language](https://kotlinlang.org/).

> Dissimilar to Go's `defer` statements, currently, function deferral only works within a *block body*.

The compiler plugin transforms the (Kotlin compiler backend) IR to defer the execution of
*deferrable* functions until the surrounding block exits.

## Example

Given the following Kotlin:

```kotlin
fun printHelloWorld() {
  defer { println("World!") }
  println("Hello")
}
```

Ignoring the implementation details of `defer`, the output of invoking `printHelloWorld()` is
(reasonably) expected to be:

```text
World!
Hello
```

With the `deferred-compiler-plugin` [included](#usage), the body of `printHelloWorld` will be
transformed (transparently during compilation) to:

```kotlin
fun printHelloWorld() {
  try {
    println("Hello")
  } finally {
    run { println("World!") }
  }
}
```

Thus, the output of invoking `printHelloWorld()` will be:

```text
Hello
World!
```

## Usage

The `deferred-compiler-plugin` is enabled through the `deferred-gradle-plugin`, which is published
to [Maven Central](https://search.maven.org/artifact/io.github.c-fraser/deferred-gradle-plugin). Add
the following to the `build.gradle` configuration to apply the plugin:

```groovy
buildscript {
    dependencies {
        classpath "io.github.c-fraser:deferred-gradle-plugin:+"
    }
}

apply plugin: 'deferred'
```

The gradle plugin inserts
the [defer-api library](https://javadoc.io/doc/io.github.c-fraser/deferred-api/latest/index.html)
into the project dependencies. This library defines the `defer` function, which is the only function
that the `deferred-compiler-plugin` transforms to *defer* the invocation of.

## Acknowledgements

Kudos to the [kotlin-power-assert](https://github.com/bnorm/kotlin-power-assert) project which
significantly influenced the implementation of `deferred`. 
