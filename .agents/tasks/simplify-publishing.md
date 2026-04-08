# Task: Simplify the publishing process

## Introduction

- Now there is only one module of this project which is published. It is named `plugins`.
- The published artifact is `io.spine.tools:core-jvm-plugins:<version>`.
- This artifact is a fat JAR produced by Shadow Gradle plugin.
- This is a customly published module, which is controled by tunning of `spinePublishing`
  extension in the root project `build.gradle.kts` file.
- The code which is really published belongs to the `gradle-plugins` module. This module
  is the `implementation` dependency of the `plugins` module.
- Thus, the `plugins` module is a publishing-only "wrapper" for the `gradle-plugins` module,
  which is published as a fat JAR.
- Having a publish-only module is confusing and is a maintenance problem.
 
## Goals
- Merge the `gradle-plugins` module into the `plugins` module, 
  so that there is only one module which is published and contains the code
  which is really published.

## Constraints
- Preserve the Git history.
- Move the files instead of deleting and creating new ones.
