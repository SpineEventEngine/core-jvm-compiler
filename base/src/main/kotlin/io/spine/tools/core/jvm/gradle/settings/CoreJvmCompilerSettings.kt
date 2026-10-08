/*
 * Copyright 2026 CodeMatters, Lda.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

@file:Suppress("TooManyFunctions")

package io.spine.tools.core.jvm.gradle.settings

import io.spine.annotation.Internal
import io.spine.base.MessageFile
import io.spine.tools.compiler.ast.FilePattern
import io.spine.tools.compiler.ast.FilePatternFactory
import io.spine.tools.core.jvm.gradle.settings.SignalSettings.Companion.DEFAULT_COMMAND_ACTIONS
import io.spine.tools.core.jvm.gradle.settings.SignalSettings.Companion.DEFAULT_EVENT_ACTIONS
import io.spine.tools.core.jvm.gradle.settings.SignalSettings.Companion.DEFAULT_REJECTION_ACTIONS
import io.spine.tools.core.jvm.settings.Combined
import io.spine.tools.core.jvm.settings.MessageGroup
import io.spine.tools.core.jvm.settings.combined
import io.spine.tools.core.jvm.settings.groupSettings
import io.spine.tools.core.jvm.settings.pattern
import io.spine.tools.core.jvm.settings.signalSettings
import io.spine.tools.core.jvm.settings.typePattern
import io.spine.tools.proto.code.protoTypeName
import org.gradle.api.Action
import org.gradle.api.Project

/**
 * A part of [CoreJvmOptions][io.spine.tools.core.jvm.gradle.CoreJvmOptions] responsible
 * for code generation settings.
 */
public class CoreJvmCompilerSettings @Internal public constructor(private val project: Project) :
    Settings<Combined>(project) {

    /**
     * Settings for the generated command code.
     */
    public val commands: SignalSettings = SignalSettings(
        project,
        MessageFile.COMMANDS.suffix(),
        DEFAULT_COMMAND_ACTIONS
    )

    /**
     * Settings for the generated event code.
     */
    public val events: SignalSettings = SignalSettings(
        project,
        MessageFile.EVENTS.suffix(),
        DEFAULT_EVENT_ACTIONS
    )

    /**
     * Settings for the generated rejection code.
     */
    public val rejections: SignalSettings = SignalSettings(
        project,
        MessageFile.REJECTIONS.suffix(),
        DEFAULT_REJECTION_ACTIONS
    )

    /**
     * Settings for the generated entities code.
     */
    public val entities: EntitySettings = EntitySettings(project)

    /**
     * Settings for the generated code of [io.spine.base.UuidValue] types.
     */
    public val uuids: UuidSettings = UuidSettings(project)

    /**
     * Settings for the generated code of comparable messages.
     */
    public val comparables: ComparableSettings = ComparableSettings(project)

    /**
     * Settings for the generated code of grouped messages.
     */
    public val messageGroups: MutableSet<MessageGroup> = mutableSetOf()

    /**
     * Obtains an instance of [FilePatternFactory] that creates file patterns.
     *
     * @see forMessages
     */
    public fun by(): FilePatternFactory = FilePatternFactory

    /**
     * Configures code generation for command messages.
     */
    public fun forCommands(action: Action<SignalSettings>) {
        action.execute(commands)
    }

    /**
     * Configures code generation for event messages.
     *
     * Settings applied to events do not automatically apply to rejections as well.
     */
    public fun forEvents(action: Action<SignalSettings>) {
        action.execute(events)
    }

    /**
     * Configures code generation for rejection messages.
     *
     * Settings applied to events do not automatically apply to rejections as well.
     */
    public fun forRejections(action: Action<SignalSettings>) {
        action.execute(rejections)
    }

    /**
     * Configures code generation for entity state messages.
     */
    public fun forEntities(action: Action<EntitySettings>) {
        action.execute(entities)
    }

    /**
     * Configures code generation for a group of messages.
     *
     * The group is defined by a file-based selector.
     *
     * @see by
     */
    public fun forMessages(filePattern: FilePattern, action: Action<MessageGroupSettings>) {
        val pattern = pattern {
            file = filePattern
        }
        val mgs = MessageGroupSettings(project, pattern)
        action.execute(mgs)
        messageGroups.add(mgs.toProto())
    }

    /**
     * Configures code generation for a particular message.
     */
    public fun forMessage(protoTypeName: String, action: Action<MessageGroupSettings>) {
        val pattern = pattern {
            type = typePattern {
                expectedType = protoTypeName {
                    value = protoTypeName
                }
            }
        }
        val mgs = MessageGroupSettings(project, pattern)
        action.execute(mgs)
        messageGroups.add(mgs.toProto())
    }

    /**
     * Configures code generation for UUID messages.
     */
    public fun forUuids(action: Action<UuidSettings>) {
        action.execute(uuids)
    }

    /**
     * Configures code generation for comparable messages.
     */
    public fun forComparables(action: Action<ComparableSettings>) {
        action.execute(comparables)
    }

    override fun toProto(): Combined {
        val self = this@CoreJvmCompilerSettings
        val ss = signalSettings {
            commands = self.commands.toProto()
            events = self.events.toProto()
            rejections = self.rejections.toProto()
        }
        val gs = groupSettings {
            group.addAll(messageGroups)
        }
        return combined {
            signalSettings = ss
            groupSettings = gs
            entities = self.entities.toProto()
            uuids = self.uuids.toProto()
            comparables = self.comparables.toProto()
        }
    }
}
