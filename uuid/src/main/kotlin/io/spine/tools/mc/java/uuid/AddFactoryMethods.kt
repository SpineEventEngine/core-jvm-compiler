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

package io.spine.tools.mc.java.uuid

import com.google.protobuf.Empty
import com.intellij.psi.PsiClass
import com.intellij.psi.javadoc.PsiDocComment
import io.spine.base.UuidValue
import io.spine.protodata.ast.MessageType
import io.spine.protodata.context.CodegenContext
import io.spine.protodata.java.render.DirectMessageAction
import io.spine.protodata.render.SourceFile
import io.spine.tools.code.Java
import io.spine.tools.java.reference
import io.spine.tools.mc.java.GeneratedAnnotation
import io.spine.tools.psi.addFirst
import io.spine.tools.psi.java.Environment.elementFactory
import io.spine.tools.psi.java.addLast
import java.util.*
import org.intellij.lang.annotations.Language

/**
 * Updates the code of the message which qualifies as [UuidValue] type by
 * adding `generate()` and `of(String)` static factory methods.
 *
 * The class is public because its fully qualified name is used as a default
 * value in [UuidSettings][io.spine.tools.mc.java.gradle.settings.UuidSettings].
 *
 * @param type The type of the message.
 * @param file The source code to which the action is applied.
 * @param context The code generation context in which this action runs.
 */
public class AddFactoryMethods(
    type: MessageType,
    file: SourceFile<Java>,
    context: CodegenContext
) : DirectMessageAction<Empty>(type, file, Empty.getDefaultInstance(), context)  {

    override fun doRender() {
        MethodGenerate(cls).render()
        MethodOf(cls).render()
    }
}

/**
 * Renders a static method `generate()` which creates an instance of [UuidValue]
 * using [UUID.randomUUID] value.
 */
private class MethodGenerate(private val cls: PsiClass) {

    private val javadoc: PsiDocComment by lazy {
        @Language("JAVA") @Suppress("EmptyClass")
        val doc = elementFactory.createDocCommentFromText("""
            /**
             * Creates a new instance with a random UUID value.
             *
             * @see java.util.UUID#randomUUID   
             */
            """.trimIndent()
        )
        doc
    }

    fun render() {
        @Language("JAVA") @Suppress("EmptyClass", "NewClassNamingConvention")
        val method = elementFactory.createMethodFromText("""
            public static ${cls.name} generate() {
                return newBuilder()
                    .setUuid(${UUID::class.java.reference}.randomUUID().toString())
                    .build();                            
            }            
            """.trimIndent(), cls
        )
        method.run {
            val annotation = GeneratedAnnotation.forPsi()
            addFirst(annotation)
            addFirst(javadoc)
        }
        cls.addLast(method)
    }
}

/**
 * Renders a static method `of()` which creates an instance of [UuidValue]
 * using the given string value.
 *
 * The value is checked using [UuidValue.checkValid].
 */
private class MethodOf(private val cls: PsiClass) {

    private val javadoc: PsiDocComment by lazy {
        @Language("JAVA") @Suppress("EmptyClass")
        val doc = elementFactory.createDocCommentFromText("""
            /**
             * Creates a new instance from the given value.
             * 
             * @throws ${IllegalArgumentException::class.java.reference} 
             *          if the passed value is not a valid UUID string
             */
            """.trimIndent()
        )
        doc
    }

    fun render() {
        @Language("JAVA") @Suppress("EmptyClass", "NewClassNamingConvention")
        val method = elementFactory.createMethodFromText("""
            public static ${cls.name} of(String uuid) {
                ${UuidValue::class.java.reference}.checkValid(uuid);
                return newBuilder()
                    .setUuid(uuid)
                    .build();                            
            }                
            """.trimIndent(), cls
        )
        method.run {
            val annotation = GeneratedAnnotation.forPsi()
            addFirst(annotation)
            addFirst(javadoc)
        }
        cls.addLast(method)
    }
}
