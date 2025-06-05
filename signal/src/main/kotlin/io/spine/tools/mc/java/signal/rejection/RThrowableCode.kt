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

package io.spine.tools.mc.java.signal.rejection

import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import io.spine.base.RejectionThrowable
import io.spine.logging.WithLogging
import io.spine.protodata.ast.MessageType
import io.spine.protodata.java.MessageOrEnumConvention
import io.spine.protodata.type.TypeSystem
import io.spine.tools.java.classSpec
import io.spine.tools.java.constructorSpec
import io.spine.tools.java.methodSpec
import io.spine.tools.mc.java.GeneratedAnnotation
import io.spine.tools.mc.java.signal.rejection.Javadoc.forConstructorOfThrowable
import io.spine.tools.mc.java.signal.rejection.Javadoc.forThrowableOf
import javax.lang.model.element.Modifier.FINAL
import javax.lang.model.element.Modifier.PRIVATE
import javax.lang.model.element.Modifier.PUBLIC
import javax.lang.model.element.Modifier.STATIC
import com.squareup.javapoet.ClassName as PoClassName

/**
 * A spec for a generated rejection type.
 *
 * The generated type extends [RejectionThrowable] and encloses an instance of the
 * corresponding [rejection message][io.spine.base.RejectionMessage].
 *
 * @param javaPackage The name of the Java package where the rejection type should be generated.
 * @param rejection The declaration of the rejection.
 */
internal class RThrowableCode(
    val javaPackage: String,
    val rejection: MessageType,
    typeSystem: TypeSystem
) : WithLogging {

    private val messageOrEnumConvention = MessageOrEnumConvention(typeSystem)
    private val simpleClassName: String = rejection.name.simpleName
    private val messageClass: PoClassName
    private val builder: RThrowableBuilderCode

    init {
        val clsName = messageOrEnumConvention.declarationFor(rejection.name).name.canonical
        messageClass = PoClassName.bestGuess(clsName)
        val throwableClass = PoClassName.get(javaPackage, simpleClassName)
        builder = RThrowableBuilderCode(
            rejection,
            messageClass,
            throwableClass,
            typeSystem
        )
    }

    fun toPoet(): TypeSpec = classSpec(simpleClassName) {
        addJavadoc(forThrowableOf(rejection))
        addAnnotation(GeneratedAnnotation.forJavaPoet())
        addModifiers(PUBLIC)
        superclass(RejectionThrowable::class.java)
        addField(serialVersionUID())
        addMethod(constructor())
        addMethod(messageThrown())
        addMethod(builder.newBuilder())
        addType(builder.toPoet())
    }

    private fun constructor(): MethodSpec {
        logger.atDebug().log {
            "Creating the constructor for the type `${rejection}`."
        }
        return constructorSpec {
            val builderParameter = builder.asParameter()
            val buildRejectionMessage = builder.buildRejectionMessage()

            addJavadoc(forConstructorOfThrowable(builderParameter))
            addModifiers(PRIVATE)
            addParameter(builderParameter)
            addStatement("super(\$L)", buildRejectionMessage.toString())
        }
    }

    private fun messageThrown(): MethodSpec {
        val methodSignature = messageThrown.signature()
        logger.atDebug().log {
            "Adding method `$methodSignature`."
        }
        return methodSpec(messageThrown.name()) {
            val returnType = messageClass
            addAnnotation(Override::class.java)
            addModifiers(PUBLIC)
            returns(returnType)
            addStatement("return (\$T) super.\$L", returnType, methodSignature)
        }
    }
}

private val messageThrown = NoArgMethod("messageThrown")

private fun serialVersionUID(): FieldSpec {
    return FieldSpec.builder(
        Long::class.javaPrimitiveType,
        "serialVersionUID", PRIVATE, STATIC, FINAL)
        .initializer("0L")
        .build()
}
