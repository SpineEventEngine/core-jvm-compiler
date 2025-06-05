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

@file:Suppress("UNUSED_PARAMETER")

package io.spine.tools.mc.annotation

import io.spine.core.External
import io.spine.protodata.ast.File
import io.spine.protodata.ast.Option
import io.spine.protodata.ast.ProtobufSourceFile
import io.spine.protodata.ast.event.FileExited
import io.spine.protodata.ast.event.FileOptionDiscovered
import io.spine.server.entity.alter
import io.spine.server.entity.state
import io.spine.server.event.NoReaction
import io.spine.server.event.React
import io.spine.server.procman.ProcessManager
import io.spine.server.query.select
import io.spine.tools.mc.annotation.ApiOption.Companion.findMatching
import io.spine.tools.mc.annotation.event.FileOptionMatched
import io.spine.tools.mc.annotation.event.fileOptionMatched

/**
 * Transforms options defined in a Protobuf file into [FileOptionMatched] events that match
 * a file-level option with an option for a corresponding Protobuf type such as
 * a message or a service defined in the file.
 *
 * @see io.spine.tools.mc.annotation.event.FileOptionMatched
 */
internal class FileOptionsProcess : ProcessManager<File, FileOptions, FileOptions.Builder>() {

    @React
    fun on(@External e: FileOptionDiscovered): NoReaction {
        val isApiLevelOption = findMatching(e.option) != null
        val optionValue = e.option.value
        if (isApiLevelOption && optionValue.isTrue()) {
            alter {
                file = e.file
                addOption(e.option)
            }
        }
        return noReaction()
    }

    @React
    fun on(@External e: FileExited): Iterable<FileOptionMatched> {
        if (state.optionList.isEmpty()) {
            // There are no API-related options in this file.
            return emptyList()
        }
        return emitEvents()
    }

    private fun emitEvents(): Iterable<FileOptionMatched> {
        val currentFile = state().file
        val protoSrc = select<ProtobufSourceFile>().findById(currentFile)
        check(protoSrc != null) {
            "Unable to load type data of the Protobuf source file with path `$currentFile`."
        }
        val events = mutableListOf<FileOptionMatched>()

        state.optionList.forEach { fileOption ->
            val apiOption = findMatching(fileOption)
            apiOption?.let {
                protoSrc.addEvents(events, fileOption, it)
            }
        }
        return events
    }
}

private fun ProtobufSourceFile.addEvents(
    events: MutableList<FileOptionMatched>,
    fileOption: Option,
    apiOption: ApiOption
) {
    addMessageEvents(events, fileOption, apiOption.messageOption)
    apiOption.serviceOption?.let { serviceOption ->
        addServiceEvents(events, fileOption, serviceOption)
    }
}

private fun ProtobufSourceFile.addMessageEvents(
    events: MutableList<FileOptionMatched>,
    fileOption: Option,
    typeOption: Option
) {
    val thisSource = this
    typeMap.values.forEach { message ->
        events.add(fileOptionMatched {
            file = thisSource.file
            this.fileOption = fileOption
            this@fileOptionMatched.messageType = message.name
            assumed = typeOption
        })
    }
}

private fun ProtobufSourceFile.addServiceEvents(
    events: MutableList<FileOptionMatched>,
    fileOption: Option,
    serviceOption: Option
) {
    val thisSource = this
    serviceMap.values.forEach {
        events.add(fileOptionMatched {
            file = thisSource.file
            this.fileOption = fileOption
            service = it.name
            assumed = serviceOption
        })
    }
}
