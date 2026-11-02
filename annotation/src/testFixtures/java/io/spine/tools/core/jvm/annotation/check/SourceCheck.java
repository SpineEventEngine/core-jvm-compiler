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
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Abstract base for checking annotations in the generated source code.
 */
public abstract class SourceCheck implements Consumer<PsiClass> {

    private final Class<? extends Annotation> annotationClass;
    private final boolean shouldBeAnnotated;

    SourceCheck(Class<? extends Annotation> annotationClass, boolean shouldBeAnnotated) {
        this.annotationClass = checkNotNull(annotationClass);
        this.shouldBeAnnotated = shouldBeAnnotated;
    }

    protected Class<? extends Annotation> annotationClass() {
        return annotationClass;
    }

    boolean shouldBeAnnotated() {
        return shouldBeAnnotated;
    }
}
