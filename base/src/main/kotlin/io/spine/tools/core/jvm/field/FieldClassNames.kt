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
 * If there are no more words for a name that still clashes, the names of types related to it are
 * chosen one by one, so that they differ. A type may then have fewer package words than others.
 *
 * If there is no way to give different names, the compilation fails.
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
    private fun candidatesFor(types: List<TypeName>): List<NameCandidate> {
        val byPackage = types.groupBy { it.packageName }
        return byPackage.flatMap { (packageName, neighbours) ->
            val keepsPackage =
                byPackage.size == 1 || packageName == messageType.name.packageName
            val leastNested = neighbours.leastNested()
            neighbours.map { type ->
                val keepsShortName = type == leastNested
                NameCandidate(
                    type = type,
                    base = if (keepsShortName) type.shortName else type.underscoredName,
                    isFixed = keepsShortName && keepsPackage,
                    needsPackage = !keepsPackage,
                    around = fieldClass.simpleNames
                )
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

        /**
         * The separator of the parts of names that would clash without it.
         */
        const val UNDERSCORE = "_"
    }
}

private val TypeName.path: List<String>
    get() = nestingTypeNameList + simpleName

private val TypeName.shortName: String
    get() = path.joinToString(separator = "")

private val TypeName.underscoredName: String
    get() = path.joinToString(separator = FieldClassNames.UNDERSCORE)

/**
 * Obtains the type that is nested less deeply than the others, or
 * `null` if there are several such types.
 */
private fun List<TypeName>.leastNested(): TypeName? {
    val depth = minOf { it.path.size }
    return singleOrNull { it.path.size == depth }
}
