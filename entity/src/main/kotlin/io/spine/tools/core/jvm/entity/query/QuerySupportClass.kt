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

package io.spine.tools.core.jvm.entity.query

import com.intellij.psi.PsiAnnotation
import io.spine.tools.compiler.ast.Field
import io.spine.tools.compiler.ast.MessageType
import io.spine.tools.compiler.context.CodegenContext
import io.spine.tools.compiler.jvm.ClassName
import io.spine.tools.compiler.jvm.render.CreateNestedClass
import io.spine.tools.compiler.render.SourceFile
import io.spine.tools.code.Java
import io.spine.tools.core.jvm.GeneratedAnnotation
import io.spine.tools.core.jvm.entity.TypeReferences
import io.spine.tools.core.jvm.entity.idField
import io.spine.tools.core.jvm.settings.Entities

/**
 * Abstract base for classes generating code for supporting queries for
 * entity state types.
 */
internal abstract class QuerySupportClass(
    type: MessageType,
    file: SourceFile<Java>,
    className: String,
    protected val settings: Entities,
    context: CodegenContext
) : CreateNestedClass(type, file, className, context) {

    override fun createAnnotation(): PsiAnnotation = GeneratedAnnotation.forPsi()

    /**
     * The class of the entity state, same as [messageClass].
     */
    protected val entityStateClass: ClassName by ::messageClass

    /**
     * The simple name of the entity state type, which is equivalent of
     * the simple class name for the generated Java class.
     */
    protected val stateType: String = type.name.simpleName

    /**
     * The identifier field of the entity state.
     */
    val idField: Field by lazy {
        type.idField(settings)
    }

    /**
     * The references to the types of the fields of the entity state.
     */
    protected val typeReferences: TypeReferences by lazy {
        TypeReferences(
            file = psiFile,
            stateClass = entityStateClass,
            typeSystem = typeSystem
        )
    }

    /**
     * The type of the [first][idField] entity state type field.
     */
    val idType: String by lazy {
        typeReferences.of(idField)
    }
}
