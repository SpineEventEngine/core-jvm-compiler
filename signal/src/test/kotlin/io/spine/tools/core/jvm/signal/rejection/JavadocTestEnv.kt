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

package io.spine.tools.core.jvm.signal.rejection

import io.spine.code.java.PackageName
import io.spine.string.ti
import io.spine.tools.core.jvm.signal.rejection.Javadoc.BUILDER_ABSTRACT_TEMPLATE
import io.spine.tools.core.jvm.signal.rejection.Javadoc.PROTO_MESSAGE_NOTE_TEMPLATE
import io.spine.tools.fs.div
import io.spine.tools.fs.DirectoryName.generated
import io.spine.tools.fs.DirectoryName.java
import io.spine.tools.fs.DirectoryName.main
import io.spine.tools.java.fs.toDirectory
import java.io.File
import kotlin.io.path.div

/**
 * The environment for the integration tests checking Javadocs in
 * the generated rejections code.
 *
 * This object provides the expected Javadoc text of the generated code.
 * The values assume that the text of a comment is a single line, without
 * the comment delimiters and the leading asterisks, and that each run of
 * whitespace is replaced with a single space.
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

    fun rejectionJavaFile(projectDir: File): File {
        val javaPackage = PackageName.of(JAVA_PACKAGE).toDirectory()
        val filePath =
            projectDir.toPath() / generated / main / java / javaPackage / "$REJECTION_NAME.java"
        return filePath.toFile()
    }

    fun expectedClassComment(): String =
        wrappedInPreTag(TYPE_COMMENT) + " " +
                PROTO_MESSAGE_NOTE_TEMPLATE.replace("\$L.\$L", "$PROTO_PACKAGE.$REJECTION_NAME")

    fun expectedBuilderClassComment() =
        BUILDER_ABSTRACT_TEMPLATE.replace("\$L", REJECTION_NAME)

    fun expectedFirstFieldComment(): String =
        wrappedInPreTag(FIRST_FIELD_COMMENT)

    fun expectedSecondFieldComment(): String =
        wrappedInPreTag(SECOND_FIELD_COMMENT)

    private fun wrappedInPreTag(commentText: String) =
        "<pre> $commentText </pre>"
}
