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

package io.spine.tools.core.jvm.field;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.Immutable;
import io.spine.code.proto.FieldDeclaration;
import io.spine.tools.compiler.jvm.JavaTypeName;

/**
 * Field type information for the needs of code generation.
 */
@Immutable
public interface FieldType {

    /**
     * Obtains the {@link JavaTypeName} for the field.
     *
     * @return the type name
     */
    JavaTypeName name();

    /**
     * Obtains the setter accessor for the field.
     */
    Accessor primarySetter();

    /**
     * Obtains the accessors for a field of this type.
     */
    ImmutableSet<Accessor> accessors();

    /**
     * Creates an instance for the passed field declaration.
     */
    static FieldType of(FieldDeclaration field) {
        if (field.isMap()) {
            return new MapFieldType(field);
        } else if (field.isRepeated()) {
            return new RepeatedFieldType(field);
        } else {
            return new SingularFieldType(field);
        }
    }
}
