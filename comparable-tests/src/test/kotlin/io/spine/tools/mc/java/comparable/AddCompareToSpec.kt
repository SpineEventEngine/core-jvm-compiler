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

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldContain
import io.spine.tools.mc.java.comparable.action.AddCompareTo
import io.spine.tools.mc.java.comparable.given.Account
import io.spine.tools.mc.java.comparable.given.AccountId
import io.spine.tools.psi.java.method
import java.nio.file.Path
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("`AddCompareTo` should")
internal class AddCompareToSpec {

    companion object : ComparablePluginTestSetup(AddCompareTo::class) {

        @BeforeAll
        @JvmStatic
        fun setup(@TempDir projectDir: Path) = runPipeline(projectDir)
    }

    @Test
    fun `add the method to a message with the option`() {
        val message = Account.getDescriptor().name
        val psiClass = generatedCodeOf(message)
        val method = psiClass.method("compareTo")
        val expected = """
        |    @Override
        |    public int compareTo($message other) {
        |        return comparator.compare(this, other);
        |    }
        """.trimMargin()
        method.shouldNotBeNull()
        method.text shouldContain expected
    }

    @Test
    fun `ignore messages without the option`() {
        val message = AccountId.getDescriptor().name
        val cls = generatedCodeOf(message)
        val method = cls.findMethodsByName("compareTo")
        method.shouldBeEmpty()
    }
}
