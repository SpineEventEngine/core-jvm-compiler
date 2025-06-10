/*
 * Copyright 2024, TeamDev. All rights reserved.
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

import io.spine.annotation.Beta
import io.spine.annotation.Experimental
import io.spine.annotation.Internal
import io.spine.annotation.SPI
import org.gradle.api.Action
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested

/**
 * Settings for annotations exposed by McJava in a Gradle project.
 *
 * @see McJavaOptions
 */
public abstract class AnnotationSettings {

    @get:Nested
    public abstract val types: AnnotationTypeSettings

    /**
     * The pattern for the names of the classes that to be annotated
     * as [internal][AnnotationTypeSettings.internal].
     */
    public abstract val internalClassPatterns: ListProperty<String>

    /**
     * The pattern for the method names to be annotated
     * as [internal][AnnotationTypeSettings.internal].
     */
    public abstract val internalMethodNames: ListProperty<String>

    /**
     * The action to customize [types].
     */
    public fun types(action: Action<AnnotationTypeSettings>) {
        action.execute(types)
    }
}

/**
 * Type names of Java annotation types used to mark generated code.
 */
@Suppress("LeakingThis") // as advised by Gradle API.
public abstract class AnnotationTypeSettings {

    /**
     * A fully qualified name of a Java annotation type to be used for annotating
     * experimental code elements.
     *
     * The default value is the name of [Experimental] annotation type.
     */
    public abstract val experimental: Property<String>

    /**
     * A fully qualified name of a Java annotation type to be used for annotating
     * beta code elements.
     *
     * The default value is the name of [Beta] annotation type.
     */
    public abstract val beta: Property<String>

    /**
     * A fully qualified name of a Java annotation type to be used for annotating
     * SPI code elements.
     *
     * The default value is the name [SPI] annotation type.
     */
    public abstract val spi: Property<String>

    /**
     * A fully qualified name of a Java annotation type to be used for annotating
     * internal code elements.
     *
     * The default value is the name [Internal] annotation type.
     */
    public abstract val internal: Property<String>

    init {
        experimental.convention<Experimental>()
        beta.convention<Beta>()
        spi.convention<SPI>()
        internal.convention<Internal>()
    }

    private inline fun <reified T: Any> Property<String>.convention() {
        convention(T::class.java.canonicalName)
    }
}
