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

package io.spine.tools.core.jvm.field

import io.spine.string.camelCase
import io.spine.string.joinBackticked
import io.spine.tools.compiler.Compilation
import io.spine.tools.compiler.ast.MessageType
import io.spine.tools.compiler.ast.TypeName
import io.spine.tools.compiler.ast.qualifiedName
import io.spine.tools.compiler.jvm.ClassName

/**
 * Names of the classes that [AddFieldClass] generates for the types of the fields of a message.
 *
 * A class has a _short_ name, unless it clashes with another one. The short name consists of
 * the names of the types enclosing the field type, if any, from the outermost inwards,
 * the simple name of the field type, and the [SUFFIX].
 * For example, `UserIdField` for `UserId`, and `BarnStallField` for `Barn.Stall`.
 *
 * All the classes are nested in the [fieldClass]. Therefore, their names must differ from
 * each other, and from the names of the classes around them. Names that clash are changed
 * as follows:
 *
 *  1. Among types from one package, the least nested type keeps the short name.
 *   The parts of the names of other types are separated with underscores. For example,
 *   `ShelfBoxField` for `ShelfBox`, and `Shelf_BoxField` for `Shelf.Box`.
 *   If several types are the least nested, all the names have underscores.
 *
 *  2. Among types from different packages, the types from the package of the [messageType]
 *   keep their names. The names of other types start with the last word of their package.
 *   For example, `AddressField` for `acme.home.Address`, and `WebAddressField` for
 *   `acme.web.Address`, if the message is declared in the `acme.home` package.
 *   If no type is from the package of the message, the names of all the types start with
 *   package words. More words are added to the names that still clash.
 *   The names with package words differ from other names ignoring case, if it is possible.
 *   Otherwise, class files with such names would overwrite each other in some file systems.
 *
 *  3. A name that is the same as the name of a class around it has an underscore before
 *   the [SUFFIX]. For example, `Crate_Field` for `Crate` in the message `CrateField`.
 *
 * If the names still clash, the compilation fails.
 *
 * @param messageType The type of the message for which the [fieldClass] is generated.
 * @param fieldClass The name of the class that encloses the classes with the calculated names.
 * @param fieldTypes The names of the types for which the classes are generated.
 */
internal class FieldClassNames(
    private val messageType: MessageType,
    private val fieldClass: ClassName,
    fieldTypes: Set<TypeName>
) {
    private val names: Map<TypeName, String>

    init {
        val candidates = fieldTypes
            .groupBy { it.shortName }
            .values
            .flatMap { candidatesFor(it) }
        names = candidates.resolve().associate { it.type to it.name }
        checkNoClashes()
    }

    /**
     * Obtains the simple name of the class generated for the given type.
     *
     * @throws IllegalStateException If the type was not passed when creating this instance.
     */
    fun of(type: TypeName): String = names[type]
        ?: error(
            "The type `${type.qualifiedName}` is not among the types of the fields of" +
                    " `${messageType.name.qualifiedName}`."
        )

    /**
     * Creates candidates for the types that have the same short name.
     */
    private fun candidatesFor(types: List<TypeName>): List<Candidate> {
        val byPackage = types.groupBy { it.packageName }
        return byPackage.flatMap { (packageName, neighbours) ->
            val keepsPackage =
                byPackage.size == 1 || packageName == messageType.name.packageName
            val leastNested = neighbours.leastNested()
            neighbours.map { type ->
                val keepsShortName = type == leastNested
                val candidate = Candidate(
                    type = type,
                    base = if (keepsShortName) type.shortName else type.underscoredName,
                    isFixed = keepsShortName && keepsPackage,
                    around = fieldClass.simpleNames
                )
                if (keepsPackage) candidate else candidate.lengthen()
            }
        }
    }

    private fun checkNoClashes() {
        val clashes = names.entries
            .groupBy({ it.value }, { it.key.qualifiedName })
            .filter { (name, types) -> types.size > 1 || name in fieldClass.simpleNames }
        if (clashes.isEmpty()) {
            return
        }
        Compilation.error(messageType.file, messageType.span) {
            val message = messageType.name.qualifiedName
            val details = clashes.toSortedMap().map { (name, types) -> clash(name, types) }
            "Unable to generate the `${fieldClass.simpleName}` class" +
                    " for the message type `$message`." +
                    details.joinToString(separator = "") +
                    " Please rename the clashing types."
        }
    }

    private fun clash(name: String, types: List<String>): String {
        val typeList = types.sorted().joinBackticked()
        return if (types.size > 1) {
            " Classes for the types $typeList would have the same name `$name`."
        } else {
            " The class for the type $typeList would have the name `$name`" +
                    " of a class it is nested in."
        }
    }

    companion object {

        /**
         * The suffix of the names of field classes.
         */
        const val SUFFIX = "Field"
    }
}

