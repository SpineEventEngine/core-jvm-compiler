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

package io.spine.tools.mc.java.mgroup

import io.spine.protodata.java.file.hasJavaRoot
import io.spine.protodata.java.render.JavaRenderer
import io.spine.protodata.java.render.RenderActions
import io.spine.protodata.render.SourceFile
import io.spine.protodata.render.SourceFileSet
import io.spine.tools.code.Java
import io.spine.tools.mc.java.settings.GroupSettings
import io.spine.tools.psi.java.execute

/**
 * Renders code for message types gathered in [GroupedMessage].
 */
internal class GroupedMessageRenderer : JavaRenderer(), MessageGroupPluginComponent {

    private val settings: GroupSettings by lazy {
        loadSettings(GroupSettings::class.java)
    }

    private val enabledBySettings: Boolean
        get() = settings != GroupSettings.getDefaultInstance()

    override fun render(sources: SourceFileSet) {
        val relevant = sources.hasJavaRoot && enabledBySettings
        if (!relevant) {
            return
        }
        val types = findTypes()
        types.forEach {
            val sourceFile = sources.javaFileOf(it.type)
            execute {
                it.doRender(sourceFile)
            }
        }
    }

    private fun GroupedMessage.doRender(sourceFile: SourceFile<Java>) {
        groupList.forEach {
            RenderActions(type, sourceFile, it.actions, context).apply()
        }
    }

    private fun findTypes(): Set<GroupedMessage> {
        val found = select(GroupedMessage::class.java).all()
        return found
    }
}
