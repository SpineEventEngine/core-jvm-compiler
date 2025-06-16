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

package io.spine.tools.core.jvm.signal.rejection

import com.google.protobuf.BoolValue
import com.google.protobuf.DescriptorProtos.FileOptions.JAVA_MULTIPLE_FILES_FIELD_NUMBER
import com.google.protobuf.DescriptorProtos.FileOptions.JAVA_OUTER_CLASSNAME_FIELD_NUMBER
import com.google.protobuf.DescriptorProtos.FileOptions.JAVA_PACKAGE_FIELD_NUMBER
import io.spine.protobuf.pack
import io.spine.tools.compiler.ast.EnumConstant
import io.spine.tools.compiler.ast.EnumType
import io.spine.tools.compiler.ast.Field
import io.spine.tools.compiler.ast.File
import io.spine.tools.compiler.ast.MessageType
import io.spine.tools.compiler.ast.Option
import io.spine.tools.compiler.ast.PrimitiveType.TYPE_BOOL
import io.spine.tools.compiler.ast.PrimitiveType.TYPE_STRING
import io.spine.tools.compiler.ast.ProtoFileHeader
import io.spine.tools.compiler.ast.Service
import io.spine.tools.compiler.ast.ServiceName
import io.spine.tools.compiler.ast.TypeName
import io.spine.tools.compiler.ast.constantName
import io.spine.tools.compiler.ast.enumConstant
import io.spine.tools.compiler.ast.fieldName
import io.spine.tools.compiler.ast.fieldType
import io.spine.tools.compiler.ast.file
import io.spine.tools.compiler.ast.messageType
import io.spine.tools.compiler.ast.option
import io.spine.tools.compiler.ast.protoFileHeader
import io.spine.tools.compiler.ast.protobufSourceFile
import io.spine.tools.compiler.ast.service
import io.spine.tools.compiler.ast.serviceName
import io.spine.tools.compiler.ast.toPath
import io.spine.tools.compiler.ast.type
import io.spine.tools.compiler.ast.typeName
import io.spine.tools.compiler.protobuf.ProtoFileList
import io.spine.tools.compiler.type.TypeSystem
import io.spine.tools.compiler.value.pack
import io.spine.tools.compiler.ast.enumType as newEnumType
import io.spine.tools.compiler.ast.field as newField

object TypesTestEnv {

    private val protoSourceMultiple: File = file { path = "acme/example/multiple.proto" }
    private val protoSourceSingle: File = file { path = "acme/example/single.proto" }
    private val rejectionsFile: File = file {
        path = "acme/example/cartoon_rejections.proto"
    }
    private val multipleFilesOption: Option = option {
        name = "java_multiple_files"
        number = JAVA_MULTIPLE_FILES_FIELD_NUMBER
        type = type { primitive = TYPE_BOOL }
        value = BoolValue.of(true).pack()
    }
    private val javaPackageOption: Option = option {
        name = "java_package"
        number = JAVA_PACKAGE_FIELD_NUMBER
        type = type { primitive = TYPE_STRING }
        value = "dev.acme.example".pack()
    }
    private val outerClassnameOption: Option = option {
        name = "java_outer_classname"
        number = JAVA_OUTER_CLASSNAME_FIELD_NUMBER
        type = type { primitive = TYPE_STRING }
        value = "CartoonRejections".pack()
    }
    private val multipleFilesHeader: ProtoFileHeader = protoFileHeader {
        file = protoSourceMultiple
        packageName = "acme.example"
        option.add(multipleFilesOption)
        option.add(javaPackageOption)
    }
    private val singleFileHeader: ProtoFileHeader = protoFileHeader {
        file = protoSourceSingle
        packageName = "acme.example"
        option.add(javaPackageOption)
    }
    private val rejectionsProtoHeader: ProtoFileHeader = protoFileHeader {
        file = rejectionsFile
        packageName = "acme.example"
        option.add(javaPackageOption)
        option.add(outerClassnameOption)
    }
    val messageTypeName: TypeName = typeName {
        packageName = multipleFilesHeader.packageName
        simpleName = "Foo"
        typeUrlPrefix = "type.spine.io"
    }
    val rejectionTypeName: TypeName = typeName {
        packageName = rejectionsProtoHeader.packageName
        simpleName = "CannotDrawCartoon"
        typeUrlPrefix = "type.spine.io"
    }
    private val stringField: Field = newField {
        type = fieldType { primitive = TYPE_STRING }
        name = fieldName { value = "bar" }
        declaringType = messageTypeName
    }
    private val idField: Field = newField {
        type = fieldType { primitive = TYPE_STRING }
        name = fieldName { value = "uuid" }
        declaringType = messageTypeName
    }
    private val messageType: MessageType = messageType {
        file = protoSourceMultiple
        name = messageTypeName
        field.add(stringField)
    }
    private val rejectionType: MessageType = messageType {
        file = rejectionsFile
        name = rejectionTypeName
        field.add(idField)
    }
    private val enumTypeName: TypeName = typeName {
        packageName = multipleFilesHeader.packageName
        typeUrlPrefix = messageTypeName.typeUrlPrefix
        simpleName = "Kind"
    }
    private val undefinedConstant: EnumConstant = enumConstant {
        name = constantName { value = "UNDEFINED" }
        number = 0
        declaredIn = enumTypeName
    }
    private val enumConstant: EnumConstant = enumConstant {
        name = constantName { value = "INSTANCE" }
        number = 1
        declaredIn = enumTypeName
    }
    private val enumType: EnumType = newEnumType {
        file = protoSourceMultiple
        name = enumTypeName
        constant.add(undefinedConstant)
        constant.add(enumConstant)
    }
    private val serviceNameMultiple: ServiceName = serviceName {
        simpleName = "ServiceFromSourceWithMultipleFilesTrue"
        packageName = "multiple.file.sample"
        typeUrlPrefix = "service.spine.io"
    }
    private val serviceNameSingle: ServiceName = serviceName {
        simpleName = "ServiceFromSourceWithMultipleFilesFalse"
        packageName = "single.file.sample"
        typeUrlPrefix = "service.spine.io"
    }
    private val serviceFromMultiple: Service = service {
        file = protoSourceMultiple
        name = serviceNameMultiple
    }
    private val serviceFromSingle: Service = service {
        file = protoSourceSingle
        name = serviceNameSingle
    }
    val typeSystem: TypeSystem = run {
        val multipleFilesProto = protobufSourceFile {
            file = protoSourceMultiple
            header = multipleFilesHeader
            type.put(messageTypeName.typeUrl, messageType)
            enumType.put(enumTypeName.typeUrl, TypesTestEnv.enumType)
            service.put(serviceNameMultiple.typeUrl, serviceFromMultiple)
        }
        val singleFileProto = protobufSourceFile {
            file = protoSourceSingle
            header = singleFileHeader
            service.put(serviceNameSingle.typeUrl, serviceFromSingle)
        }
        val rejections = protobufSourceFile {
            file = rejectionsFile
            header = rejectionsProtoHeader
            type.put(rejectionTypeName.typeUrl, rejectionType)
        }
        val compiledProtoFiles = ProtoFileList(
            listOf(protoSourceMultiple, protoSourceSingle, rejectionsFile).map {
                it.toPath().toFile()
            }
        )
        val definitions = setOf(
            multipleFilesProto,
            singleFileProto,
            rejections
        )
        TypeSystem(compiledProtoFiles, definitions)
    }
}
