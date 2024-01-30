/*
 * Copyright 2024, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import io.spine.base.EntityState
import io.spine.protodata.codegen.java.JavaRenderer
import io.spine.protodata.renderer.SourceFileSet
import io.spine.tools.mc.annotation.ApiAnnotationsPlugin
import io.spine.protodata.settings.loadSettings
import io.spine.tools.mc.annotation.ApiOption
import io.spine.tools.mc.annotation.ApiOption.BETA
import io.spine.tools.mc.annotation.ApiOption.EXPERIMENTAL
import io.spine.tools.mc.annotation.ApiOption.INTERNAL
import io.spine.tools.mc.annotation.ApiOption.SPI

/**
 * An abstract base for annotation renderers.
 *
 * @param T the type of the view state which contains information about annotated types.
 */
internal abstract class Annotator<T>(
    private val viewClass: Class<T>
) : JavaRenderer() where T : EntityState<*> {

    protected lateinit var sources: SourceFileSet

    override val consumerId: String
        get() = ApiAnnotationsPlugin::class.java.canonicalName

    protected val settings: Settings by lazy {
        loadSettings<Settings>()
    }

    protected fun annotationClass(apiOption: ApiOption): Class<Annotation> {
        val annotationType = settings.annotationTypes
        val className = when (apiOption) {
            BETA -> annotationType.beta
            EXPERIMENTAL -> annotationType.experimental
            INTERNAL -> annotationType.internal
            SPI -> annotationType.spi
        }
        @Suppress("UNCHECKED_CAST") /* Here we rely on the user's input.
         If the class is not an annotation type, `ClassCastException`
         would inform the developer on the fact. */
        val result = Class.forName(className) as Class<Annotation>
        return result
    }

    /**
     * Tells if the given source file set is suitable for this renderer.
     *
     * @see <a href="https://github.com/SpineEventEngine/ProtoData/issues/150">ProtoData issue</a>
     */
    protected abstract fun suitableFor(sources: SourceFileSet): Boolean

    final override fun render(sources: SourceFileSet) {
        if (suitableFor(sources)) {
            this.sources = sources
            doRender()
        }
    }

    private fun doRender() {
        val annotated: Set<T> = select(viewClass).all()
        annotated.forEach {
            annotate(it)
        }
    }

    /**
     * Annotates the code according to the given view state.
     */
    protected abstract fun annotate(view: T)
}
