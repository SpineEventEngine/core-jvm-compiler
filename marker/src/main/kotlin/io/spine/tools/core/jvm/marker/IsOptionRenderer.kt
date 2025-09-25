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

package io.spine.tools.core.jvm.marker

import io.spine.option.IsOption
import io.spine.tools.compiler.ast.MessageType
import io.spine.tools.compiler.ast.find
import io.spine.tools.compiler.ast.qualifiedName
import io.spine.tools.compiler.context.findHeader
import io.spine.tools.compiler.jvm.qualifiedJavaType
import io.spine.tools.compiler.jvm.render.superInterface
import io.spine.tools.psi.java.execute

/**
 * Makes message classes implement an interface specified in the option `(is).java_type`.
 *
 * @see EveryIsOptionRenderer
 */
internal class IsOptionRenderer : MarkerRenderer<MessagesWithIs>() {

    override fun doRender(view: MessagesWithIs) {
        view.typeList.forEach {
            doRender(it)
        }
    }

    private fun doRender(type: MessageType) {
        val isOption = type.optionList.find<IsOption>()
        check(isOption != null) {
            "Unable to find `(is)` option for the type `${type.name.qualifiedName}`."
        }
        val header = findHeader(type.file)!!
        val interfaceName = isOption.qualifiedJavaType(header)
        val interfaceProto = superInterface {
            name = interfaceName
        }

        execute {
            type.implementInterface(interfaceProto)
        }
    }
}
