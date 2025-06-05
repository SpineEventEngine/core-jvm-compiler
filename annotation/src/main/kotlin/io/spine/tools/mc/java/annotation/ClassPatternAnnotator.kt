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

package io.spine.tools.mc.java.annotation

import io.spine.protodata.java.annotation.TypeAnnotation
import io.spine.protodata.render.SourceFile
import io.spine.protodata.render.SourceFileSet
import io.spine.protodata.render.forEachOfLanguage
import io.spine.tools.code.Java

/**
 * Annotates classes matching [name patterns specified][Settings.getInternalClassPatternList]
 * in [Settings] as [`internal`][Settings.AnnotationTypes.getInternal].
 *
 * The annotation type to be used is obtained from
 * the [`internal`][Settings.AnnotationTypes.getInternal] field of
 * the [Settings.AnnotationTypes] message.
 */
internal class ClassPatternAnnotator : PatternAnnotator() {

    override fun loadPatterns(): List<String> =
        settings.internalClassPatternList

    override fun render(sources: SourceFileSet) {
        sources.forEachOfLanguage<Java> { file ->
            val className = file.qualifiedTopClassName()
            if (matches(className)) {
                annotate(sources, file)
            }
        }
    }

    private fun annotate(sources: SourceFileSet, file: SourceFile<Java>) {
        TopClassAnnotation(annotationClass, file = file).let {
            it.registerWith(context)
            it.renderSources(sources)
        }
    }
}

private class TopClassAnnotation(annotation: Class<Annotation>, file: SourceFile<Java>) :
    TypeAnnotation<Annotation>(annotation, file = file) {
    override fun renderAnnotationArguments(file: SourceFile<Java>): String = ""
}
