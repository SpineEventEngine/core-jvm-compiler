/*
 * Copyright 2024, TeamDev. All rights reserved.
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

package io.spine.tools.mc.java.signal.rejection

import com.google.common.annotations.VisibleForTesting
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.ParameterSpec
import io.spine.protodata.ast.MessageType
import io.spine.tools.java.codeBlock
import io.spine.tools.java.javadoc.JavadocText.fromEscaped
import io.spine.tools.java.javadoc.JavadocText.fromUnescaped
import io.spine.tools.mc.java.signal.rejection.Javadoc.PROTO_MESSAGE_NOTE_TEMPLATE

/**
 * Pieces of Javadoc code used in the generated code of rejections.
 */
internal object Javadoc {

    const val PROTO_MESSAGE_NOTE_TEMPLATE =
        "<p>The rejection message proto type is {@code \$L.\$L}."

    @VisibleForTesting
    const val NEW_BUILDER_METHOD_ABSTRACT = "Creates a new builder for the rejection."

    @VisibleForTesting
    const val BUILD_METHOD_ABSTRACT = "Creates the rejection from the builder and validates it."

    private const val REJECTION_MESSAGE_METHOD_ABSTRACT = "Obtains the rejection message."
    private const val BUILDER_CONSTRUCTOR_ABSTRACT = "Prevent direct instantiation of the builder."

    @VisibleForTesting
    const val BUILDER_ABSTRACT_TEMPLATE = "The builder for the {@code \$L} rejection."

    val ofNewBuilderMethod: String by lazy {
        fromEscaped(NEW_BUILDER_METHOD_ABSTRACT)
            .withNewLine()
            .value
    }

    val ofBuilderConstructor: String by lazy {
        fromEscaped(BUILDER_CONSTRUCTOR_ABSTRACT).withNewLine().value
    }

    val ofRejectionMessageMethod: String by lazy {
        fromEscaped(REJECTION_MESSAGE_METHOD_ABSTRACT).withNewLine().value
    }

    val ofBuildMethod: String by lazy {
        fromEscaped(BUILD_METHOD_ABSTRACT).withNewLine().value
    }

    /**
     * Generated Javadoc for the `RejectionThrowable` class corresponding
     * the given rejection type.
     */
    fun forThrowableOf(rejection: MessageType): CodeBlock = codeBlock {
        add(classSummaryFor(rejection))
        add(protoMessageNote(rejection))
    }

    /**
     * A Javadoc content for the rejection constructor.
     *
     * @param builder
     *          the name of a rejection builder parameter.
     */
    fun forConstructorOfThrowable(builder: ParameterSpec): CodeBlock = codeBlock {
        val generalPart = fromEscaped("Creates a new instance.")
            .withNewLine()
            .withNewLine()
        val paramsPart = fromEscaped(codeBlock(
            "@param \$N the builder for the rejection", builder
        )).withNewLine()
        add(generalPart.value())
        add(paramsPart.value())
    }

    /**
     * Generates a JavaDoc comment for the builder of the `RejectionThrowable`
     * class corresponding the given rejection type.
     */
    fun forBuilderOf(rejection: MessageType): String {
        val javadocText = codeBlock {
            add(BUILDER_ABSTRACT_TEMPLATE, rejection.name.simpleName)
        }
        return fromEscaped(javadocText.toString())
            .withNewLine()
            .value
    }
}

/**
 * Obtains the first paragraph of the rejection Javadoc.
 *
 * The text is taken as a leading comment of the rejection message
 * declaration wrapped in `<pre>` tags.
 */
private fun classSummaryFor(messageType: MessageType): String {
    val leadingComment = messageType.doc.leadingComment
    if (leadingComment.isEmpty()) {
        return ""
    }
    val javadoc = fromUnescaped(
        // Add line separator to simulate behavior of native Protobuf API.
        leadingComment + System.lineSeparator())
        // Wrap the comment in `<pre>` tags similarly to how Protobuf does
        // it for Javadocs of message types.
        .inPreTags()
        // Add new line to separate the comment from the rest of the Javadoc.
        .withNewLine()
    return javadoc.value
}

/**
 * Obtains a Javadoc note about the rejection message type which is used together
 * with a `RejectionThrowable` class.
 */
private fun protoMessageNote(messageType: MessageType): String {
    val protoType = messageType.name
    val protoMessageNote = codeBlock {
        add(PROTO_MESSAGE_NOTE_TEMPLATE, protoType.packageName, protoType.simpleName)
    }
    return fromEscaped(protoMessageNote.toString())
        .withNewLine()
        .value
}
