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
import com.squareup.javapoet.JavaFile
import io.spine.logging.WithLogging
import io.spine.tools.compiler.ast.MessageType
import io.spine.tools.compiler.ast.ProtobufSourceFile
import io.spine.tools.compiler.ast.find
import io.spine.tools.compiler.ast.isTopLevel
import io.spine.tools.compiler.jvm.file.hasJavaRoot
import io.spine.tools.compiler.jvm.javaOuterClassName
import io.spine.tools.compiler.jvm.javaPackage
import io.spine.tools.compiler.jvm.render.JavaRenderer
import io.spine.tools.compiler.render.SourceFileSet
import io.spine.string.Indent.Companion.defaultJavaIndent
import io.spine.string.ti
import java.nio.file.Path

/**
 * A renderer of classes implementing [RejectionThrowable][io.spine.base.RejectionThrowable].
 *
 * The output is placed in the `java` subdirectory under the [outputRoot][SourceFileSet.outputRoot]
 * directory of the given [sources]. Other subdirectories, such as `grpc` or `kotlin`, are ignored.
 */
internal class RThrowableRenderer: JavaRenderer(), WithLogging {

    private lateinit var sources: SourceFileSet

    override fun render(sources: SourceFileSet) {
        // We could receive `grpc` or `kotlin` output roots here. Now we do only `java`.
        if (!sources.hasJavaRoot) {
            return
        }
        this.sources = sources
        val rejectionFiles = findRejectionFiles()
        rejectionFiles.forEach {
            generateRejections(it)
        }
    }

    private fun findRejectionFiles(): List<ProtobufSourceFile> {
        val result = select(ProtobufSourceFile::class.java).all()
            .filter { it.isRejections() }

        result.forEach { it.checkConventions() }

        if (result.isNotEmpty()) {
            logger.atDebug().log {
                val nl = System.lineSeparator()
                val fileList = result.joinToString(separator = nl) { " * `${it.file.path}`" }
                "Found ${result.size} rejection files:$nl$fileList"
            }
        }

        return result
    }

    private fun generateRejections(protoFile: ProtobufSourceFile) {
        if (protoFile.typeMap.isEmpty()) {
            logger.atWarning().log {
                "No rejection types found in the file `${protoFile.file.path}`."
            }
            return
        }
        logger.atDebug().log {
            """
            Generating rejection classes for `${protoFile.file.path}`.
                  Java package: `${protoFile.javaPackage()}`.
                  Outer class name: `${protoFile.outerClassName()}`.
                  Output directory: `${sources.outputRoot}`.            
            """.ti()
        }
        protoFile.typeMap.values
            .filter { it.isTopLevel }
            .forEach {
                generateRejection(protoFile, it)
            }
    }

    private fun generateRejection(protoFile: ProtobufSourceFile, rejection: MessageType) {
        val rtCode = RThrowableCode(protoFile.javaPackage(), rejection, typeSystem)
        val file = rejection.throwableJavaFile(protoFile)
        rtCode.writeToFile(file)

        logger.atDebug().log {
            val nl = System.lineSeparator()
            val rejectionName = "`${rejection.qualifiedName}`"
            // The padding is to align the file name with the rejection name.
            "$rejectionName ->$nl$      `$file`"
        }
    }

    /**
     * Obtains a name of the Java file corresponding to this [rejection message][MessageType] type.
     *
     * @param protoFile The file which declares this rejection type. Serves for calculating
     *   the Java package.
     */
    private fun MessageType.throwableJavaFile(protoFile: ProtobufSourceFile): Path {
        val javaPackage = protoFile.javaPackage()
        val packageDir = sources.outputRoot.resolve(javaPackage.replace('.', '/'))
        val file = packageDir.resolve("${name.simpleName}.java")
        return file
    }

    private fun RThrowableCode.writeToFile(file: Path) {
        val typeSpec = toPoet()
        val javaFile = JavaFile.builder(javaPackage, typeSpec)
            .skipJavaLangImports(true)
            .indent(defaultJavaIndent.value)
            .build()
        val appendable = StringBuilder()
        javaFile.writeTo(appendable)
        sources.createFile(file, appendable.toString())
    }
}

private fun ProtobufSourceFile.isRejections(): Boolean =
    file.path.endsWith("rejections.proto")

internal typealias RejectionFile = ProtobufSourceFile

/**
 * Ensures that this rejection file is configured according to the conventions.
 *
 * `java_multiple_files` option must be set to `false` or not specified, and
 * `java_outer_classname` must end with `Rejections` or absent.
 */
private fun RejectionFile.checkConventions() {
    checkNotMultipleFiles()
    checkOuterClassName()
}

private const val JAVA_MULTIPLE_FILES: String = "java_multiple_files"

private fun RejectionFile.checkNotMultipleFiles() {
    val javaMultipleFiles = header.optionList.find(JAVA_MULTIPLE_FILES, BoolValue::class.java)
    javaMultipleFiles?.let {
        check(!it.value) {
            "A rejection file (`${file.path}`) should generate" +
                    " Java classes into a single source code file." +
                    " Please set `$JAVA_MULTIPLE_FILES` option to `false`."
        }
    }
}

private const val OUTER_CLASS_NAME: String = "java_outer_classname"
private const val REJECTIONS_CLASS_SUFFIX: String = "Rejections"

internal fun RejectionFile.outerClassName(): String = header.javaOuterClassName()

private fun RejectionFile.checkOuterClassName() {
    val outerClassName = outerClassName()
    check(outerClassName.endsWith(REJECTIONS_CLASS_SUFFIX)) {
        "A rejection file (`${file.path}`) should have" +
                " the outer class named ending with `$REJECTIONS_CLASS_SUFFIX` or" +
                " do not have the option `$OUTER_CLASS_NAME` at all." +
                " Encountered outer class name: `$outerClassName`." +
                " Please rename the outer class or remove the option."
    }
}

/**
 * Obtains the Java package name for the given rejection file, taking into account
 * the `java_package` option.
 */
private fun RejectionFile.javaPackage(): String = header.javaPackage()
