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

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiModifierListOwner;

import java.lang.annotation.Annotation;
import java.util.Optional;

import static java.util.Arrays.stream;

/**
 * Utilities for finding annotations in the generated code.
 */
final class Annotations {

    /** Prevents instantiation of this utility class. */
    private Annotations() {
    }

    /**
     * Finds an annotation of the given type on the given element.
     *
     * <p>The generated code is parsed without a classpath, so the references
     * in it are not resolved. Therefore, an annotation matches if its name,
     * as written in the code, is either the qualified or the simple name of
     * the given type.
     */
    static Optional<PsiAnnotation>
    findAnnotation(PsiModifierListOwner element, Class<? extends Annotation> annotationType) {
        var qualifiedName = annotationType.getCanonicalName();
        var simpleName = annotationType.getSimpleName();
        return stream(element.getAnnotations())
                .filter(annotation -> {
                    var name = annotation.getQualifiedName();
                    return qualifiedName.equals(name) || simpleName.equals(name);
                })
                .findFirst();
    }
}
