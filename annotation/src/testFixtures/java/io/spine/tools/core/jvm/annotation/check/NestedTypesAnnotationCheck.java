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

import com.intellij.psi.PsiClass;

import java.lang.annotation.Annotation;
import java.util.Optional;

import static io.spine.tools.core.jvm.annotation.check.Annotations.findAnnotation;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that a nested type is annotated.
 */
public final class NestedTypesAnnotationCheck extends SourceCheck {

    public NestedTypesAnnotationCheck(
            Class<? extends Annotation> expectedAnnotation,
            boolean shouldBeAnnotated
    ) {
        super(expectedAnnotation, shouldBeAnnotated);
    }

    @Override
    public void accept(PsiClass outerClass) {
        requireNonNull(outerClass);
        for (var nestedType : outerClass.getInnerClasses()) {
            Optional<?> annotation = findAnnotation(nestedType, annotationClass());
            var qualifiedName = nestedType.getQualifiedName();
            var annotated = annotation.isPresent();
            if (shouldBeAnnotated()) {
                assertTrue(annotated,
                           () -> format("`%s` is not annotated but should be.", qualifiedName));
            } else {
                assertFalse(annotated,
                            () -> format("`%s` is annotated but should not be.", qualifiedName));
            }
        }
    }
}
