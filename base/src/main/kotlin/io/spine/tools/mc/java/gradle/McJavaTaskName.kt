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
package io.spine.tools.mc.java.gradle

import io.spine.tools.code.SourceSetName
import io.spine.tools.code.SourceSetName.Companion.main
import io.spine.tools.code.SourceSetName.Companion.test
import io.spine.tools.gradle.task.TaskName
import io.spine.tools.gradle.task.TaskWithSourceSetName

/**
 * Names of Gradle tasks defined by the Spine Model Compiler plugin.
 */
public class McJavaTaskName(value: String, ssn: SourceSetName) : TaskWithSourceSetName(value, ssn) {

    public companion object {

        /** Additional cleanup task added to the Gradle lifecycle. */
        @JvmField
        public val preClean: TaskName = PreCleanTaskName()

        /**
         * Obtains the name of the task which annotates Java code according to
         * visibility options defined in proto files.
         */
        @JvmStatic
        public fun annotateProto(ssn: SourceSetName): TaskName =
            McJavaTaskName("annotate${ssn.toInfix()}Proto", ssn)

        /**
         * Obtains the name of the task which generate rejections for the specified source set.
         */
        @JvmStatic
        public fun generateRejections(ssn: SourceSetName): TaskName =
            McJavaTaskName("generate${ssn.toInfix()}Rejections", ssn)

        /**
         * Obtains the name of the task which merges descriptor set files of the specified
         * source set.
         */
        @JvmStatic
        public fun mergeDescriptorSet(ssn: SourceSetName): TaskName =
            McJavaTaskName("merge${ssn.toInfix()}DescriptorSet", ssn)

        /**
         * Obtains the name of the task which creates Protobuf compiler plugin configuration for
         * the code in the specified source set.
         */
        @JvmStatic
        public fun writePluginConfiguration(ssn: SourceSetName): TaskName =
            McJavaTaskName("write${ssn.toInfix()}PluginConfiguration", ssn)

        /**
         * Obtains the name of the task which creates the `desc.ref` file containing the reference
         * to the descriptor file(s) with the known types from the specified source set.
         */
        @JvmStatic
        public fun writeDescriptorReference(ssn: SourceSetName): TaskName =
            McJavaTaskName("write${ssn.toInfix()}DescriptorReferences", ssn)

        /**
         * Obtains the name of the task which launches ProtoData for the given source set.
         */
        @JvmStatic
        public fun launchProtoData(ssn: SourceSetName): TaskName =
            McJavaTaskName("launch${ssn.toInfix()}ProtoData", ssn)

        /** The name of the task which launches ProtoData for the main source set. */
        @JvmField
        public val launchProtoData: TaskName = launchProtoData(main)

        /** Generates source code of rejections in the `main` source set. */
        @JvmField
        public val generateRejections: TaskName = generateRejections(main)

        /** Generates source code of rejections in the `test` scope. */
        @JvmField
        public val generateTestRejections: TaskName = generateRejections(test)

        /** Annotates the Java sources generated from `.proto` files the `main` scope. */
        @JvmField
        @Deprecated("No longer used")
        public val annotateProto: TaskName = annotateProto(main)

        /** Annotates the Java sources generated from `.proto` files the `test` scope. */
        @JvmField
        @Deprecated("No longer used")
        public val annotateTestProto: TaskName = annotateProto(test)

        /**
         * Merges all the known type descriptors of the module into one in the `main` source set.
         */
        @JvmField
        public val mergeDescriptorSet: TaskName = mergeDescriptorSet(main)

        /**
         * Merges all the known type descriptors of the module into one in the `test` source set.
         */
        @JvmField
        public val mergeTestDescriptorSet: TaskName = mergeDescriptorSet(test)
    }
}

/**
 * The `preClean` task which does not depend on a source set name.
 */
private data class PreCleanTaskName(private val value: String = "preClean") : TaskName {
    override fun name(): String = value
}
