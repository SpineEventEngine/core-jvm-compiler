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

package io.spine.tools.core.jvm.routing.processor

import com.google.devtools.ksp.symbol.ClassKind.CLASS
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeReference
import com.squareup.kotlinpoet.ksp.toClassName
import io.spine.tools.core.jvm.ksp.processor.diagRef
import io.spine.tools.core.jvm.ksp.processor.toTypeArgument
import io.spine.tools.core.jvm.routing.processor.EntityClass.Companion.ID_METHOD_NAME


/**
 * Provides information about an entity class.
 *
 * @property decl The declaration of the class.
 * @param environment The environment for resolving generic parameters.
 */
internal class EntityClass(
    val decl: KSClassDeclaration,
    val environment: Environment
) {
    /**
     * Applies the given visitor to the class declaration.
     */
    fun accept(visitor: RouteVisitor<*>, data: Unit) {
        decl.accept(visitor, data)
    }

    /**
     * The type of the entity class resolved without generic parameters.
     */
    val type: KSType by lazy { decl.asStarProjectedType() }

    /**
     * The name of the package in which the entity class is declared.
     */
    val packageName: String by lazy { decl.packageName.asString() }

    /**
     * The name that the entity class gives to its routing classes, without a suffix.
     *
     * For a nested class, the name starts with the names of the enclosing classes.
     * The names are separated with underscores. For example, `Outer_Nested`.
     */
    val routingName: String by lazy {
        decl.toClassName().simpleNames.joinToString(separator = "_")
    }

    /**
     * The type of the entity identifiers as [KSTypeArgument].
     */
    val idClassTypeArgument: KSTypeArgument by lazy {
        resolveIdType(decl)
    }

    /**
     * Obtains the type of the entity identifiers as an instance of [KSTypeArgument].
     *
     * The function uses the reference to the [Entity.id][ID_METHOD_NAME] function
     * which returns the ID for resolving the type.
     */
    private fun resolveIdType(classDeclaration: KSClassDeclaration): KSTypeArgument {
        val idGetter = classDeclaration.getAllFunctions().find {
            it.simpleName.asString() == ID_METHOD_NAME
        }
        checkNotNull(idGetter) {
            "Unable to find the function named `$ID_METHOD_NAME` in" +
                    " the class `${classDeclaration.diagRef}`."
        }
        val idReturnType = idGetter.returnType!!.resolve()
        return idReturnType.toTypeArgument(environment.resolver)
    }

    /**
     * The reference to the ID class.
     */
    private val idClassReference: KSTypeReference by lazy {
        idClassTypeArgument.type!!
    }

    /**
     * The type of the entity identifiers.
     */
    val idClass: KSType by lazy {
        idClassReference.resolve().makeNotNullable()
    }

    /**
     * The class that this entity class extends.
     */
    fun superClass(): KSType {
        val found = decl.superTypes.find {
            val superType = it.resolve().declaration
            (superType is KSClassDeclaration) && (superType.classKind == CLASS)
        }
        return found!!.resolve().makeNotNullable()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EntityClass) return false
        return decl == other.decl
    }

    override fun hashCode(): Int = decl.hashCode()

    /**
     * Obtains the qualified name of the entity class.
     */
    override fun toString(): String = decl.qualifiedName!!.asString()

    companion object {

        /**
         * The name of the [id][io.spine.server.entity.Entity.id] method
         * of the [Entity][io.spine.server.entity.Entity] interface which is used
         * for obtaining the type of the entity identifiers.
         */
        private const val ID_METHOD_NAME = "id"
    }
}
