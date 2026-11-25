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

package io.spine.tools.core.jvm.routing.processor

import com.tschuchort.compiletesting.SourceFile

/**
 * The package of the entity classes declared in the tests.
 */
internal const val PACKAGE = "io.spine.given.devices"

/**
 * The code of the entity class that declares routes of all kinds.
 */
internal const val SCREEN = """
    class Screen : ProcessManager<DeviceId, Inspection, Inspection.Builder>() {
        companion object {
            @Route
            fun route(c: ReportStatus): DeviceId = c.device

            @Route
            fun route(e: StatusReported): DeviceId = e.device

            @Route
            fun route(s: Device): DeviceId = s.id
        }
    }
"""

/**
 * Creates a Kotlin source file with the given declarations.
 */
internal fun kotlinSource(
    fileName: String,
    declarations: String,
    packageName: String = PACKAGE
): SourceFile = kotlinFile(fileName, """
    package $packageName

    import io.spine.given.devices.Device
    import io.spine.given.devices.DeviceId
    import io.spine.given.devices.Inspection
    import io.spine.given.devices.commands.ReportStatus
    import io.spine.given.devices.events.StatusReported
    import io.spine.server.procman.ProcessManager
    import io.spine.server.route.Route

    $declarations
    """
)

/**
 * Creates a Java source file with the entity class `Screen` nested in the given class.
 */
internal fun javaSource(outerClass: String): SourceFile = javaFile(outerClass, """
    package $PACKAGE;

    import io.spine.given.devices.events.StatusReported;
    import io.spine.server.projection.Projection;
    import io.spine.server.route.Route;

    public class $outerClass {
        public static class Screen
                extends Projection<DeviceId, DeviceStatus, DeviceStatus.Builder> {
            @Route
            static DeviceId route(StatusReported e) {
                return e.getDevice();
            }
        }
    }
    """
)
