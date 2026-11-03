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

package io.spine.tools.core.jvm.signal.rejection

import io.kotest.matchers.shouldBe
import io.spine.tools.compiler.jvm.ClassName
import io.spine.tools.core.jvm.signal.rejection.ClassReferences.Companion.javaFile
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("`ClassReferences` should")
internal class ClassReferencesSpec {

    private val generated = ClassName(PACKAGE, "Thrown")

    /**
     * Produces the file with the [generated] class, which has the `Builder` nested class.
     */
    private fun file(classCode: ClassReferences.() -> String): String =
        javaFile(generated, nested = setOf("Builder"), classCode)

    @Test
    fun `refer to the generated class and its nested classes by simple names`() {
        file {
            "${name(generated)} ${name(generated.nested("Builder"))}"
        } shouldBe code("Thrown Builder")
    }

    @Test
    fun `refer to a class of the same package relative to the package`() {
        file {
            name(ClassName(PACKAGE, "Rejections", "Thrown"))
        } shouldBe code("Rejections.Thrown")
    }

    @Test
    fun `import the top-level class of a class from another package`() {
        file {
            name(ClassName("io.spine.other", "Outer", "Inner"))
        } shouldBe code("Outer.Inner", imports = listOf("io.spine.other.Outer"))
    }

    @Test
    fun `refer to 'String' by its simple name without importing it`() {
        file {
            name(ClassName(String::class))
        } shouldBe code("String")
    }

    @Test
    fun `sort imports by canonical names`() {
        file {
            listOf(
                ClassName("io.spine.zeta", "Zeta"),
                ClassName("io.spine.alpha", "Beta"),
                ClassName("io.spine.alpha", "Alpha")
            ).joinToString(" ") { name(it) }
        } shouldBe code(
            "Zeta Beta Alpha",
            imports = listOf("io.spine.alpha.Alpha", "io.spine.alpha.Beta", "io.spine.zeta.Zeta")
        )
    }

    @Test
    fun `not import a class from the default package`() {
        file {
            name(ClassName("", "Default"))
        } shouldBe code("Default")
    }

    @Test
    fun `omit the package statement for a class in the default package`() {
        val inDefaultPackage = ClassName("", "Thrown")
        javaFile(inDefaultPackage, nested = emptySet()) {
            name(ClassName("io.spine.other", "Item"))
        } shouldBe "import io.spine.other.Item;\n\nItem\n"
    }

    @Nested inner class
    `refer to a class by its canonical name if its simple name is taken by` {

        @Test
        fun `a nested class`() {
            file {
                name(ClassName("io.spine.other", "Builder"))
            } shouldBe code("io.spine.other.Builder")
        }

        @Test
        fun `the generated class`() {
            file {
                name(ClassName("io.spine.other", "Thrown"))
            } shouldBe code("io.spine.other.Thrown")
        }

        @Test
        fun `a class of the same package`() {
            file {
                val samePackage = name(ClassName(PACKAGE, "Rejections", "Thrown"))
                val other = name(ClassName("io.spine.other", "Rejections"))
                "$samePackage $other"
            } shouldBe code("Rejections.Thrown io.spine.other.Rejections")
        }

        @Test
        fun `a class imported before`() {
            file {
                val first = name(ClassName("io.spine.first", "Item"))
                val second = name(ClassName("io.spine.second", "Item"))
                "$first $second"
            } shouldBe code("Item io.spine.second.Item", imports = listOf("io.spine.first.Item"))
        }

        @Test
        fun `the 'String' class referenced before`() {
            file {
                val string = name(ClassName(String::class))
                val other = name(ClassName("io.spine.other", "String"))
                "$string $other"
            } shouldBe code("String io.spine.other.String")
        }
    }

    @Nested inner class
    `annotate a class` {

        private val validated = ClassName("io.spine.validation", "Validated")

        @Test
        fun `placing the annotation before the simple name of a nested class`() {
            file {
                annotated(ClassName(PACKAGE, "Rejections", "Thrown"), validated)
            } shouldBe code(
                "Rejections.@Validated Thrown",
                imports = listOf("io.spine.validation.Validated")
            )
        }

        @Test
        fun `placing the annotation before the simple name of a class with a canonical name`() {
            file {
                annotated(ClassName("io.spine.other", "Thrown"), validated)
            } shouldBe code(
                "io.spine.other.@Validated Thrown",
                imports = listOf("io.spine.validation.Validated")
            )
        }

        @Test
        fun `placing the annotation before a top-level class`() {
            file {
                annotated(ClassName("io.spine.other", "Item"), validated)
            } shouldBe code(
                "@Validated Item",
                imports = listOf("io.spine.other.Item", "io.spine.validation.Validated")
            )
        }
    }
}

private const val PACKAGE = "io.spine.given"

/**
 * Composes the expected code of a file of the [PACKAGE] with the given imports.
 */
private fun code(classCode: String, imports: List<String> = emptyList()): String = buildString {
    append("package $PACKAGE;\n\n")
    imports.forEach { append("import $it;\n") }
    if (imports.isNotEmpty()) {
        append('\n')
    }
    append(classCode).append('\n')
}
