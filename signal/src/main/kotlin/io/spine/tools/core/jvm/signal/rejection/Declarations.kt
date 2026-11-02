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

import io.spine.string.Indent.Companion.defaultJavaIndent

/**
 * Composes the code of a class declaration.
 *
 * @param javadoc The text of the Javadoc comment, or an empty string for no comment.
 * @param header The code preceding the class body, such as annotations, modifiers,
 *   and the class name.
 * @param members The code of the class members, which the declaration separates
 *   with blank lines.
 */
internal fun classDeclaration(javadoc: String, header: String, members: List<String>): String =
    declaration(javadoc = javadoc, header = header, body = members.joinToString("\n\n"))

/**
 * Composes the code of a method or a constructor.
 *
 * @param javadoc The text of the Javadoc comment, or an empty string for no comment.
 * @param signature The code preceding the method body, such as annotations, modifiers,
 *   the return type, the name, and the parameters.
 * @param statements The statements of the method body.
 */
internal fun methodDeclaration(
    javadoc: String = "",
    signature: String,
    statements: List<String>
): String = declaration(
    javadoc = javadoc,
    header = signature,
    body = statements.joinToString("\n")
)

private fun declaration(javadoc: String, header: String, body: String): String = buildString {
    if (javadoc.isNotEmpty()) {
        append(javadocComment(javadoc)).append('\n')
    }
    append(header).append(" {\n")
    if (body.isNotEmpty()) {
        append(body.indented()).append('\n')
    }
    append('}')
}

/**
 * Formats the given text as a Javadoc comment.
 *
 * Each line of the text becomes a line of the comment.
 * The line separator ending the text does not start a new line.
 */
private fun javadocComment(text: String): String {
    val lines = text.split(lineBreak)
    val textLines = if (lines.last().isEmpty()) lines.dropLast(1) else lines
    val commentLines = textLines.map { if (it.isEmpty()) " *" else " * $it" }
    return (listOf("/**") + commentLines + " */").joinToString("\n")
}

/**
 * A pattern matching any line break, as JavaPoet did when it generated the Javadoc
 * of rejections.
 */
private val lineBreak = Regex("\\R")

private fun String.indented(): String =
    split('\n').joinToString("\n") { if (it.isEmpty()) it else defaultJavaIndent.value + it }
