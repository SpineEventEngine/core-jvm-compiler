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

import io.spine.core.External
import io.spine.protodata.ast.File
import io.spine.protodata.ast.event.FileEntered
import io.spine.protodata.java.javaMultipleFiles
import io.spine.server.entity.alter
import io.spine.server.event.NoReaction
import io.spine.server.event.React
import io.spine.server.procman.ProcessManager
import io.spine.server.procman.ProcessManagerRepository
import io.spine.server.route.EventRouting
import io.spine.tools.mc.annotation.ApiOption.Companion.findMatching

/**
 * A process manager which discovers the API annotation options set on the outer
 * class of a Protobuf file which has `java_multiple_files` set to `false`.
 *
 * @see OuterClassAnnotator
 */
internal class OuterClassAnnotationDiscovery:
    ProcessManager<File, OuterClassAnnotations, OuterClassAnnotations.Builder>() {

    @React
    fun on(@External e: FileEntered): NoReaction {
        // The check here is a safety net. We should get only events for
        // proto files with `java_multiple_files` set to `true`. See `Repository.setEventRouting`.
        if (!e.header.javaMultipleFiles()) {
            alter {
                file = e.file
                header = e.header
                e.header.optionList.mapNotNull {
                    findMatching(it)
                }.forEach {
                    // We care here only about the message options because service options
                    // are handled by `ServiceAnnotationsView`, and gRPC services are top
                    // level classes anyway. Using the message option would trigger
                    // the annotation that would have been added to a message class if it
                    // were a top level.
                    addOption(it.messageOption)
                }
            }
        }
        return noReaction()
    }

    class Repository :
        ProcessManagerRepository<File, OuterClassAnnotationDiscovery, OuterClassAnnotations>() {

        override fun setupEventRouting(routing: EventRouting<File>) {
            super.setupEventRouting(routing)
            routing.route<FileEntered> { e, _ ->
                // If the proto file assumes multiple files, there's no need to
                // look for outer class options.
                if (e.header.javaMultipleFiles()) {
                    emptySet<File>()
                } else {
                    setOf(e.file)
                }
            }
        }
    }
}
