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

package io.spine.tools.core.jvm.annotation.check;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import io.spine.annotation.Internal;
import io.spine.code.proto.FieldName;

import java.lang.annotation.Annotation;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.code.java.SimpleClassName.ofBuilder;
import static io.spine.tools.core.jvm.annotation.check.Annotations.findAnnotation;
import static java.lang.String.format;
import static java.util.Arrays.stream;
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

    private static PsiClass builderOf(PsiClass messageClass) {
        var builderName = ofBuilder().value();
        return checkNotNull(
                messageClass.findInnerClassByName(builderName, false),
                "The class `%s` has no nested class `%s`.",
                messageClass.getQualifiedName(), builderName
        );
    }

    @Override
    public void accept(PsiClass message) {
        checkNotNull(message);
        var messageBuilder = builderOf(message);
        checkAccessorsAnnotation(message);
        checkAccessorsAnnotation(messageBuilder);
    }

    private void checkAccessorsAnnotation(PsiClass cls) {
        var fieldNameCamelCase = FieldName.of(field.toProto()).toCamelCase();
        stream(cls.getMethods())
                .filter(method -> method.hasModifierProperty(PsiModifier.PUBLIC))
                .filter(method -> method.getName().contains(fieldNameCamelCase))
                .forEach(this::assertMethodAnnotation);
    }

    private void assertMethodAnnotation(PsiMethod method) {
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
