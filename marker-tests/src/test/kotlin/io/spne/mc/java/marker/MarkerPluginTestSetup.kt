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

package io.spne.mc.java.marker

import com.google.protobuf.Empty
import io.spine.tools.mc.java.PluginTestSetup
import io.spine.tools.mc.java.marker.MarkerPlugin
import java.nio.file.Path
import org.junit.jupiter.api.io.TempDir

/**
 * Abstract base for [MarkerPlugin] tests.
 *
 * The plugin does not have settings.
 *
 * The class exposes properties common for tests based on proto types
 * generated in response to files under `testFixtures/proto/given/types`.
 */
internal abstract class MarkerPluginTestSetup : PluginTestSetup<Empty>(MarkerPlugin(), "") {

    /**
     * The directory of the Java package generated for proto types in `animals.proto` and
     * their siblings of the same "domain".
     */
    internal val animalDir = "io/spine/tools/mc/java/marker/given/animal"

    /**
     * The package corresponding to [animalDir].
     */
    internal val animalPackage = animalDir.replace('/', '.')

    override fun createSettings(projectDir: Path): Empty = Empty.getDefaultInstance()

    fun generateCode(@TempDir projectDir: Path) {
        runPipeline(projectDir)
    }
}
