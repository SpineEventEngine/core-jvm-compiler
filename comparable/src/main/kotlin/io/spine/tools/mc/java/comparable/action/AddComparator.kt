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

import com.google.protobuf.Empty
import io.spine.base.FieldPath
import io.spine.base.copy
import io.spine.base.fieldPath
import io.spine.compare.ComparatorRegistry
import io.spine.option.CompareByOption
import io.spine.protodata.Compilation
import io.spine.protodata.ast.Cardinality.CARDINALITY_SINGLE
import io.spine.protodata.ast.MessageType
import io.spine.protodata.ast.Option
import io.spine.protodata.ast.PrimitiveType.PT_UNKNOWN
import io.spine.protodata.ast.PrimitiveType.TYPE_BYTES
import io.spine.protodata.ast.cardinality
import io.spine.protodata.ast.find
import io.spine.protodata.ast.name
import io.spine.protodata.ast.option
import io.spine.protodata.ast.qualifiedName
import io.spine.protodata.ast.unpack
import io.spine.protodata.check
import io.spine.protodata.context.CodegenContext
import io.spine.protodata.java.ClassName
import io.spine.protodata.java.MethodCall
import io.spine.protodata.java.javaClass
import io.spine.protodata.java.render.DirectMessageAction
import io.spine.protodata.java.toPsi
import io.spine.protodata.render.SourceFile
import io.spine.tools.code.Java
import io.spine.tools.mc.java.GeneratedAnnotation
import io.spine.tools.mc.java.base.joined
import io.spine.tools.mc.java.base.resolve
import io.spine.tools.mc.java.comparable.WellKnownComparables.isWellKnownComparable
import io.spine.tools.psi.addFirst

/**
 * Builds and inserts a static `comparator` field into the messages that qualify
 * as comparable.
 *
 * @param type The type of the message.
 * @param file The source code to which the action is applied.
 * @param context The code generation context in which this action runs.
 */
