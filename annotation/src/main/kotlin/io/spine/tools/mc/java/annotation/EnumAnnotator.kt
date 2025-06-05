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

import io.spine.tools.mc.annotation.EnumAnnotations

/**
 * Annotates enum types with the given API level annotation.
 *
 * An `EnumAnnotator` takes the options gathered by
 * [EnumAnnotationsView][io.spine.tools.mc.annotation.EnumAnnotationsView] and applies
 * them to the corresponding Java enum type as annotations from the `io.spine.annotation` package.
 *
 * @see io.spine.tools.mc.annotation.EnumAnnotationsView
 */
internal class EnumAnnotator :
    MessageOrEnumAnnotator<EnumAnnotations>(EnumAnnotations::class.java) {

    override fun annotateType(view: EnumAnnotations, annotationClass: Class<out Annotation>) {
        val enumType = convention.declarationFor(view.type).name
        ApiAnnotation(enumType, annotationClass).let {
            it.registerWith(context)
            it.renderSources(sources)
        }
    }
}
