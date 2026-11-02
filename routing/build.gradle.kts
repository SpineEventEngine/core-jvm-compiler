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

import io.spine.dependency.lib.KotlinPoet
import io.spine.dependency.local.Base
import io.spine.dependency.local.CoreJvm
import io.spine.dependency.local.Logging
import io.spine.dependency.local.ToolBase
import io.spine.dependency.test.Kotest
import io.spine.dependency.test.KotlinCompileTesting

plugins {
    module
    id("io.spine.artifact-meta")
    id("io.spine.core-jvm")
}

/**
 * The ID used for publishing this module.
 */
val moduleArtifactId = "core-jvm-routing"

artifactMeta {
    artifactId.set(moduleArtifactId)
    excludeConfigurations {
        containing(*buildToolConfigurations)
    }
}

dependencies {
    api(project(":ksp"))

    implementation(Base.annotations)
    implementation(ToolBase.jvmTools)
    implementation(KotlinPoet.ksp)
    implementation(CoreJvm.server)
    implementation(project(":base"))

    testImplementation(gradleTestKit())
    testImplementation(Kotest.assertions)
    testImplementation(KotlinCompileTesting.libKsp)
    testImplementation(Logging.testLib)
}

if (JavaVersion.current() >= JavaVersion.VERSION_16) {
    tasks.withType<Test>().configureEach {
        jvmArgs(
            "--add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
            "--add-opens=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
            "--add-opens=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED",
            "--add-opens=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
            "--add-opens=jdk.compiler/com.sun.tools.javac.jvm=ALL-UNNAMED",
            "--add-opens=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED",
            "--add-opens=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
            "--add-opens=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED",
            "--add-opens=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
            "--add-opens=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
        )
    }
}
