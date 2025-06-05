/*
 * Copyright 2024, TeamDev. All rights reserved.
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

package io.spine.tools.mc.java.annotation

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
import io.spine.protodata.render.SourceFile
import io.spine.protodata.render.SourceFileSet
import io.spine.protodata.render.forEachOfLanguage
import io.spine.tools.code.Java
import io.spine.tools.java.isRepeatable
import io.spine.tools.java.reference
import io.spine.tools.psi.java.annotate
import io.spine.tools.psi.java.execute

/**
 * Annotates methods matching [name patterns specified][Settings.getInternalMethodNameList]
 * in [Settings] as [`internal`][Settings.AnnotationTypes.getInternal].
 *
 * The annotation type to be used is obtained from
 * the [`internal`][Settings.AnnotationTypes.getInternal] field of
 * the [Settings.AnnotationTypes] message.
 */
internal class MethodPatternAnnotator : PatternAnnotator() {

    override fun loadPatterns(): List<String> =
        settings.internalMethodNameList

    private val annotationCode: String by lazy {
        "@${annotationClass.reference}"
    }

    override fun render(sources: SourceFileSet) {
        sources.forEachOfLanguage<Java> {
            annotateIn(it)
        }
    }

    private fun annotateIn(file: SourceFile<Java>) {
        var updated = false
        val javaFile = file.psi() as PsiJavaFile
        execute {
            javaFile.classes.forEach {
                if (annotateInClass(it)) {
                    updated = true
                }
            }
        }
        if (updated) {
            val updatedCode = javaFile.text
            file.overwrite(updatedCode)
        }
    }

    private fun annotateInClass(cls: PsiClass): Boolean {
        var updated = false
        cls.methods.filter {
            matches(it.name)
        }.forEach {
            if (annotate(it)) {
                updated = true
            }
        }
        return updated
    }

    private fun annotate(method: PsiMethod): Boolean {
        val alreadyAnnotated = method.hasAnnotation(annotationClass.reference)
        if (alreadyAnnotated && !annotationClass.isRepeatable) {
            return false
        }
        method.annotate(annotationCode)
        return true
    }
}
