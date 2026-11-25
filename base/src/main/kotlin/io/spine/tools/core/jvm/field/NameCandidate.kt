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
import io.spine.tools.compiler.ast.TypeName
import io.spine.tools.compiler.ast.qualifiedName
import io.spine.tools.core.jvm.field.FieldClassNames.Companion.SUFFIX
import io.spine.tools.core.jvm.field.FieldClassNames.Companion.UNDERSCORE

/**
 * A possible name of a field class.
 *
 * @property type The type for which the class is generated.
 * @property base The part of the name between the package words and the suffix.
 * @property isFixed If `true`, package words must not be added to the name.
 * @property needsPackage If `true`, the name has at least one package word, unless the type
 *   is declared without a package.
 * @property around The simple names of the classes around the generated class.
 * @property extraWords The number of package words added to the name because of clashes.
 *
 * @see FieldClassNames
 */
internal data class NameCandidate(
    val type: TypeName,
    private val base: String,
    val isFixed: Boolean,
    private val needsPackage: Boolean,
    private val around: List<String>,
    private val extraWords: Int = 0
) {
    private val packageWords: List<String> =
        type.packageName.split(".").filter { it.isNotEmpty() }.map { it.camelCase() }

    private val wordCount: Int =
        extraWords + if (needsPackage) minOf(1, packageWords.size) else 0

    /**
     * The name of the class, which has an underscore before the suffix if the name without it is
     * the name of a class around.
     */
    val name: String = run {
        val stem = packageWords.takeLast(wordCount).joinToString(separator = "") + base
        val usual = stem + SUFFIX
        if (usual in around) stem + UNDERSCORE + SUFFIX else usual
    }

    /**
     * Tells if the [name] is the name of a class around the generated class.
     */
    val isTakenAround: Boolean = name in around

    /**
     * Adds one more package word to the name, if it is possible.
     */
    fun lengthen(): NameCandidate =
        if (isFixed || wordCount == packageWords.size) this else copy(extraWords = extraWords + 1)

    /**
     * Obtains the candidates with all the names that the type may have, the shortest one first.
     */
    fun variants(): List<NameCandidate> =
        generateSequence(copy(extraWords = 0)) { shorter ->
            shorter.lengthen().takeIf { it != shorter }
        }.toList()
}

/**
 * Chooses different names for the types of these candidates.
 *
 * If it is not possible, the names of some of the resulting candidates clash.
 */
internal fun List<NameCandidate>.resolve(): List<NameCandidate> {
    val lengthened = lengthenUntilDifferent().associateBy { it.type }
    return relatedGroups().flatMap { group ->
        val candidates = group.map { lengthened.getValue(it.type) }
        if (candidates.haveNoExactClashes()) {
            candidates
        } else {
            group.choose { it.name.lowercase() }
                ?: group.choose { it.name }
                ?: candidates
        }
    }
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
private tailrec fun List<NameCandidate>.lengthenUntilDifferent(
    caseSensitive: Set<TypeName> = emptySet()
): List<NameCandidate> {
    val lengthened = lengthenClashing(caseSensitive)
    val unresolved = lengthened.clashing(caseSensitive)
        .filter { !it.isFixed && it.type !in caseSensitive }
        .map { it.type }
    return if (unresolved.isEmpty()) {
        lengthened
    } else {
        lengthenUntilDifferent(caseSensitive + unresolved)
    }
}

/**
 * Adds package words to the names that clash until they differ, or until there are no words
 * to add.
 */
private tailrec fun List<NameCandidate>.lengthenClashing(
    caseSensitive: Set<TypeName>
): List<NameCandidate> {
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
private fun List<NameCandidate>.clashing(caseSensitive: Set<TypeName>): Set<NameCandidate> {
    val sameIgnoringCase = groupBy { it.name.lowercase() }.values.filter { it.size > 1 }
    val withOthers = sameIgnoringCase.flatMap { group ->
        group.filter { candidate ->
            candidate.type !in caseSensitive || group.count { it.name == candidate.name } > 1
        }
    }
    val withClassesAround = filter { it.isTakenAround }
    return (withOthers + withClassesAround).toSet()
}

private fun List<NameCandidate>.haveNoExactClashes(): Boolean =
    none { it.isTakenAround } && distinctBy { it.name }.size == size

/**
 * Splits these candidates into groups, so that names of types from different groups
 * cannot clash even ignoring case.
 */
private fun List<NameCandidate>.relatedGroups(): List<List<NameCandidate>> =
    fold(emptyList()) { groups, candidate ->
        val (related, others) = groups.partition { group ->
            group.any { it.mayClashWith(candidate) }
        }
        others.plusElement(related.flatten() + candidate)
    }

private fun NameCandidate.mayClashWith(other: NameCandidate): Boolean {
    val names = variants().map { it.name.lowercase() }
    return other.variants().any { it.name.lowercase() in names }
}

/**
 * Chooses different names for the types of these candidates, preferring the shorter ones.
 *
 * Types are handled one by one. If all the names of a type are already chosen for other types,
 * other names are sought for those types. So, the names are found, if they exist.
 *
 * @param key The function that gives equal values for candidates with clashing names.
 * @return The candidates with different names, or `null` if it is not possible.
 */
private fun List<NameCandidate>.choose(key: (NameCandidate) -> String): List<NameCandidate>? {
    val chosen = mutableMapOf<String, NameCandidate>()

    fun place(candidate: NameCandidate, tried: MutableSet<String>): Boolean =
        candidate.variants().any { variant ->
            val name = key(variant)
            val isAllowed = !variant.isTakenAround && tried.add(name)
            val isPlaced = isAllowed && (chosen[name]?.let { place(it, tried) } ?: true)
            if (isPlaced) {
                chosen[name] = variant
            }
            isPlaced
        }

    val inStableOrder = sortedBy { it.type.qualifiedName }
    val allPlaced = inStableOrder.all { place(it, mutableSetOf()) }
    return if (allPlaced) chosen.values.toList() else null
}
