# Core JVM plugins for the Spine Compiler

[![Ubuntu build][ubuntu-build-badge]][gh-actions]
[![codecov.io][codecov-badge]][codecov-data] &nbsp;
[![license][apache-badge]][apache-license]

This repository hosts the plugins to the Spine Compiler that the Core JVM library offers for
building a client or a server application.

## Environment

The modules in this repository are built with Java 17.

For the versions of other dependencies, refer to the Kotlin source code under
[io.spine.dependency](./buildSrc/src/main/kotlin/io/spine/dependency).

## Testing modules in the project structure

### Integration testing

Please refer to [TESTING.md](./TESTING.md).

### Performance testing

This repo includes the `BuildSpeed` submodule with the performance tests for the Spine tools.
The tests are executed in a GH Action. To run the tests locally, launch the `checkPerformance`
Gradle task. The task execution time will be printed to the console, and 
the [journal file](./BuildSpeed/journal.log) will be updated.


[apache-badge]: https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat
[apache-license]: http://www.apache.org/licenses/LICENSE-2.0

[gh-actions]: https://github.com/SpineEventEngine/core-jvm-compiler/actions
[ubuntu-build-badge]: https://github.com/SpineEventEngine/core-jvm-compiler/actions/workflows/build-on-ubuntu.yml/badge.svg

[codecov-badge]: https://codecov.io/github/SpineEventEngine/core-jvm-compiler/coverage.svg?branch=master
[codecov-data]: https://codecov.io/github/SpineEventEngine/core-jvm-compiler?branch=master
