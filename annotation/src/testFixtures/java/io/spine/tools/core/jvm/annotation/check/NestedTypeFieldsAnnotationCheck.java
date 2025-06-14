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

package io.spine.tools.core.jvm.annotation.check;

import org.jboss.forge.roaster.model.impl.AbstractJavaSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;

import java.lang.annotation.Annotation;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.protobuf.Descriptors.Descriptor;

/**
 * Checks that fields of a nested type are annotated.
 */
public final class NestedTypeFieldsAnnotationCheck extends SourceCheck {

    private final Descriptor descriptor;

    public NestedTypeFieldsAnnotationCheck(Descriptor descriptor,
                                           Class<? extends Annotation> annotationClass,
                                           boolean shouldBeAnnotated) {
        super(annotationClass, shouldBeAnnotated);
        this.descriptor = checkNotNull(descriptor);
    }

    @Override
    @SuppressWarnings("unchecked") // Could not determine an exact type for nested declaration.
    public void accept(AbstractJavaSource<JavaClassSource> outerClass) {
        checkNotNull(outerClass);
        var annotationClass = annotationClass();
        var shouldBeAnnotated = shouldBeAnnotated();
        for (var fieldDescriptor : descriptor.getFields()) {
            var nestedType = (AbstractJavaSource<JavaClassSource>)
                    outerClass.getNestedType(descriptor.getName());
            var check = new FieldAnnotationCheck(
                    fieldDescriptor,
                    annotationClass,
                    shouldBeAnnotated
            );
            check.accept(nestedType);
        }
    }
}