public class AddComparator(
    type: MessageType,
    file: SourceFile<Java>,
    context: CodegenContext
) : DirectMessageAction<Empty>(type, file, Empty.getDefaultInstance(), context) {

    /** The declaration of the [CompareByOption] option in the [type]. */
    private val option: Option by lazy {
        type.option<CompareByOption>()
    }

    override fun doRender() {
        val compareBy = option.unpack<CompareByOption>()
        val comparisonFields = compareBy.fieldList.map(::toComparisonField)
        Compilation.check(comparisonFields.isNotEmpty(), type.file, option.span) {
            "The `(compare_by)` option declared in the type `${type.qualifiedName}`" +
                    " should have at least one field specified."
        }

        val comparator = ComparatorBuilder(cls, compareBy.descending)
        comparisonFields.forEach { comparator.comparingBy(it) }

        val javaField = comparator.build().toPsi()
            .apply { addFirst(GeneratedAnnotation.forPsi()) }
        cls.addAfter(javaField, cls.lBrace)
    }

    /**
     * Maps the field [path] to an appropriate instance of [ComparisonField],
     * depending on the field type.
     */
    @Suppress("SwallowedException") // We transform "unknown field" into the compilation error.
    private fun toComparisonField(path: String): ComparisonField {
        val fieldPath = path.toFieldPath()
        val field = try {
            typeSystem.resolve(fieldPath, type)
        } catch (_: IllegalStateException) {
            Compilation.error(type.file, option.span) {
                val isImmediate = !path.contains(".")
                val pathOrField = if (isImmediate) "name" else "path"
                "Unable to find a field with the $pathOrField `$path`" +
                        " referred in the `(compare_by)` option" +
                        " in the type `${type.qualifiedName}`."
            }
        }

        val fieldType = field.type

        Compilation.check(field.type.cardinality == CARDINALITY_SINGLE, type.file, option.span) {
            "Repeated fields or maps cannot participate in comparison." +
                    " The field `${field.qualifiedName}` has the type" +
                    " `${fieldType.name}` which does not support comparison." +
                    " Please see the documentation of the `(compare_by)` option" +
                    " for the details on the supported field types."
        }

        return when {
            fieldType.isPrimitive -> PrimitiveComparisonField(fieldPath, fieldType.primitive)
            fieldType.isEnum -> EnumComparisonField(fieldPath)
            fieldType.isMessage -> {
                val typeName = fieldType.message
                val (type, header) = typeSystem.findMessage(typeName)!!
                val javaClass = type.javaClass(header)
                if (javaClass == null) {
                    MessageComparisonField(fieldPath, type)
                } else {
                    ExternalMessageComparisonField(fieldPath, type, javaClass)
                }
            }

            else -> unsupportedFieldType(fieldPath, fieldType.name)
        }
    }

    /**
     * Adds the comparison [field] to this [ComparatorBuilder].
     *
     * The requirements to the comparison fields are described in docs to [CompareByOption]
     * option in detail.
     *
     * In short, the following fields are accepted:
     *
     * 1. All primitives except for byte arrays.
     * 2. All enumerations (Java enums are implicitly comparable).
     * 3. Messages with [CompareByOption] option.
     * 4. External messages for which [ComparatorRegistry] has a comparator.
     * 5. [WellKnownComparables][io.spine.tools.mc.java.comparable.WellKnownComparables].
     */
    private fun ComparatorBuilder.comparingBy(field: ComparisonField) {
        val path = field.path
        when (field) {
            is EnumComparisonField -> comparingBy(path)

            is PrimitiveComparisonField -> {
                Compilation.check(field.type != PT_UNKNOWN, type.file, option.span) {
                    "The field `${path.joined}`referred in the `(compare_by) option" +
                            " has an unknown primitive type:" +
                            " `${field.type.name}`."
                }
                Compilation.check(field.type != TYPE_BYTES, type.file, option.span) {
                    "The field `${path.joined}` referred in the `(compare_by)` option" +
                            " declared in the type `${type.qualifiedName}`" +
                            " has a non-comparable `bytes` type."
                }
                comparingBy(path)
            }

            is MessageComparisonField -> {
                Compilation.check(field.type.hasCompareByOption, type.file, option.span) {
                    "The type of the `${path.joined}` field (`${field.type.qualifiedName}`)" +
                            " referred in the `(compare_by)` option" +
                            " should have the `(compare_by)` option itself" +
                            " to participate in the comparison."
                }
                comparingBy(path)
            }

            is ExternalMessageComparisonField -> comparingBy(field)
        }
    }

    /**
     * Adds the external message field to this [ComparatorBuilder].
     *
     * This method expects the given [field] to be one of the following:
     *
     * 1. An external message with [CompareByOption] and without
     * a comparator in [ComparatorRegistry].
     * 2. An external message without the option, but with a comparator
     * in the registry.
     * 3. [Well-known comparable][io.spine.tools.mc.java.comparable.WellKnownComparables].
     */
    private fun ComparatorBuilder.comparingBy(field: ExternalMessageComparisonField) {
        val path = field.path
        val clazz = field.clazz
        val fromRegistry = ComparatorRegistry.find(clazz)
        val hasCompareByOption = field.type.hasCompareByOption
        when {
            hasCompareByOption -> {
                Compilation.check(fromRegistry == null, type.file, option.span) {
                    "The type of the `${path.joined}` field must either have" +
                            " the `(compare_by)` option specified OR" +
                            " have a `Comparator` registered in the `ComparatorRegistry`," +
                            " but not both simultaneously."
                }
                comparingBy(path)
            }

            fromRegistry != null -> {
                val comparator = MethodCall<Comparator<Any>>(
                    ClassName(ComparatorRegistry::class),
                    "get",
                    ClassName(clazz).clazz
                )
                comparingBy(path, comparator)
            }

            clazz.isWellKnownComparable -> comparingBy(path.copy { fieldName.add("value") })

            else -> unsupportedFieldType(path, field.type.qualifiedName)
        }
    }

    /**
     * Throws [Compilation.Error] to indicate that the passed [fieldPath]
     * denotes a field with an unsupported type.
     *
     * This error is meant to serve as a safe net for cases when the passed field
     * type is unexpected for the plugin. For example, Protobuf may introduce a new field
     * type or cardinality. If this happens, we should add the support of such a type
     * to this plugin. Otherwise, "safe net" errors are thrown.
     *
     * Note: the names of method arguments are prefixed with "field" intentionally.
     * So not to clash with [type] class member.
     */
    private fun unsupportedFieldType(fieldPath: FieldPath, fieldType: String): Nothing =
        Compilation.error(type.file, option.span) {
            "The field `${fieldPath.joined}` declared in the message `${type.qualifiedName}`" +
                    " has the type `$fieldType` which does not support the comparison." +
                    " Supported field types are: primitives, enums, and comparable messages." +
                    " Please see the `(compare_by)` option documentation for details."
        }
}

private val MessageType.hasCompareByOption: Boolean
    get() = optionList.find<CompareByOption>() != null

/**
 * Transforms this potentially dot-delimited string into [FieldPath].
 *
 * If there are no dots in this string the returned [FieldPath] contains
 * only this string.
 */
private fun String.toFieldPath() = fieldPath {
    fieldName.addAll(this@toFieldPath.split("."))
}
