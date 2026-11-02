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

package io.spine.tools.core.jvm.annotation.check

import com.intellij.psi.PsiClass
import io.kotest.matchers.optional.shouldBeEmpty
import io.kotest.matchers.optional.shouldBePresent
import io.spine.annotation.Internal
import io.spine.testing.UtilityClassTest
import io.spine.tools.core.jvm.annotation.check.Annotations.findAnnotation
import io.spine.tools.psi.java.Environment
import io.spine.tools.psi.java.Parser
import io.spine.tools.psi.java.topLevelClass
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`Annotations` should")
internal class AnnotationsSpec : UtilityClassTest<Annotations>(Annotations::class.java) {

    @Test
    fun `find an annotation written with its qualified name`() {
        val cls = parse("@io.spine.annotation.Internal class Foo {}")
        findAnnotation(cls, Internal::class.java).shouldBePresent()
    }

    @Test
    fun `find an annotation written with its simple name`() {
        val cls = parse("import io.spine.annotation.Internal; @Internal class Foo {}")
        findAnnotation(cls, Internal::class.java).shouldBePresent()
    }

    @Test
    fun `not find an annotation of another type`() {
        val cls = parse("@io.spine.annotation.SPI class Foo {}")
        findAnnotation(cls, Internal::class.java).shouldBeEmpty()
    }

    @Test
    fun `not find an annotation with the same simple name from another package`() {
        val cls = parse("@com.example.Internal class Foo {}")
        findAnnotation(cls, Internal::class.java).shouldBeEmpty()
    }
}

private fun parse(code: String): PsiClass =
    Parser(Environment.project).parse(code).topLevelClass
