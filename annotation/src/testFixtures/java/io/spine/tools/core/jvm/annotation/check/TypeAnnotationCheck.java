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

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.tools.core.jvm.annotation.check.Annotations.findAnnotation;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tells if a specified Java annotation present in the generated code.
 */
public final class TypeAnnotationCheck extends SourceCheck {

    private final Class<? extends Annotation> annotation;

    public TypeAnnotationCheck(Class<? extends Annotation> annotation,
                               boolean shouldBeAnnotated) {
        super(annotation, shouldBeAnnotated);
        this.annotation = checkNotNull(annotation);
    }

    @Override
    public void accept(PsiClass source) {
        checkNotNull(source);
        Optional<?> annotation = findAnnotation(source, this.annotation);
        var sourceName = source.getQualifiedName();
        var annotationName = this.annotation.getName();
        if (shouldBeAnnotated()) {
            assertTrue(
                    annotation.isPresent(),
                    format("`%s` should be annotated with `%s`.", sourceName, annotationName)
            );
        } else {
            assertFalse(
                    annotation.isPresent(),
                    format("`%s` should NOT be annotated with `%s`.", sourceName, annotationName)
            );
        }
    }
}
