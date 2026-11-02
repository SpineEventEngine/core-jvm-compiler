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
    public void accept(PsiClass outerClass) {
        checkNotNull(outerClass);
        var annotationClass = annotationClass();
        var shouldBeAnnotated = shouldBeAnnotated();
        var nestedTypeName = descriptor.getName();
        var nestedType = checkNotNull(
                outerClass.findInnerClassByName(nestedTypeName, false),
                "The class `%s` has no nested class `%s`.",
                outerClass.getQualifiedName(), nestedTypeName
        );
        for (var fieldDescriptor : descriptor.getFields()) {
            var check = new FieldAnnotationCheck(
                    fieldDescriptor,
                    annotationClass,
                    shouldBeAnnotated
            );
            check.accept(nestedType);
        }
    }
}
