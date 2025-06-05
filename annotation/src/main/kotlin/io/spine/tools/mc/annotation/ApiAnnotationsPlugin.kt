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

package io.spine.tools.mc.annotation

import io.spine.protodata.plugin.Plugin
import io.spine.server.BoundedContextBuilder
import io.spine.tools.mc.java.annotation.ClassPatternAnnotator
import io.spine.tools.mc.java.annotation.EnumAnnotator
import io.spine.tools.mc.java.annotation.FieldAnnotator
import io.spine.tools.mc.java.annotation.MessageAnnotator
import io.spine.tools.mc.java.annotation.MethodPatternAnnotator
import io.spine.tools.mc.java.annotation.OuterClassAnnotationDiscovery
import io.spine.tools.mc.java.annotation.OuterClassAnnotator
import io.spine.tools.mc.java.annotation.ServiceAnnotationRenderer

/**
 * A ProtoData plugin which annotates Java code with API level annotations that match
 * the API level options defined in Protobuf files.
 *
 * Spine SDK defines two ways for defining API level stability for the Java code:
 *
 * 1. **API level annotations for Java classes** such as
 * [Beta][io.spine.annotation.Beta] or [Internal][io.spine.annotation.Internal].
 * These annotations are used for the handcrafted code.
 * Please see `io.spine.annotation` package for details.
 *
 * 2. **API level options for Protobuf types.** These options are used
 * defined in the `options.proto` file, which is likely to be imported by most of
 * the Protobuf files of a Spine-based application.
 * These options result in annotations in the generated code.
 *
 * The file `options.proto` defines options for files, message types, fields, and services which
 * serve the same purpose as the API level annotations for Java classes.
 *
 * This plugin annotates the Java code produced by the Protobuf compiler, taking into account
 * options discovered in corresponding definitions.
 *
 * @see ApiOption
 */
public class ApiAnnotationsPlugin : Plugin(
    renderers = listOf(
        MessageAnnotator(),
        EnumAnnotator(),
        ServiceAnnotationRenderer(),
        OuterClassAnnotator(),
        FieldAnnotator(),
        ClassPatternAnnotator(),
        MethodPatternAnnotator()
    ),
    views = setOf(
        EnumAnnotationsView::class.java,
        MessageAnnotationsView::class.java,
        MessageFieldAnnotationsView::class.java,
        ServiceAnnotationsView::class.java,
    )
) {
    override fun extend(context: BoundedContextBuilder) {
        context.add(FileOptionsProcess::class.java)
        context.add(OuterClassAnnotationDiscovery.Repository())
    }

    public companion object {

        /**
         * The ID for obtaining settings of the plugin.
         */
        public val SETTINGS_ID: String = ApiAnnotationsPlugin::class.java.canonicalName
    }
}
