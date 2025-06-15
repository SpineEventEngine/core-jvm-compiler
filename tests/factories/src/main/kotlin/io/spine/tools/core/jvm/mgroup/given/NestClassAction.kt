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

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiMethod
import com.intellij.psi.javadoc.PsiDocComment
import io.spine.protodata.context.CodegenContext
import io.spine.protodata.ast.MessageType
import io.spine.protodata.render.SourceFile
import io.spine.protodata.java.javaClassName
import io.spine.tools.code.Java
import io.spine.protodata.java.render.CreateNestedClass
import io.spine.tools.psi.addFirst
import io.spine.tools.psi.java.addLast
import io.spine.tools.psi.java.Environment.elementFactory
import org.intellij.lang.annotations.Language

/**
 * A stub rendering action that adds a nested class called [`StudentId`][CLASS_NAME].
 */
class NestClassAction(type: MessageType, file: SourceFile<Java>, context: CodegenContext) :
    CreateNestedClass(type, file, CLASS_NAME, context) {

    override fun tuneClass() {
        cls.addLast(method)
    }

    private val javadoc: PsiDocComment by lazy {
        @Language("JAVA") @Suppress("EmptyClass")
        val doc = elementFactory.createDocCommentFromText("""
            /**
             * Returns the message class for test purposes.
             */           
            """.trimIndent()
        )
        doc
    }

    private val method: PsiMethod by lazy {
        val messageClassName = type.javaClassName(typeSystem)
        @Language("JAVA") @Suppress("EmptyClass")
        val newMethod = elementFactory.createMethodFromText("""
            public static Class messageClass() {
              return $messageClassName.class;    
            }                                
            """.trimIndent(), cls
        )
        newMethod.addFirst(javadoc)
        newMethod
    }

    @Language("JAVA") @Suppress("EmptyClass")
    override fun classJavadoc(): String = ""

    override fun createAnnotation(): PsiAnnotation? = null

    companion object {
        const val CLASS_NAME = "SomeNestedClass"
    }
}
