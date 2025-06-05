/*
 * Copyright 2025, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.spine.tools.mc.java.marker

import com.google.protobuf.Message
import com.intellij.psi.PsiJavaFile
import io.spine.protodata.java.ClassName
import io.spine.protodata.java.qualifiedJavaType
import io.spine.protodata.java.render.CreateInterface
import io.spine.protodata.java.render.superInterface
import io.spine.protodata.render.SourceFile
import io.spine.tools.code.Java
import io.spine.tools.java.reference
import io.spine.tools.mc.java.GeneratedAnnotation
import io.spine.tools.psi.java.addFirst
import io.spine.tools.psi.java.execute
import io.spine.tools.psi.java.topLevelClass

/**
 * Makes message classes implement an interface specified by the option `(every_is).java_type`.
 *
 * If the value of `(every_is).generated` is set to `true`, the interface is generated.
 * Otherwise, the generated code assumes that the interface already exists.
 *
 * @see IsOptionRenderer
 */
internal class EveryIsOptionRenderer : MarkerRenderer<EveryIsMessages>() {

    override fun doRender(view: EveryIsMessages) {
        execute {
            view.run {
                val interfaceName = option.qualifiedJavaType(header)
                createInterface(interfaceName)
                implementInTypes(interfaceName)
            }
        }
    }

    /**
     * The super base interface for all generated interfaces.
     */
    private val superBase = superInterface {
        name = Message::class.java.reference
    }

    private fun EveryIsMessages.createInterface(name: InterfaceName) {
        if (option.generate) {
            val iface = ClassName.guess(name)
            val createdFile = CreateInterface(iface, superBase).render(sources)
            annotate(createdFile)
        }
    }

    private fun EveryIsMessages.implementInTypes(name: InterfaceName) {
        val interfaceProto = superInterface {
            this.name = name
        }
        typeList.forEach {
            it.implementInterface(interfaceProto)
        }
    }
}

private fun annotate(file: SourceFile<Java>) {
    val psiFile = file.psi() as PsiJavaFile
    val annotation = GeneratedAnnotation.forPsi()
    psiFile.topLevelClass.addFirst(annotation)
    val updatedCode = psiFile.text
    file.overwrite(updatedCode)
}
