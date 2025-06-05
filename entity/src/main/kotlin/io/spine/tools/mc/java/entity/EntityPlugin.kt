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

package io.spine.tools.mc.java.entity

import com.google.common.annotations.VisibleForTesting
import io.spine.protodata.plugin.Plugin

/**
 * A ProtoData plugin responsible for handling code generation aspects related to
 * entity state declarations.
 *
 * @see EntityPluginComponent
 */
public class EntityPlugin : Plugin(
    policies = setOf(EntityDiscovery()),
    views = setOf(DiscoveredEntitiesView::class.java),
    renderers = listOf(EntityStateRenderer())
) {
    public companion object {

        /**
         * The ID for obtaining settings of the plugin.
         */
        public val SETTINGS_ID: String = EntityPlugin::class.java.canonicalName

        /**
         * The name of the `Column` class generated under an entity state.
         */
        @VisibleForTesting
        public const val COLUMN_CLASS_NAME: String = "Column"

        /**
         * The name of the `Query` class generated under an entity state.
         */
        @VisibleForTesting
        public const val QUERY_CLASS_NAME: String = "Query"

        /**
         * The name of the `QueryBuilder` class generated under an entity state.
         */
        @VisibleForTesting
        public const val QUERY_BUILDER_CLASS_NAME: String = "QueryBuilder"

        /**
         * The name of the `query()` method generated for an entity state.
         */
        @VisibleForTesting
        public const val QUERY_METHOD_NAME: String = "query"

        /**
         * The name of the `definitions()` method of a `Column` class for
         * obtaining all the columns of an entity state.
         */
        @VisibleForTesting
        public const val DEFINITIONS_METHOD_NAME: String = "definitions"

        /**
         * The name of the `thisRef()` method of a `QueryBuilder` class.
         */
        @VisibleForTesting
        public const val THIS_REF_METHOD_NAME: String = "thisRef"

        /**
         * The name of the `build()` method of a `QueryBuilder` class.
         */
        public const val BUILD_METHOD_NAME: String = "build"
    }
}
