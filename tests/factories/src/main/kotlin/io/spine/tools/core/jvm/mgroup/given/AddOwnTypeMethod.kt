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

package io.spine.tools.core.jvm.mgroup.given

import com.google.protobuf.Empty
import com.intellij.psi.PsiMethod
import com.intellij.psi.javadoc.PsiDocComment
import io.spine.tools.compiler.context.CodegenContext
import io.spine.tools.compiler.ast.MessageType
import io.spine.tools.compiler.render.SourceFile
import io.spine.tools.compiler.jvm.javaClassName
import io.spine.tools.code.Java
import io.spine.tools.compiler.jvm.render.CreateNestedClass
import io.spine.tools.psi.addFirst
import io.spine.tools.psi.java.addLast
import io.spine.tools.psi.java.Environment.elementFactory
import org.intellij.lang.annotations.Language
import io.spine.tools.compiler.jvm.render.DirectMessageAction
import io.spine.tools.java.reference
import io.spine.type.MessageType as MType

/**
 * A stub rendering action which adds a static method called `ownClass` which
 * obtains [io.spine.type.MessageType] instance which represents the generated message class.
 */
class AddOwnTypeMethod(
    type: MessageType,
    file: SourceFile<Java>,
    context: CodegenContext
) : DirectMessageAction<Empty>(type, file, Empty.getDefaultInstance(), context) {

    override fun doRender() {
        cls.addLast(method)
    }

    private val returnType: String = MType::class.java.reference

    private val javadoc: PsiDocComment by lazy {
        @Language("JAVA") @Suppress("EmptyClass")
        val doc = elementFactory.createDocCommentFromText("""
            /**
             * Returns an instance of {@link $returnType} describing this message class.
             */
            """.trimIndent()
        )
        doc
    }

    private val method: PsiMethod by lazy {
        @Language("JAVA") @Suppress("EmptyClass")
        val newMethod = elementFactory.createMethodFromText("""
            public static $returnType ownType() {
              return new $returnType(getDescriptor());    
            }                                
            """.trimIndent(), cls
        )
        newMethod.addFirst(javadoc)
        newMethod
    }
}
