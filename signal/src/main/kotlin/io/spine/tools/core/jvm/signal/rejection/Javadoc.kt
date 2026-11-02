/*
 * Copyright 2026 CodeMatters, Lda.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package io.spine.tools.core.jvm.signal.rejection

import io.spine.annotation.VisibleForTesting
import io.spine.tools.compiler.ast.MessageType
import io.spine.tools.java.javadoc.JavadocText.fromEscaped
import io.spine.tools.java.javadoc.JavadocText.fromUnescaped
import io.spine.tools.core.jvm.signal.rejection.Javadoc.PROTO_MESSAGE_NOTE_TEMPLATE

/**
 * Pieces of Javadoc code used in the generated code of rejections.
 *
 * The templates contain the `$L` placeholders, which are replaced with values in order.
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
    fun forThrowableOf(rejection: MessageType): String =
        classSummaryFor(rejection) + protoMessageNote(rejection)

    /**
     * A Javadoc content for the rejection constructor.
     *
     * @param builder The name of a rejection builder parameter.
     */
    fun forConstructorOfThrowable(builder: String): String {
        val generalPart = fromEscaped("Creates a new instance.")
            .withNewLine()
            .withNewLine()
        val paramsPart = fromEscaped("@param $builder the builder for the rejection")
            .withNewLine()
        return generalPart.value() + paramsPart.value()
    }

    /**
     * Generates a JavaDoc comment for the builder of the `RejectionThrowable`
     * class corresponding the given rejection type.
     */
    fun forBuilderOf(rejection: MessageType): String {
        val javadocText = BUILDER_ABSTRACT_TEMPLATE.withValues(rejection.name.simpleName)
        return fromEscaped(javadocText)
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
 * Obtains a Javadoc note about the rejection message type that is used together
 * with a `RejectionThrowable` class.
 */
private fun protoMessageNote(messageType: MessageType): String {
    val protoType = messageType.name
    val protoMessageNote =
        PROTO_MESSAGE_NOTE_TEMPLATE.withValues(protoType.packageName, protoType.simpleName)
    return fromEscaped(protoMessageNote)
        .withNewLine()
        .value
}

/**
 * Replaces the `$L` placeholders of this template with the given values in order.
 */
private fun String.withValues(vararg values: String): String =
    values.fold(this) { text, value -> text.replaceFirst(PLACEHOLDER, value) }

private const val PLACEHOLDER = "\$L"
