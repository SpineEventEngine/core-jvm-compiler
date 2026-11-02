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

@file:Suppress("TooManyFunctions")

package io.spine.tools.core.jvm.signal.rejection

import io.spine.code.java.SimpleClassName
import io.spine.tools.compiler.ast.Field
import io.spine.tools.compiler.ast.MessageType
import io.spine.tools.compiler.ast.TypeBase
import io.spine.tools.compiler.ast.isList
import io.spine.tools.compiler.ast.isMap
import io.spine.tools.compiler.ast.typeName
import io.spine.tools.compiler.jvm.ClassName
import io.spine.tools.compiler.jvm.javaCase
import io.spine.tools.compiler.jvm.javaClassName
import io.spine.tools.compiler.jvm.primarySetterName
import io.spine.tools.compiler.jvm.toJavaClass
import io.spine.tools.compiler.type.TypeSystem
import io.spine.tools.core.jvm.signal.rejection.Javadoc.forBuilderOf
import io.spine.tools.core.jvm.signal.rejection.Javadoc.ofBuildMethod
import io.spine.tools.core.jvm.signal.rejection.Javadoc.ofBuilderConstructor
import io.spine.tools.core.jvm.signal.rejection.Javadoc.ofNewBuilderMethod
import io.spine.tools.core.jvm.signal.rejection.Javadoc.ofRejectionMessageMethod
import io.spine.tools.core.jvm.signal.rejection.Method.BUILD
import io.spine.tools.core.jvm.signal.rejection.Method.NEW_BUILDER
import io.spine.tools.core.jvm.signal.rejection.Method.REJECTION_MESSAGE
import io.spine.tools.java.javadoc.JavadocText
import io.spine.validation.Validated

/**
 * Generates code for a rejection builder.
 *
 * A generated builder validates rejection messages using [io.spine.validation.Validate.check].
 *
 * @param rejection The declaration of the rejection.
 * @param messageClass The class of the rejection message.
 * @param throwableClass The class of the rejection type enclosing the builder.
 * @param typeSystem The type system for resolving the types of the rejection fields.
 */
internal class RThrowableBuilderCode internal constructor(
    private val rejection: MessageType,
    private val messageClass: ClassName,
    private val throwableClass: ClassName,
    private val typeSystem: TypeSystem
) {

    /**
     * The simple name of the builder class.
     */
    val simpleClassName: String = SimpleClassName.ofBuilder().value

    /**
     * The name of the builder class.
     */
    val builderClass: ClassName = throwableClass.nested(simpleClassName)

    /**
     * Produces the code of the method creating the builder.
     */
    fun ClassReferences.newBuilderMethod(): String = methodDeclaration(
        javadoc = ofNewBuilderMethod,
        signature = "public static ${name(builderClass)} $NEW_BUILDER()",
        statements = listOf("return new $simpleClassName();")
    )

    /**
     * Produces the code of the builder class.
     */
    fun ClassReferences.builderClassCode(): String = classDeclaration(
        javadoc = forBuilderOf(rejection),
        header = "public static class $simpleClassName",
        members = buildList {
            add(builderField())
            add(constructor())
            rejection.fieldList.forEach { add(setter(it)) }
            add(rejectionMessageMethod())
            add(buildMethod())
        }
    )

    /**
     * Produces the code, which builds and validates the rejection message.
     *
     * The code is not a statement (there is no semicolon) since
     * it is intended to be passed to a constructor.
     *
     * @param builder The name of the variable referring to the builder.
     */
    fun buildRejectionMessage(builder: String): String = "$builder.$REJECTION_MESSAGE()"

    private fun ClassReferences.builderField(): String {
        val messageBuilder = name(messageClass.nested(simpleClassName))
        return "private final $messageBuilder $BUILDER_FIELD = " +
                "${name(messageClass)}.$NEW_BUILDER();"
    }

    private fun constructor(): String = methodDeclaration(
        javadoc = ofBuilderConstructor,
        signature = "private $simpleClassName()",
        statements = listOf()
    )

    private fun ClassReferences.setter(field: Field): String {
        val setterName = field.primarySetterName
        val parameterName = field.name.javaCase()
        val returnType = name(builderClass)
        val parameterType = parameterType(field)
        return methodDeclaration(
            javadoc = field.setterJavadoc(),
            signature = "public $returnType $setterName($parameterType $parameterName)",
            statements = listOf(
                "$BUILDER_FIELD.$setterName($parameterName);",
                "return this;"
            )
        )
    }

    /**
     * Obtains the code referring to the type of the setter parameter for the given field.
     *
     * Types of primitive values are boxed.
     */
    private fun ClassReferences.parameterType(field: Field): String = when {
        field.isMap -> {
            val map = field.type.map
            generic(ClassName(Map::class), map.keyType.toJavaClass(), map.valueType.className())
        }
        field.isList -> generic(ClassName(List::class), field.type.list.className())
        else -> name(field.type.className())
    }

    private fun ClassReferences.generic(cls: ClassName, vararg arguments: ClassName): String =
        name(cls) + arguments.joinToString(prefix = "<", postfix = ">") { name(it) }

    /**
     * Obtains the name of the Java class representing this type.
     *
     * For a primitive type, the boxed class is returned.
     */
    private fun TypeBase.className(): ClassName =
        if (isPrimitive) primitive.toJavaClass() else typeName.javaClassName(typeSystem)

    /**
     * Produces the code of the method which creates the rejection message by calling
     * the `build()` method of the message builder.
     *
     * The method returns the message annotated as [Validated], which makes Validation
     * generate the code checking the message.
     */
    private fun ClassReferences.rejectionMessageMethod(): String {
        val returnType = annotated(messageClass, ClassName(Validated::class))
        return methodDeclaration(
            javadoc = ofRejectionMessageMethod,
            signature = "private $returnType $REJECTION_MESSAGE()",
            statements = listOf("return $BUILDER_FIELD.$BUILD();")
        )
    }

    private fun ClassReferences.buildMethod(): String {
        val throwableType = name(throwableClass)
        return methodDeclaration(
            javadoc = ofBuildMethod,
            signature = "public $throwableType $BUILD()",
            statements = listOf("return new $throwableType(this);")
        )
    }
}

private const val BUILDER_FIELD = "builder"

/**
 * Obtains the Javadoc of the setter for this field.
 *
 * Returns an empty string if the field has no leading comment.
 */
private fun Field.setterJavadoc(): String {
    if (doc.leadingComment.isEmpty()) {
        return ""
    }
    // Add line separator to simulate the behavior of native Protobuf API.
    val leadingComment = doc.leadingComment + System.lineSeparator()
    return wrapInPre(leadingComment)
}

private fun wrapInPre(text: String): String =
    JavadocText.fromUnescaped(text)
        .inPreTags()
        .value()
