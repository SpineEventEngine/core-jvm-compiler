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

import io.spine.annotation.Generated
import io.spine.base.RejectionThrowable
import io.spine.tools.compiler.ast.MessageType
import io.spine.tools.compiler.jvm.ClassName
import io.spine.tools.compiler.jvm.MessageOrEnumConvention
import io.spine.tools.compiler.jvm.StringLiteral
import io.spine.tools.compiler.type.TypeSystem
import io.spine.tools.core.jvm.GeneratedAnnotation
import io.spine.tools.core.jvm.signal.rejection.ClassReferences.Companion.javaFile
import io.spine.tools.core.jvm.signal.rejection.Javadoc.forConstructorOfThrowable
import io.spine.tools.core.jvm.signal.rejection.Javadoc.forThrowableOf

/**
 * The code of a generated rejection type.
 *
 * The generated type extends [RejectionThrowable] and encloses an instance of the
 * corresponding [rejection message][io.spine.base.RejectionMessage].
 *
 * @param javaPackage The name of the Java package where the rejection type should be generated.
 * @param rejection The declaration of the rejection.
 * @param typeSystem The type system for resolving the types of the rejection fields.
 */
internal class RThrowableCode(
    javaPackage: String,
    private val rejection: MessageType,
    typeSystem: TypeSystem
) {

    private val throwableClass = ClassName(javaPackage, rejection.name.simpleName)
    private val messageClass: ClassName =
        MessageOrEnumConvention(typeSystem).declarationFor(rejection.name).name
    private val builder = RThrowableBuilderCode(
        rejection = rejection,
        messageClass = messageClass,
        throwableClass = throwableClass,
        typeSystem = typeSystem
    )

    /**
     * Produces the code of the Java file declaring the rejection type.
     */
    fun javaFileCode(): String =
        javaFile(throwableClass, nested = setOf(builder.simpleClassName)) {
            throwableClassCode()
        }

    private fun ClassReferences.throwableClassCode(): String {
        val generated = name(ClassName(Generated::class))
        val generatedValue = StringLiteral(GeneratedAnnotation.defaultValue)
        val superclass = name(ClassName(RejectionThrowable::class))
        return classDeclaration(
            javadoc = forThrowableOf(rejection),
            header = "@$generated($generatedValue)\n" +
                    "public class ${throwableClass.simpleName} extends $superclass",
            members = listOf(
                SERIAL_VERSION_UID_FIELD,
                throwableConstructor(),
                messageThrownMethod(),
                with(builder) { newBuilderMethod() },
                with(builder) { builderClassCode() }
            )
        )
    }

    private fun ClassReferences.throwableConstructor(): String = methodDeclaration(
        javadoc = forConstructorOfThrowable(BUILDER_PARAMETER),
        signature = "private ${throwableClass.simpleName}" +
                "(${name(builder.builderClass)} $BUILDER_PARAMETER)",
        statements = listOf("super(${builder.buildRejectionMessage(BUILDER_PARAMETER)});")
    )

    private fun ClassReferences.messageThrownMethod(): String {
        val override = name(ClassName(Override::class))
        val messageType = name(messageClass)
        return methodDeclaration(
            signature = "@$override\npublic $messageType ${messageThrown.name()}()",
            statements = listOf("return ($messageType) super.${messageThrown.signature()};")
        )
    }
}

private val messageThrown = NoArgMethod("messageThrown")

private const val SERIAL_VERSION_UID_FIELD = "private static final long serialVersionUID = 0L;"

/**
 * The name of the constructor parameter accepting the builder of the rejection type.
 */
private const val BUILDER_PARAMETER = "builder"
