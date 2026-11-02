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

import io.spine.tools.compiler.jvm.ClassName
import io.spine.tools.compiler.jvm.ClassName.Companion.CANONICAL_SEPARATOR

/**
 * Decides how the code of a generated top-level Java class refers to other classes,
 * and which classes the file of the generated class imports.
 *
 * The naming and import rules are those of JavaPoet, which previously generated
 * the rejection throwables, so that the generated code does not change:
 *
 *  1. The generated class and the classes nested in it are referred to by their simple names.
 *  2. A class from the package of the generated class is referred to by its name relative
 *     to the package, for example, `Outer.Nested`.
 *  3. The top-level class of any other class is imported, unless its simple name is already
 *     taken by a class covered by the rules above or by a class imported earlier. A class
 *     of an imported top-level class is referred to starting with the simple name of
 *     the imported class, for example, `Outer.Nested`.
 *  4. Otherwise, a class is referred to by its canonical name.
 *
 * Classes from the `java.lang` package are imported implicitly.
 *
 * The imports depend on the code of the whole class. Therefore, [javaFile] produces
 * the code of the class twice: first to collect the imports, and then to obtain the result.
 *
 * @param generated The name of the generated top-level class.
 * @param nested The simple names of the classes nested in the generated class.
 * @param imports The imported classes by their simple names.
 */
internal class ClassReferences private constructor(
    private val generated: ClassName,
    private val nested: Set<String>,
    private val imports: Map<String, ClassName>
) {

    /**
     * The top-level classes to import by their simple names, in the order of the first reference.
     */
    private val importable = mutableMapOf<String, ClassName>()

    /**
     * The simple names of the top-level classes from the package of the generated class.
     */
    private val samePackage = mutableSetOf<String>()

    /**
     * Obtains the code referring to the given class.
     */
    fun name(cls: ClassName): String {
        // Find the shortest suffix of the name that refers to the class.
        val resolved = generateSequence(cls) { it.outer() }
            .firstOrNull { resolve(it.simpleName) == it }
        if (resolved != null) {
            val suffix = cls.simpleNames.drop(resolved.simpleNames.size - 1)
            return suffix.joinToString(CANONICAL_SEPARATOR)
        }
        val topLevel = cls.simpleNames.first()
        return when {
            // The simple name of the top-level class is taken by another class.
            resolve(topLevel) != null -> cls.canonical
            cls.packageName == generated.packageName -> {
                samePackage.add(topLevel)
                cls.simpleNames.joinToString(CANONICAL_SEPARATOR)
            }
            else -> {
                if (cls.packageName.isNotEmpty()) {
                    importable.putIfAbsent(topLevel, ClassName(cls.packageName, topLevel))
                }
                cls.canonical
            }
        }
    }

    /**
     * Obtains the code referring to the given class with the given type annotation.
     *
     * The annotation precedes the simple name of the class, for example,
     * `Outer.@Validated Nested`.
     */
    fun annotated(cls: ClassName, annotation: ClassName): String {
        val name = name(cls)
        val annotationName = name(annotation)
        val simpleNameStart = name.lastIndexOf(CANONICAL_SEPARATOR) + 1
        return name.substring(0, simpleNameStart) +
                "@$annotationName " +
                name.substring(simpleNameStart)
    }

    private fun resolve(simpleName: String): ClassName? = when (simpleName) {
        in nested -> generated.nested(simpleName)
        generated.simpleName -> generated
        else -> imports[simpleName]
    }

    private fun suggestedImports(): Map<String, ClassName> =
        importable.filterKeys { it !in samePackage }

    companion object {

        private const val JAVA_LANG = "java.lang"

        /**
         * Produces the code of the Java file declaring the given top-level class.
         *
         * @param cls The name of the declared class.
         * @param nested The simple names of the classes nested in the declared class.
         * @param classCode The function producing the code of the class declaration, which
         *   refers to other classes via the receiver. The function is called twice: first to
         *   collect the imports, and then to obtain the code. Therefore, it must refer to
         *   the same classes in the same order on both calls, and must have no side effects.
         */
        fun javaFile(
            cls: ClassName,
            nested: Set<String>,
            classCode: ClassReferences.() -> String
        ): String {
            val collector = ClassReferences(generated = cls, nested = nested, imports = emptyMap())
            collector.classCode()
            val imports = collector.suggestedImports()
            val code = ClassReferences(generated = cls, nested = nested, imports = imports)
                .classCode()
            val importLines = imports.values
                .filter { it.packageName != JAVA_LANG }
                .sortedBy { it.canonical }
                .map { "import ${it.canonical};" }
            return buildString {
                if (cls.packageName.isNotEmpty()) {
                    append("package ${cls.packageName};\n\n")
                }
                importLines.forEach { append(it).append('\n') }
                if (importLines.isNotEmpty()) {
                    append('\n')
                }
                append(code).append('\n')
            }
        }
    }
}
