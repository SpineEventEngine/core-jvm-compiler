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

import com.google.protobuf.Descriptors.GenericDescriptor

/**
 * Obtains the place of the given [declaration] in the proto file of this type in the form used
 * in compilation errors.
 *
 * We search in the text of the proto file because the descriptors available from
 * the generated code do not have the information on the source code.
 *
 * @param declaration The text with which the declaration starts. If there are several such
 *   declarations in the file, the first one is taken.
 */
fun GenericDescriptor.placeOf(declaration: String): String {
    val classLoader = PluginTestSetup::class.java.classLoader
    val protoFile = checkNotNull(classLoader.getResource(file.name)) {
        "Unable to find the resource `${file.name}`."
    }
    val (index, line) = protoFile.readText().lines()
        .withIndex()
        .first { (_, line) -> declaration in line }
    return "${file.name}:${index + 1}:${line.indexOf(declaration) + 1}: "
}
