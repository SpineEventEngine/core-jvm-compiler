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

import io.spine.dependency.local.CoreJvmCompiler
import io.spine.dependency.local.Compiler
import io.spine.dependency.local.ToolBase

/*
    This Smoke Tests module holds a `TestMethodFactory` that is used in
    the `model-compiler` module.
*/

dependencies {
    implementation(ToolBase.code)
    implementation(ToolBase.javaCode)
    implementation(Compiler.api)!!.because("Custom codegen actions use the Compiler API.")
    implementation(Compiler.jvm)
    val coreJvmBundle = CoreJvmCompiler.gradlePlugin(version.toString())
    implementation(coreJvmBundle)!!.because(
        "We take abstract base classes from the CoreJvm Compiler Base module."
    )
}
