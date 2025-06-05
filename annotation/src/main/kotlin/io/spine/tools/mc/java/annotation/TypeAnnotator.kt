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

import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper
import io.spine.base.EntityState
import io.spine.protodata.ast.ProtoFileHeader
import io.spine.protodata.context.findHeader
import io.spine.tools.mc.annotation.ApiOption
import io.spine.tools.mc.annotation.WithOptions
import io.spine.tools.mc.annotation.file
import io.spine.tools.mc.annotation.optionList

/**
 * Adds annotations to the types in the generated code.
 */
internal abstract class TypeAnnotator<T>(
    viewClass: Class<T>
): ProtoAnnotator<T>(viewClass) where T : EntityState<*>, T : WithOptions {

    @OverridingMethodsMustInvokeSuper
    override fun annotate(view: T) {
        view.optionList
            .mapNotNull { ApiOption.findMatching(it) }
            .filter {
                val header = findHeader(view.file)!!
                needsAnnotation(it, header)
            }
            .map { annotationClass(it) }
            .forEach {
                annotateType(view, it)
            }
    }

    /**
     * Tells if the given message type, enum, or a service needs to be annotated
     * assuming the file header.
     *
     * If the file header tells having an outer Java class, options applied
     * at the file level and at type level may semantically duplicate each other.
     *
     * For example, if file options are `java_multiple_files = false` and `(internal_all) = true`,
     * there is no need to have `(internal_type) = true` on a message type.
     *
     * Implementations of this method should check such semantic duplications and
     * return `false` if found, and `true` otherwise.
     */
    protected abstract fun needsAnnotation(apiOption: ApiOption, header: ProtoFileHeader): Boolean

    /**
     * Adds the annotation to the type.
     */
    abstract fun annotateType(view: T, annotationClass: Class<out Annotation>)
}
