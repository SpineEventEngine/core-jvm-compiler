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

package io.spine.tools.mc.java.comparable

import com.google.protobuf.Message
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import io.kotest.matchers.nulls.shouldBeNull
import io.spine.logging.testing.tapConsole
import io.spine.protobuf.defaultInstance
import io.spine.protodata.Compilation
import io.spine.protodata.testing.acceptingOnly
import io.spine.testing.logging.mute.withLoggingMutedIn
import io.spine.tools.mc.java.comparable.AddComparatorSpec.Companion.generatedCodeOf
import io.spine.tools.mc.java.comparable.action.AddComparator
import java.nio.file.Path
import org.junit.jupiter.api.assertThrows

/**
 * Runs the [block] with logging muted in the `io.spine.tools.mc.java.comparable` package.
 */
internal fun muteLogging(block: () -> Unit) {
    withLoggingMutedIn(listOf(AddComparator::class.java.packageName), block)
}

/**
 * Asserts that the given message doesn't have a comparator.
 */
internal inline fun <reified T : Message> assertNoComparator() {
    val psiClass = generatedCodeOf(T::class.simpleName!!)
    val field = psiClass.findComparatorField()
    field.shouldBeNull()
}

internal fun PsiClass.findComparatorField(): PsiField? =
    fields.firstOrNull { it.name == "comparator" }

/**
 * Compiles only the given message expecting the [Compilation.Error] to be thrown.
 *
 * If the error is not thrown, the test calling this fixture fails.
 *
 * @param M The type of the message to compile.
 * @param projectDir The working directory for the test project.
 * @return compilation error thrown during the compilation.
 */
inline fun <reified M: Message> assertCompilationFails(projectDir: Path): Compilation.Error {
    val message = M::class.java.defaultInstance.descriptorForType
    val setup = ComparablePluginTestSetup(AddComparator::class)
    val error = assertThrows<Compilation.Error> {
        tapConsole {
            setup.runPipeline(projectDir, acceptingOnly(message))
        }
    }
    return error
}
