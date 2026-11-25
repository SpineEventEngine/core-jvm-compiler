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

package io.spine.tools.core.jvm.entity

import io.spine.annotation.VisibleForTesting
import io.spine.tools.compiler.plugin.Plugin

/**
 * A Spine Compiler plugin responsible for handling code generation aspects related to
 * entity state declarations.
 *
 * @see EntityPluginComponent
 */
public class EntityPlugin : Plugin(
    reactions = setOf(
        EntityDiscovery(),
        EntityStateIdReaction()
    ),
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
