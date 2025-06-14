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

import io.spine.protobuf.isNotDefault
import io.spine.protodata.ast.TypeName
import io.spine.protodata.java.BaseJavaConvention
import io.spine.protodata.java.ClassName
import io.spine.protodata.java.javaPackage
import io.spine.protodata.type.Declaration
import io.spine.protodata.type.TypeSystem
import io.spine.tools.code.Java

/**
 * A convention which governs Java Rejection-Throwable class declarations.
 *
 * The convention only defines a declaration for rejection message types. Any other types are
 * undefined and thus result in the [declarationFor] method returning `null`.
 */
public class RejectionThrowableConvention(
    typeSystem: TypeSystem
) : BaseJavaConvention<TypeName, ClassName>(typeSystem) {

    @Suppress("ReturnCount")
    override fun declarationFor(name: TypeName): Declaration<Java, ClassName>? {
        val declaration = typeSystem.findMessage(name) ?: return null
        val (msg, header) = declaration
        val fileName = header.file.path
        if (!fileName.endsWith("rejections.proto") // Not a rejection message.
            || msg.declaredIn.isNotDefault()       // Not a top-level message.
        ) {
            return null
        }
        val packageName = header.javaPackage()
        val simpleName = name.simpleName
        val cls = ClassName(packageName, simpleName)
        return Declaration(cls, cls.javaFile)
    }
}
