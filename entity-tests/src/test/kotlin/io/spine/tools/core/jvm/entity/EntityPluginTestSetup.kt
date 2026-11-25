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

import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.Descriptors.GenericDescriptor
import com.intellij.psi.PsiClass
import io.kotest.matchers.string.shouldContain
import io.spine.tools.compiler.Compilation
import io.spine.tools.core.jvm.PluginTestSetup
import io.spine.tools.core.jvm.entity.given.BrokenIdEntity
import io.spine.tools.core.jvm.entity.given.Campus
import io.spine.tools.core.jvm.entity.given.DefaultSingleFileProto.Garage
import io.spine.tools.core.jvm.entity.given.Office
import io.spine.tools.core.jvm.entity.given.RepeatedIdEntity
import io.spine.tools.core.jvm.entity.given.SingleFileProto.Depot
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
     * [Office] and [Campus] have nested entity states, rejected at compile time and tested in
     * `NestedEntityStateErrorSpec` and `DeeplyNestedEntityStateErrorSpec`.
     * The outer messages are listed because the descriptor filter matches only top-level types.
     *
     * [Depot] and [Garage] are declared in files which do not set `java_multiple_files` to `true`:
     * the option is `false` and absent, respectively. They are rejected at compile time and
     * tested in `SingleFileEntityStateErrorSpec` and `DefaultSingleFileEntityStateErrorSpec`.
     */
    override fun defaultExclusions(): List<GenericDescriptor> =
        listOf(
            BrokenIdEntity.getDescriptor(),
            RepeatedIdEntity.getDescriptor(),
            Office.getDescriptor(),
            Campus.getDescriptor(),
            Depot.getDescriptor(),
            Garage.getDescriptor(),
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

/**
 * Asserts that this error tells that the given entity [state] is nested in another message.
 */
internal fun Compilation.Error.shouldReportNested(state: Descriptor) {
    shouldReport(state)
    message.let {
        it shouldContain "nested in `${state.containingType.fullName}`"
        it shouldContain "must be a top-level message"
        it shouldContain "move `${state.name}` to the top level"
    }
}

/**
 * Asserts that this error tells that the given entity [state] is declared in a file which does not
 * set the `java_multiple_files` option to `true`.
 */
internal fun Compilation.Error.shouldReportSingleFile(state: Descriptor) {
    shouldReport(state)
    message.let {
        it shouldContain "does not set the `java_multiple_files` option to `true`"
        it shouldContain "Please set `java_multiple_files` to `true`"
    }
}

/**
 * Asserts that this error is about the `(entity)` option of the given [state], and that it refers
 * to the place in the proto file at which the [state] is declared.
 */
private fun Compilation.Error.shouldReport(state: Descriptor) {
    message.let {
        it shouldContain "${state.file.name}:${state.findDeclaration()}: "
        it shouldContain "The `(entity)` option cannot be applied"
        it shouldContain "the message `${state.fullName}`"
    }
}

/**
 * Finds the place at which this message is declared in its proto file.
 *
 * We search in the text of the file because the descriptors available from
 * the generated code do not have information about the source code.
 * The simple name of the message must be unique in the file.
 *
 * @return the one-based line and column in the form of `line:column`.
 */
private fun Descriptor.findDeclaration(): String {
    val classLoader = EntityPluginTestSetup::class.java.classLoader
    val protoFile = checkNotNull(classLoader.getResource(file.name)) {
        "Unable to find the resource `${file.name}`."
    }
    val declaration = "message $name {"
    val (index, line) = protoFile.readText().lines()
        .withIndex()
        .single { (_, line) -> declaration in line }
    return "${index + 1}:${line.indexOf(declaration) + 1}"
}
