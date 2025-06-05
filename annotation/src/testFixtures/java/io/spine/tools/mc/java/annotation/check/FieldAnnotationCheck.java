/*
 * Copyright 2022, TeamDev. All rights reserved.
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

package io.spine.tools.mc.java.annotation.check;

import com.google.protobuf.Descriptors.FieldDescriptor;
import io.spine.annotation.Internal;
import io.spine.code.proto.FieldName;
import org.jboss.forge.roaster.model.TypeHolder;
import org.jboss.forge.roaster.model.VisibilityScoped;
import org.jboss.forge.roaster.model.impl.AbstractJavaSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaSource;
import org.jboss.forge.roaster.model.source.MethodSource;

import java.lang.annotation.Annotation;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.code.java.SimpleClassName.ofBuilder;
import static io.spine.tools.mc.java.annotation.check.Annotations.findAnnotation;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that a proto field marked with an option gets corresponding annotations
 * in the accessor methods in the generated Java code.
 */
public final class FieldAnnotationCheck extends SourceCheck {

    private final FieldDescriptor field;

    public FieldAnnotationCheck(FieldDescriptor field,
                                Class<? extends Annotation> annotationClass,
                                boolean shouldBeAnnotated) {
        super(annotationClass, shouldBeAnnotated);
        this.field = checkNotNull(field);
    }

    private static JavaClassSource builderOf(JavaSource<?> messageSource) {
        var messageType = (TypeHolder<?>) messageSource;
        var builderType = messageType.getNestedType(ofBuilder().value());
        return (JavaClassSource) builderType;
    }

    @Override
    public void accept(AbstractJavaSource<JavaClassSource> input) {
        checkNotNull(input);
        var message = (JavaClassSource) input;
        var messageBuilder = builderOf(message);
        checkAccessorsAnnotation(message);
        checkAccessorsAnnotation(messageBuilder);
    }

    private void checkAccessorsAnnotation(JavaClassSource message) {
        var fieldNameCamelCase = FieldName.of(field.toProto()).toCamelCase();
        message.getMethods()
               .stream()
               .filter(VisibilityScoped::isPublic)
               .filter(method -> method.getName().contains(fieldNameCamelCase))
               .forEach(this::assertMethodAnnotation);
    }

    private void assertMethodAnnotation(MethodSource<JavaClassSource> method) {
        Optional<?> annotation = findAnnotation(method, annotationClass());
        var methodName = method.getName();
        if (shouldBeAnnotated()) {
            assertTrue(annotation.isPresent(), msg(true, methodName));
        } else {
            assertFalse(annotation.isPresent(), msg(false, methodName));
        }
    }

    private String msg(boolean expected, String methodName) {
        var annotationClass = Internal.class.getSimpleName();
        var fullFieldName = field.getFullName();
        return format(
                "The method `%s()` generated for the field `%s` is expected to be" +
                        "%s" +
                        "annotated `@%s`.",
                methodName, fullFieldName,
                expected ? " " : " NOT ",
                annotationClass
        );
    }
}
