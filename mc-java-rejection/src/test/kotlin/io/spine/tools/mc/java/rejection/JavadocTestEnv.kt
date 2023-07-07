/*
 * Copyright 2023, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.tools.mc.java.rejection

import io.spine.code.java.PackageName
import io.spine.string.ti
import io.spine.tools.div
import io.spine.tools.fs.DirectoryName.generated
import io.spine.tools.fs.DirectoryName.java
import io.spine.tools.fs.DirectoryName.main
import io.spine.tools.java.fs.toDirectory
import java.nio.file.Path
import kotlin.io.path.div

/**
 * The environment for the integration tests checking Javadocs in
 * the generated rejections code.
 *
 * @see RejectionJavadocIgTest
 */
internal object JavadocTestEnv {

    private const val JAVA_PACKAGE = "io.spine.sample.rejections"
    private const val PROTO_PACKAGE = "spine.sample.rejections"
    private const val TYPE_COMMENT = "The rejection definition to test Javadoc generation."
    private const val REJECTION_NAME = "Rejection"
    private const val FIRST_FIELD_COMMENT = "The rejection ID."
    private const val FIRST_FIELD = "id"
    private const val SECOND_FIELD_COMMENT = "The rejection message."
    private const val SECOND_FIELD = "rejection_message"

    fun rejectionFileContent(): List<String> =
       """
       syntax = "proto3";
       package $PROTO_PACKAGE;
       option java_package = "$JAVA_PACKAGE";
       option java_multiple_files = false;
       
       // $TYPE_COMMENT
       message $REJECTION_NAME {
       
           // $FIRST_FIELD_COMMENT
           int32 $FIRST_FIELD = 1; // Is not a part of Javadoc.
            
           // $SECOND_FIELD_COMMENT
           string $SECOND_FIELD = 2;
           
           bool hasNoComment = 3;
       }     
       """.ti().lines()

    fun rejectionsJavadocThrowableSource(projectDir: Path): Path {
        val javaPackage = PackageName.of(JAVA_PACKAGE).toDirectory()
        return projectDir / generated / main / java / javaPackage / "$REJECTION_NAME.java"
    }

    fun expectedClassComment(): String = (
            wrappedInPreTag(TYPE_COMMENT)
            + " <p>The rejection message proto type is "
            + " {@code $PROTO_PACKAGE.$REJECTION_NAME}"
        )

    fun expectedBuilderClassComment() =
        "The builder for the  {@code $REJECTION_NAME}  rejection."

    fun expectedFirstFieldComment(): String =
        wrappedInPreTag(FIRST_FIELD_COMMENT)

    fun expectedSecondFieldComment(): String =
        wrappedInPreTag(SECOND_FIELD_COMMENT)

    private fun wrappedInPreTag(commentText: String) =
        "<pre> $commentText </pre>"
}
