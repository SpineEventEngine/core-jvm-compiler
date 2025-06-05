/*
 * Copyright 2025, TeamDev. All rights reserved.
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

package io.spine.tools.mc.java.comparable.action

import com.google.protobuf.Message
import com.intellij.psi.PsiClass
import io.spine.base.FieldPath
import io.spine.protodata.java.ClassName
import io.spine.protodata.java.Expression
import io.spine.protodata.java.FieldDeclaration
import io.spine.protodata.java.ParameterizedTypeName
import io.spine.protodata.java.call
import io.spine.string.camelCase
import io.spine.string.lowerCamelCase
import io.spine.tools.mc.java.base.isNotNested
import io.spine.tools.mc.java.base.root
import io.spine.tools.psi.java.packageName
import java.util.function.Function

/**
 * Builds a static Java field containing the [Comparator] for the given message.
 *
 * An example of the built comparator:
 *
 * ```java
 * private static final java.util.Comparator<com.example.Jogging> comparator =
 *     java.util.Comparator.comparing((com.example.Jogging jogging) -> jogging.getDuration().getHours())
 *                         .thenComparing(com.example.Jogging::getStarted)
 *                         .thenComparing(com.example.Jogging::getFinished);
 * ```
 *
 * @param cls The message class to be used as the comparator's generic parameter.
 * @param reversed If `true`, imposes the reverse of the natural ordering.
 *  If a comparator sorts objects in ascending order, reversed will sort them
 *  in descending order.
 */
internal class ComparatorBuilder(cls: PsiClass, private val reversed: Boolean = false) {

    private val message = ClassName(cls.packageName, cls.name!!)
    private val instance = message.simpleName.lowerCamelCase()
    private val fields = mutableListOf<FieldComparison>()

    /**
     * Builds a private static `comparator` Java field.
     */
    fun build(): FieldDeclaration<Comparator<Message>> {
        val comparator = ClassName(Comparator::class)
        var comparisons = comparator.call<Comparator<Message>>("comparing", fields.first())
        for (i in 1 until fields.size) {
            comparisons = comparisons.chain("thenComparing", fields[i])
        }
        if (reversed) {
            comparisons = comparisons.chain("reversed")
        }
        return FieldDeclaration(
            modifiers = "private static final",
            type = ParameterizedTypeName(comparator, message),
            name = "comparator",
            value = comparisons
        )
    }

    /**
     * Adds the next comparing closure for the given field [path].
     *
     * The comparator should be passed when the field type is neither primitive nor comparable.
     * It is the responsibility of the caller to check it in advance.
     * This builder is responsible only for building a string representation of
     * the comparator.
     *
     * @param path The path to the field.
     * @param comparator The optional comparator to be used for the field values.
     */
    fun comparingBy(path: FieldPath, comparator: Expression<Comparator<Any>>? = null) {
        val extractor = if (path.isNotNested) extractField(path.root) else extractNestedField(path)
        val comparison = FieldComparison(extractor, comparator)
        fields.add(comparison)
    }

    /**
     * Returns a key extractor as a method reference to the getter of the given [fieldName].
     */
    private fun extractField(fieldName: String): FieldExtractor =
        Expression("$message::${fieldName.toJavaGetter()}")

    /**
     * Returns a key extractor as a lambda expression for a nested field
     * denoted by the given [path].
     */
    private fun extractNestedField(path: FieldPath): FieldExtractor {
        val parts = path.fieldNameList
        val joined = parts.joinToString(".") { "${it.toJavaGetter()}()" }
        return Expression("($message $instance) -> $instance.$joined")
    }
}

/**
 * A lambda expression or a getter reference, which extracts the field value
 * for comparison.
 *
 * For example: `Jogging::getStarted` OR `(Jogger jogger) -> jogger.getStarted()`.
 */
private typealias FieldExtractor = Expression<Function<Message, Any>>

/**
 * A comparator to be used for the extracted field value.
 *
 * The comparator is not mandatory. It is passed either for the custom
 * comparison logic or for messages that are not comparable themselves.
 */
private typealias FieldComparator = Expression<Comparator<Any>>

/**
 * The expressions required to perform a comparison by a specific message field.
 *
 * The field [extractor] and its optional [comparator] are going to be passed
 * as arguments to Java `Comparator.comparing()` OR `thenComparing()` methods.
 */
@Suppress("JavaDefaultMethodsNotOverriddenByDelegation") // only the deprecated `toArray()`.
private class FieldComparison(val extractor: FieldExtractor, val comparator: FieldComparator?) :
    List<Expression<*>> by listOfNotNull(extractor, comparator)


/**
 * Converts this [String] with a Protobuf field name to a Java getter.
 *
 * For example, `my_best_field` will be converted to `getMyBestField`.
 *
 * The round brackets are omitted to allow this getter name to be used for composing
 * both a direct invocation and a method reference.
 *
 * For example, `getMyBestField()` and `::getMyBestField`.
 */
private fun String.toJavaGetter() = "get${camelCase()}"
