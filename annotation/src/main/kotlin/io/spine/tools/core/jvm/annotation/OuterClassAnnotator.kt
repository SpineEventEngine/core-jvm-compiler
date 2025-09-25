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

package io.spine.tools.core.jvm.annotation

import io.spine.tools.compiler.ast.ProtoFileHeader
import io.spine.tools.compiler.jvm.ClassName
import io.spine.tools.compiler.jvm.javaOuterClassName
import io.spine.tools.compiler.jvm.javaPackage
import io.spine.tools.core.annotation.ApiOption

/**
 * Annotates the outer class of a `.proto` file IFF `java_multiple_files` option is set to `true`.
 *
 * @see OuterClassAnnotationDiscovery
 */
internal class OuterClassAnnotator :
    TypeAnnotator<OuterClassAnnotations>(OuterClassAnnotations::class.java) {

    override fun annotateType(view: OuterClassAnnotations, annotationClass: Class<out Annotation>) {
        val outerClassName = view.header.javaOuterClassName()
        val packageName = view.header.javaPackage()
        val className = ClassName(packageName, outerClassName)
        ApiAnnotation(className, annotationClass).let {
            it.registerWith(context)
            it.renderSources(sources)
        }
    }

    /**
     * Always returns `true` assuming that if this renderer is invoked, the outer class
     * to be annotated was discovered by the [OuterClassAnnotationDiscovery] process.
     */
    override fun needsAnnotation(apiOption: ApiOption, header: ProtoFileHeader): Boolean = true
}
