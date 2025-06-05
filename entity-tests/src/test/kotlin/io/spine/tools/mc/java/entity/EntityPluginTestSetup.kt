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

import com.intellij.psi.PsiClass
import io.spine.tools.mc.java.PluginTestSetup
import io.spine.tools.mc.java.gradle.settings.EntitySettings
import io.spine.tools.mc.java.settings.Entities
import io.spine.tools.psi.java.method
import java.nio.file.Path
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
        const val DEPARTMENT_JAVA = "io/spine/tools/mc/java/entity/given/Department.java"
    }

    override fun createSettings(projectDir: Path): Entities {
        val project = createProject(projectDir)
        val entityConfig = EntitySettings(project)
        return entityConfig.toProto()
    }
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
