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

import io.spine.base.EntityState
import io.spine.protodata.render.SourceFileSet

/**
 * An abstract base for annotation renderers that annotate Protobuf generated code
 * in accordance to API level options found in the source proto files.
 *
 * @param T the type of the view state which contains information about annotated types.
 */
internal abstract class ProtoAnnotator<T>(
    private val viewClass: Class<T>
) : Annotator() where T : EntityState<*> {

    /**
     * The sources passed for processing to this renderer.
     */
    protected lateinit var sources: SourceFileSet

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
