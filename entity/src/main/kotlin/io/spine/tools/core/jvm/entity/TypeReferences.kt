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

package io.spine.tools.core.jvm.entity

import com.intellij.psi.PsiJavaFile
import io.spine.tools.compiler.ast.Field
import io.spine.tools.compiler.ast.MessageType
import io.spine.tools.compiler.jvm.ClassName
import io.spine.tools.compiler.jvm.javaClassName
import io.spine.tools.compiler.jvm.javaType
import io.spine.tools.compiler.jvm.render.findClass
import io.spine.tools.compiler.jvm.typeReference
import io.spine.tools.compiler.type.TypeSystem
import io.spine.tools.core.jvm.field.AddFieldClass

/**
 * Obtains references to the types of fields of an entity state for the code which is nested in
 * the Java class of the state.
 *
 * @param state The type of the entity state.
 * @param file The file with the Java class of the entity state.
 * @param typeSystem The type system to resolve the types of the fields.
 */
internal class TypeReferences(
    private val state: MessageType,
    private val file: PsiJavaFile,
    private val typeSystem: TypeSystem
) {

    private val stateClass: ClassName by lazy {
        state.javaClassName(typeSystem)
    }

    /**
     * The simple names of the classes nested in the class of the entity state.
     *
     * The classes with the [reserved names][reservedNames], and the class for
     * the [fields][AddFieldClass] may be generated later. So, their names are added explicitly.
     */
    private val nestedClasses: Set<String> by lazy {
        val present = file.findClass(stateClass).innerClasses.mapNotNull { it.name }
        present.toSet() + reservedNames + AddFieldClass.nameFor(state, typeSystem)
    }

    /**
     * Obtains a reference to the type of the given [field].
     *
     * The reference is the same as the one given by [typeReference], unless it is a simple name of
     * a top-level class which is also the name of a class nested in the class of the entity state.
     * The simple name would mean the nested class. Therefore, the fully qualified name is returned.
     */
    fun of(field: Field): String {
        val reference = field.typeReference(stateClass, typeSystem)
        val qualifiedName = field.javaType(typeSystem)
        val topLevel = qualifiedName == "${stateClass.packageName}.$reference"
        return if (topLevel && reference in nestedClasses) qualifiedName else reference
    }
}
