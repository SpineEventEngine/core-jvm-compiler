/*
 * Copyright 2026, TeamDev. All rights reserved.
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
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import io.spine.protobuf.pack
import io.spine.tools.compiler.ast.Option
import io.spine.tools.compiler.ast.PrimitiveType.TYPE_BOOL
import io.spine.tools.compiler.ast.PrimitiveType.TYPE_STRING
import io.spine.tools.compiler.ast.ProtobufSourceFile
import io.spine.tools.compiler.ast.file
import io.spine.tools.compiler.ast.option
import io.spine.tools.compiler.ast.protoFileHeader
import io.spine.tools.compiler.ast.protobufSourceFile
import io.spine.tools.compiler.ast.type
import io.spine.tools.compiler.value.pack
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("`checkRejectionConventions` should")
internal class RejectionConventionsSpec {

    @Nested
    inner class `require a single output file` {

        @Test
        fun `rejecting 'java_multiple_files' set to 'true'`() {
            val file = rejectionsFile(multipleFiles = true)

            val error = shouldThrow<IllegalArgumentException> {
                file.checkRejectionConventions()
            }

            // The error must clearly tell what is wrong, where, and how to fix it.
            error.message.let {
                it shouldContain REJECTIONS_PATH
                it shouldContain "java_multiple_files"
                it shouldContain "false"
            }
        }

        @Test
        fun `accepting 'java_multiple_files' set to 'false'`() {
            val file = rejectionsFile(multipleFiles = false)
            shouldNotThrowAny {
                file.checkRejectionConventions()
            }
        }

        @Test
        fun `accepting an absent 'java_multiple_files' option`() {
            val file = rejectionsFile()
            shouldNotThrowAny {
                file.checkRejectionConventions()
            }
        }
    }

    @Nested
    inner class `require a 'Rejections' outer class` {

        @Test
        fun `rejecting an outer class name not ending with 'Rejections'`() {
            val file = rejectionsFile(outerClassName = "CartoonErrors")

            val error = shouldThrow<IllegalArgumentException> {
                file.checkRejectionConventions()
            }

            error.message.let {
                it shouldContain REJECTIONS_PATH
                it shouldContain "CartoonErrors"
                it shouldContain "Rejections"
            }
        }

        @Test
        fun `accepting an outer class name ending with 'Rejections'`() {
            val file = rejectionsFile(outerClassName = "CartoonRejections")
            shouldNotThrowAny {
                file.checkRejectionConventions()
            }
        }

        @Test
        fun `accepting an absent 'java_outer_classname' option`() {
            // The default outer class name is derived from the file name
            // (`cartoon_rejections.proto` -> `CartoonRejections`), and ends with `Rejections`.
            val file = rejectionsFile()
            shouldNotThrowAny {
                file.checkRejectionConventions()
            }
        }
    }
}

private const val REJECTIONS_PATH = "acme/example/cartoon_rejections.proto"

/**
 * Creates a stub rejection [ProtobufSourceFile] with the given file options.
 *
 * Both options are omitted from the file header when the corresponding argument is `null`.
 */
private fun rejectionsFile(
    multipleFiles: Boolean? = null,
    outerClassName: String? = null
): ProtobufSourceFile {
    val source = file { path = REJECTIONS_PATH }
    val fileOptions = buildList {
        multipleFiles?.let { add(multipleFilesOption(it)) }
        outerClassName?.let { add(outerClassnameOption(it)) }
    }
    val fileHeader = protoFileHeader {
        file = source
        packageName = "acme.example"
        fileOptions.forEach { option.add(it) }
    }
    return protobufSourceFile {
        file = source
        header = fileHeader
    }
}

private fun multipleFilesOption(enabled: Boolean): Option = option {
    name = "java_multiple_files"
    number = JAVA_MULTIPLE_FILES_FIELD_NUMBER
    type = type { primitive = TYPE_BOOL }
    value = BoolValue.of(enabled).pack()
}

private fun outerClassnameOption(className: String): Option = option {
    name = "java_outer_classname"
    number = JAVA_OUTER_CLASSNAME_FIELD_NUMBER
    type = type { primitive = TYPE_STRING }
    value = className.pack()
}
