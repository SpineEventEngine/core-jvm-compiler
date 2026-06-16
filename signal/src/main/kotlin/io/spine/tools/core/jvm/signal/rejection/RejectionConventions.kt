/*
 * Copyright 2026, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.spine.tools.core.jvm.signal.rejection

import io.spine.annotation.VisibleForTesting
import io.spine.tools.compiler.ast.ProtobufSourceFile
import io.spine.tools.compiler.jvm.javaMultipleFiles
import io.spine.tools.compiler.jvm.javaOuterClassName

/**
 * The suffix required for the outer Java class name generated for a rejection file.
 */
private const val REJECTIONS_CLASS_SUFFIX: String = "Rejections"

/**
 * The name of the `java_multiple_files` Protobuf file option.
 */
private const val JAVA_MULTIPLE_FILES: String = "java_multiple_files"

/**
 * The name of the `java_outer_classname` Protobuf file option.
 */
private const val OUTER_CLASS_NAME: String = "java_outer_classname"

/**
 * Ensures that this rejection file is configured according to the conventions required
 * for generating [RejectionThrowable][io.spine.base.RejectionThrowable] classes.
 *
 * Rejection messages are generated as nested classes of a single outer class so that
 * the top-level throwable classes generated for the rejections do not clash with
 * the rejection message classes of the same simple name. Therefore, a rejection file must:
 *
 *  1. Have the `java_multiple_files` option set to `false`, or not declare it at all.
 *  2. Have the `java_outer_classname` option end with `"Rejections"`, or not declare it at all.
 *
 * The method is expected to be called only for files recognized as rejection files,
 * i.e., those with the `rejections.proto` name suffix.
 *
 * @throws IllegalArgumentException if the file violates one of the conventions above.
 *   The message of the exception explains the violation and the way to fix it.
 */
@VisibleForTesting
public fun ProtobufSourceFile.checkRejectionConventions() {
    checkNotMultipleFiles()
    checkOuterClassName()
}

private fun ProtobufSourceFile.checkNotMultipleFiles() {
    require(!header.javaMultipleFiles()) {
        "The rejection file `${file.path}` has the `$JAVA_MULTIPLE_FILES` option set to `true`." +
                " Rejection messages must be generated into a single Java source file so that" +
                " the generated throwable classes do not clash with the rejection message" +
                " classes of the same name." +
                " Please set the `$JAVA_MULTIPLE_FILES` option to `false` or remove the option."
    }
}

private fun ProtobufSourceFile.checkOuterClassName() {
    val outerClassName = outerClassName()
    require(outerClassName.endsWith(REJECTIONS_CLASS_SUFFIX)) {
        "The rejection file `${file.path}` has the outer Java class name `$outerClassName`" +
                " which does not end with `$REJECTIONS_CLASS_SUFFIX`." +
                " Please make the `$OUTER_CLASS_NAME` option value end with" +
                " `$REJECTIONS_CLASS_SUFFIX`, or remove the option to derive the name" +
                " from the file name."
    }
}

/**
 * Obtains the name of the outer Java class generated for this rejection file.
 */
internal fun ProtobufSourceFile.outerClassName(): String = header.javaOuterClassName()
