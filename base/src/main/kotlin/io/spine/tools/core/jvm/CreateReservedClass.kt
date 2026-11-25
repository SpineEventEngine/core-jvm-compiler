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

package io.spine.tools.core.jvm

import com.intellij.psi.PsiAnnotation
import io.spine.tools.code.Java
import io.spine.tools.compiler.ast.MessageType
import io.spine.tools.compiler.context.CodegenContext
import io.spine.tools.compiler.jvm.render.CreateNestedClass
import io.spine.tools.compiler.render.SourceFile

/**
 * The base class for actions which nest a class with a reserved name in the Java class of
 * a message.
 *
 * A compilation error is reported if the name belongs to the message itself, to a type around it,
 * or to a type nested directly in it.
 *
 * The created class is annotated as [Generated][io.spine.annotation.Generated].
 *
 * @param type The type of the message.
 * @param file The file with the Java code generated for the [type].
 * @param simpleName The name of the class to be created.
 * @param context The code generation context in which this action runs.
 */
public abstract class CreateReservedClass(
    type: MessageType,
    file: SourceFile<Java>,
    simpleName: String,
    context: CodegenContext
) : CreateNestedClass(type, file, simpleName, context) {

    /**
     * What the message is called in the compilation error, e.g., "entity state".
     */
    protected open val ownerKind: String
        get() = "message"

    override fun createAnnotation(): PsiAnnotation = GeneratedAnnotation.forPsi()

    /**
     * Checks that the name is free, and then inserts the class into the message class.
     */
    override fun doRender() {
        ReservedName(
            value = simpleName,
            owner = type,
            ownerKind = ownerKind,
            typeSystem = typeSystem
        ).check()
        super.doRender()
    }
}
