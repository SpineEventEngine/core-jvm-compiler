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

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.spine.tools.core.jvm.GeneratedAnnotation
import io.spine.tools.core.jvm.signal.SignalPluginTestSetup
import java.nio.file.Path
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.readText
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Tests the code of the rejection throwables generated from
 * `given/signals/rejections.proto`.
 *
 * Unlike the IgTests of the `signal` module, the suite runs the plugin in the test JVM.
 */
@DisplayName("`RThrowableRenderer` should")
internal class RThrowableRendererSpec {

    companion object : SignalPluginTestSetup(RThrowablePlugin()) {

        /**
         * The directory of the throwables, as defined by the `java_package` option.
         */
        private const val PACKAGE_DIR = "$JAVA_SRC_DIR/rejection"

        /**
         * The directory with the generated throwables.
         */
        private lateinit var packageDir: Path

        @BeforeAll
        @JvmStatic
        fun generateRejections(@TempDir projectDir: Path) {
            runPipeline(projectDir)
            packageDir = sourceFileSet.outputRoot.resolve(PACKAGE_DIR)
        }

        /**
         * Reads the code of the throwable class generated for the given rejection.
         *
         * The renderer writes the files directly, so they are read from the disk.
         * The line separators are converted because the generated Javadoc uses
         * the separators of the platform.
         */
        private fun code(rejection: String): String =
            packageDir.resolve("$rejection.java").readText().replace("\r\n", "\n")
    }

    @Test
    fun `generate a throwable for each top-level rejection`() {
        val generated = packageDir.listDirectoryEntries("*.java")
            .map { it.nameWithoutExtension }
            .filter { it != "Rejections" } // The outer class of the rejection messages.
        generated shouldContainExactlyInAnyOrder listOf(
            "CannotStartScanning",
            "TelescopeIsOffline",
            "ObservationRejected",
            "Rejection3",
            "Rejection4",
            "RejectionWithRepeatedField",
            "RejectionWithMapField"
        )
    }

    /**
     * Compares the whole code of a throwable with the expected one.
     *
     * The expected code is the code which JavaPoet generated for the same rejection in
     * CoreJvm Compiler `2.0.0-SNAPSHOT.103`, with the package and the outer class adapted.
     * The code is formatted by the `JavaCodeStyleFormatterPlugin` of the Compiler, so
     * a change in that formatting, like the wrapping of the `builder` field, fails the test
     * even if the generator does not change. Update the expected code only after checking
     * that the change of the generated code is intended.
     *
     * The blank line before the closing quotes stands for the line separator ending the file.
     */
    @Test
    fun `generate the throwable with its builder for a rejection with a map field`() {
        code("RejectionWithMapField") shouldBe """
            package io.spine.tools.core.signal.given.rejection;

            import io.spine.annotation.Generated;
            import io.spine.base.RejectionThrowable;
            import io.spine.validation.Validated;
            import java.util.Map;

            /**
             * <p>The rejection message proto type is {@code given.signals.RejectionWithMapField}.
             */
            @Generated("${GeneratedAnnotation.defaultValue}")
            public class RejectionWithMapField extends RejectionThrowable {
                private static final long serialVersionUID = 0L;

                /**
                 * Creates a new instance.
                 *
                 * @param builder the builder for the rejection
                 */
                private RejectionWithMapField(Builder builder) {
                    super(builder.rejectionMessage());
                }

                @Override
                public Rejections.RejectionWithMapField messageThrown() {
                    return (Rejections.RejectionWithMapField) super.messageThrown();
                }

                /**
                 * Creates a new builder for the rejection.
                 */
                public static Builder newBuilder() {
                    return new Builder();
                }

                /**
                 * The builder for the {@code RejectionWithMapField} rejection.
                 */
                public static class Builder {
                    private final Rejections.RejectionWithMapField.Builder builder =
                            Rejections.RejectionWithMapField.newBuilder();

                    /**
                     * Prevent direct instantiation of the builder.
                     */
                    private Builder() {}

                    public Builder putAllMap(Map<Integer, Integer> map) {
                        builder.putAllMap(map);
                        return this;
                    }

                    /**
                     * Obtains the rejection message.
                     */
                    private Rejections.@Validated RejectionWithMapField rejectionMessage() {
                        return builder.build();
                    }

                    /**
                     * Creates the rejection from the builder and validates it.
                     */
                    public RejectionWithMapField build() {
                        return new RejectionWithMapField(this);
                    }
                }
            }

            """.trimIndent()
    }

    @Test
    fun `import the types of fields declared in other packages`() {
        val code = code("CannotStartScanning")
        code shouldContain "import io.spine.tools.core.jvm.signal.given.Star;"
        code shouldContain "import io.spine.tools.core.jvm.signal.given.TelescopeId;"
        code shouldContain "public Builder setStar(Star star) {"
        code shouldContain "public Builder setTelescope(TelescopeId telescope) {"
    }

    @Test
    fun `copy the leading comment of a rejection to its Javadoc`() {
        code("CannotStartScanning") shouldContain """
            /**
             * <pre>
             * A telescope is unable to start scanning the surrounding of the given star.
             * </pre>
             *
             * <p>The rejection message proto type is {@code given.signals.CannotStartScanning}.
             */
            """.trimIndent()
    }

    @Test
    fun `refer to nested types relative to the outer class of the rejections`() {
        val code = code("Rejection4")
        code shouldContain "import com.google.protobuf.Any;"
        code shouldContain
                "public Builder setInnerId(Rejections.Rejection4.Rejection4Inner innerId) {"
        code shouldContain
                "public Builder setInnerEnumId(" +
                "Rejections.Rejection4.Rejection4InnerEnum innerEnumId) {"
    }

    @Test
    fun `accept boxed values of primitive fields`() {
        code("Rejection4") shouldContain "public Builder setBooleanId(Boolean booleanId) {"
        code("RejectionWithRepeatedField") shouldContain
                "public Builder addAllValues(List<Integer> values) {"
    }
}
