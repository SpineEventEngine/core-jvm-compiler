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

package io.spine.tools.mc.java.annotation

import io.spine.protodata.ast.FieldName
import io.spine.protodata.ast.MessageType
import io.spine.protodata.java.MessageOrBuilderConvention
import io.spine.protodata.java.MessageOrEnumConvention
import io.spine.tools.java.reference
import io.spine.tools.mc.annotation.ApiOption
import io.spine.tools.mc.annotation.FieldOptions
import io.spine.tools.mc.annotation.MessageFieldAnnotations

/**
 * Annotates methods for accessing fields of a message class, the builder of the message, and
 * `MessageOrBuilder` interface.
 */
internal class FieldAnnotator :
    ProtoAnnotator<MessageFieldAnnotations>(MessageFieldAnnotations::class.java) {

    private val convention by lazy {
        MessageOrEnumConvention(typeSystem)
    }

    private val messageOrBuilderConvention by lazy {
        MessageOrBuilderConvention(typeSystem)
    }

    override fun annotate(view: MessageFieldAnnotations) {
        view.fieldOptionsList.forEach { fieldOption ->
            annotateField(view, fieldOption)
        }
    }

    private fun annotateField(
        view: MessageFieldAnnotations,
        fieldOption: FieldOptions
    ) {
        val messageType = typeSystem.findMessage(view.type)!!.first
        fieldOption.optionList.forEach { option ->
            val apiOption = ApiOption.findMatching(option)
            check(apiOption != null) {
                "Unable to find an API option for `${option.name}`."
            }
            val annotationClass = annotationClass(apiOption)
            annotateAccessors(
                messageType,
                fieldOption.field,
                annotationClass
            )
        }
    }

    private fun annotateAccessors(
        messageType: MessageType,
        fieldName: FieldName,
        annotationClass: Class<out Annotation>
    ) {
        val messageDeclaration =
            convention.declarationFor(messageType.name)
        val messageOrBuilderDeclaration =
            messageOrBuilderConvention.declarationFor(messageType.name)

        val messageFile = sources.file(messageDeclaration.path)
        val messageClass = messageDeclaration.name

        val annotationLine = "@${annotationClass.reference}"

        val gettersInMessage = FieldAccessors(messageClass, fieldName)
        messageFile.at(gettersInMessage)
            .add(annotationLine)

        val builderClass = messageClass.nested("Builder")
        val builderAccessors = FieldAccessors(builderClass, fieldName)
        messageFile.at(builderAccessors)
            .add(annotationLine)

        val messageOrBuilderFile = sources.file(messageOrBuilderDeclaration.path)
        val messageOrBuilderClass = messageOrBuilderDeclaration.name
        val accessors = FieldAccessors(messageOrBuilderClass, fieldName)
        messageOrBuilderFile.at(accessors)
            .add(annotationLine)
    }
}
