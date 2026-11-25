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

import com.google.protobuf.Descriptors.GenericDescriptor
import com.intellij.psi.PsiClass
import io.spine.tools.core.jvm.PluginTestSetup
import io.spine.tools.core.jvm.entity.given.BrokenIdEntity
import io.spine.tools.core.jvm.entity.given.RepeatedIdEntity
import io.spine.tools.core.jvm.entity.given.reserved.Board
import io.spine.tools.core.jvm.entity.given.reserved.QueryBuilder
import io.spine.tools.core.jvm.entity.given.reserved.Report
import io.spine.tools.core.jvm.gradle.settings.EntitySettings
import io.spine.tools.core.jvm.settings.Entities
import io.spine.tools.psi.java.method
import java.nio.file.Path
import kotlin.io.path.Path
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

/**
 * Abstract base for suites testing [EntityPlugin] parts.
 */
@Suppress("UtilityClassWithPublicConstructor")
abstract class EntityPluginTestSetup : PluginTestSetup<Entities>(
    EntityPlugin(),
    EntityPlugin.SETTINGS_ID
) {

    companion object {

        /**
         * The path to the Java file generated for the `Department` entity state.
         */
        val departmentJava = "Department".java

        /**
         * Obtains a path to a Java source file for the proto type with this name.
         */
        val String.java: Path
            get() = Path("io/spine/tools/core/jvm/entity/given/$this.java")
    }

    override fun createSettings(projectDir: Path): Entities {
        val project = createProject(projectDir)
        val entityConfig = EntitySettings(project)
        return entityConfig.toProto()
    }

    /**
     * Excludes the compile-fail fixtures from regular pipeline runs.
     *
     * [BrokenIdEntity] has an implicitly-required `Empty` ID field, rejected at
     * compile time and tested in `IdFieldErrorSpec`.
     *
     * [RepeatedIdEntity] has a `repeated` ID field, rejected at compile time and tested in
     * `UnsupportedIdTypeErrorSpec`.
     *
     * [Board], [Report], and [QueryBuilder] break the rule on the reserved names, and are
     * tested in `ReservedMessageNameErrorSpec`, `ReservedEnumNameErrorSpec`, and
     * `ReservedStateNameErrorSpec`.
     */
    override fun defaultExclusions(): List<GenericDescriptor> =
        listOf(
            BrokenIdEntity.getDescriptor(),
            RepeatedIdEntity.getDescriptor(),
            Board.getDescriptor(),
            Report.getDescriptor(),
            QueryBuilder.getDescriptor(),
        )
}

/**
 * Asserts that this class has a method with the given name.
 */
internal fun PsiClass.assertHasMethod(name: String) {
    assertDoesNotThrow {
        method(name)
    }
}

/**
 * Asserts that this class does not have a method with the given name.
 */
internal fun PsiClass.assertDoesNotHaveMethod(name: String) {
    assertThrows<IllegalStateException> {
        method(name)
    }
}

/**
 * Obtains an inner class with the given name.
 *
 * The class is immediately inner to this one.
 * Base classes for this one are not searched.
 *
 * @throws IllegalStateException If the inner class was not found.
 * @see PsiClass.findInnerClassByName
 */
internal fun PsiClass.innerClass(name: String): PsiClass {
    val innerClass = findInnerClassByName(name, false)
    return innerClass ?: error(
        "The class `$qualifiedName` does not have an inner class `$name`."
    )
}
