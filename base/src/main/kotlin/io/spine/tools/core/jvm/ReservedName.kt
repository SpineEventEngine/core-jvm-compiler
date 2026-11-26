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

package io.spine.tools.core.jvm

import io.spine.tools.compiler.Compilation
import io.spine.tools.compiler.ast.MessageType
import io.spine.tools.compiler.ast.Span
import io.spine.tools.compiler.ast.TypeName
import io.spine.tools.compiler.ast.isTopLevel
import io.spine.tools.compiler.ast.qualifiedName
import io.spine.tools.compiler.check
import io.spine.tools.compiler.jvm.javaClassName
import io.spine.tools.compiler.type.TypeSystem
import io.spine.tools.compiler.type.enum
import io.spine.tools.compiler.type.message

/**
 * The name of a class which the compiler nests in the Java class of a message.
 *
 * The Java code does not compile if a class around the nested one, or a class declared next to it,
 * has the same name. Therefore, the following cannot have the reserved name:
 *
 *  1. The message itself, and the messages in which it is nested.
 *  2. The messages and enums nested directly in the message.
 *   Types nested deeper may have any names.
 *  3. The outer Java class of the file, if the class of the message is nested in it.
 *
 * @param value The reserved name.
 * @param owner The message for which the class is generated.
 * @param ownerKind What the [owner] is called in the error.
 * @param typeSystem The type system to obtain the types around the [owner] and nested in it.
 */
internal class ReservedName(
    private val value: String,
    private val owner: MessageType,
    private val ownerKind: String,
    private val typeSystem: TypeSystem
) {

    /**
     * The [owner] and the messages in which it is nested, starting from the [owner].
     */
    private val messages: List<MessageType> by lazy {
        generateSequence(owner) {
            if (it.isTopLevel) null else typeSystem.message(it.declaredIn)
        }.toList()
    }

    /**
     * Reports a compilation error if the name is used by one of the types for which it is reserved.
     */
    fun check() {
        messages.forEach {
            checkType(type = it.name, kind = MESSAGE, span = it.span)
        }
        owner.nestedMessagesList.forEach {
            checkType(type = it, kind = MESSAGE, span = typeSystem.message(it).span)
        }
        owner.nestedEnumsList.forEach {
            checkType(type = it, kind = ENUM, span = typeSystem.enum(it).span)
        }
        checkOuterClass()
    }

    private fun checkType(type: TypeName, kind: String, span: Span) =
        Compilation.check(condition = type.simpleName != value, file = owner.file, span = span) {
            "The $kind `${type.qualifiedName}` cannot have the name `$value`." +
                    " $reason" +
                    " Please rename the $kind."
        }

    /**
     * Checks the outermost Java class.
     *
     * If it is the class of a message, the name of this message is already checked.
     * Otherwise, it is the outer class of the file.
     */
    private fun checkOuterClass() {
        val outermost = owner.javaClassName(typeSystem).simpleNames.first()
        Compilation.check(condition = outermost != value, file = owner.file, span = owner.span) {
            "The outer Java class of the file cannot have the name `$value`." +
                    " $reason" +
                    " Please set the `java_outer_classname` option to another name."
        }
    }

    private val reason: String
        get() = "This name is reserved for the class which the compiler generates" +
                " for the $ownerKind `${owner.qualifiedName}`."

    private companion object {
        const val MESSAGE = "message"
        const val ENUM = "enum"
    }
}
