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

package io.spine.tools.core.jvm.routing.tests

import io.kotest.matchers.collections.shouldContainAll
import io.spine.given.home.Cellar
import io.spine.given.home.Kitchen
import io.spine.server.route.setup.EventRoutingSetup
import java.util.ServiceLoader
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Generated routing setup classes of nested entities should")
internal class NestedEntityRoutingITest {

    /**
     * This test verifies that classes generated for entities with the same simple name, which are
     * nested in classes of the same package, are both discovered if the entities are compiled
     * separately, as the main code and the test code are.
     */
    @Test
    fun `be discovered in different source sets`() {
        val entityClasses = ServiceLoader.load(EventRoutingSetup::class.java)
            .map { it.entityClass() }

        entityClasses shouldContainAll listOf(
            Kitchen.Shelf::class.java,
            Cellar.Shelf::class.java
        )
    }
}