private const val UNDERSCORE = "_"

/**
 * A possible name of a field class.
 *
 * @property type The type for which the class is generated.
 * @property base The part of the name between the package words and the suffix.
 * @property isFixed If `true`, package words must not be added to the name.
 * @property around The simple names of the classes around the generated class.
 * @property wordCount The number of package words at the beginning of the name.
 */
private data class Candidate(
    val type: TypeName,
    val base: String,
    val isFixed: Boolean,
    val around: List<String>,
    val wordCount: Int = 0
) {
    private val packageWords: List<String> =
        type.packageName.split(".").filter { it.isNotEmpty() }.map { it.camelCase() }

    /**
     * The name of the class, which has an underscore before the suffix if the name without it is
     * the name of a class around.
     */
    val name: String = run {
        val stem = packageWords.takeLast(wordCount).joinToString(separator = "") + base
        val usual = stem + FieldClassNames.SUFFIX
        if (usual in around) stem + UNDERSCORE + FieldClassNames.SUFFIX else usual
    }

    /**
     * Adds one more package word to the name, if it is possible.
     */
    fun lengthen(): Candidate =
        if (isFixed || wordCount == packageWords.size) this else copy(wordCount = wordCount + 1)
}

/**
 * Adds package words to the names that clash until they differ ignoring case, and
 * are not the names of the classes around.
 *
 * If there are not enough words for a type, the case of its name is taken into account. Names of
 * other types are still compared ignoring case.
 *
 * @param caseSensitive The types for which the case of their names is taken into account.
 */
private tailrec fun List<Candidate>.resolve(
    caseSensitive: Set<TypeName> = emptySet()
): List<Candidate> {
    val lengthened = lengthenClashing(caseSensitive)
    val unresolved = lengthened.clashing(caseSensitive)
        .filter { !it.isFixed && it.type !in caseSensitive }
        .map { it.type }
    return if (unresolved.isEmpty()) {
        lengthened
    } else {
        resolve(caseSensitive + unresolved)
    }
}

/**
 * Adds package words to the names that clash until they differ, or until there are no words
 * to add.
 */
private tailrec fun List<Candidate>.lengthenClashing(
    caseSensitive: Set<TypeName>
): List<Candidate> {
    val clashing = clashing(caseSensitive)
    val lengthened = map { if (it in clashing) it.lengthen() else it }
    return if (lengthened == this) {
        this
    } else {
        lengthened.lengthenClashing(caseSensitive)
    }
}

/**
 * Obtains the candidates with the names that clash with the names of other candidates, or of
 * the classes around.
 */
private fun List<Candidate>.clashing(caseSensitive: Set<TypeName>): Set<Candidate> {
    val sameIgnoringCase = groupBy { it.name.lowercase() }.values.filter { it.size > 1 }
    val withOthers = sameIgnoringCase.flatMap { group ->
        group.filter { candidate ->
            candidate.type !in caseSensitive || group.count { it.name == candidate.name } > 1
        }
    }
    val withClassesAround = filter { it.name in it.around }
    return (withOthers + withClassesAround).toSet()
}

private val TypeName.path: List<String>
    get() = nestingTypeNameList + simpleName

private val TypeName.shortName: String
    get() = path.joinToString(separator = "")

private val TypeName.underscoredName: String
    get() = path.joinToString(separator = UNDERSCORE)

/**
 * Obtains the type that is nested less deeply than the others, or
 * `null` if there are several such types.
 */
private fun List<TypeName>.leastNested(): TypeName? {
    val depth = minOf { it.path.size }
    return singleOrNull { it.path.size == depth }
}
