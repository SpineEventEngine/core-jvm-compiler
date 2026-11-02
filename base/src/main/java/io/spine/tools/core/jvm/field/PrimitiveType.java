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

import io.spine.code.proto.ScalarType;
import io.spine.tools.compiler.jvm.JavaTypeName;

import java.util.Optional;

import static io.spine.util.Preconditions2.checkNotEmptyOrBlank;

/**
 * Enumeration of the Java primitives used for representing Proto scalar types.
 */
enum PrimitiveType {
    INT(int.class, Integer.class),
    LONG(long.class, Long.class),
    FLOAT(float.class, Float.class),
    DOUBLE(double.class, Double.class),
    BOOLEAN(boolean.class, Boolean.class);

    private final String name;
    private final Class<?> wrapperClass;

    PrimitiveType(Class<?> primitiveType, Class<?> wrapperClass) {
        this.name = primitiveType.getSimpleName();
        this.wrapperClass = wrapperClass;
    }

    /**
     * Returns the boxed {@link Class} for the Protobuf scalar primitive name.
     *
     * @param primitiveType
     *         the primitive type name
     * @return the wrapper class or {@link Optional#empty() Optional.empty()}
     *         if the specified primitive name does not belong to {@link ScalarType}.
     */
    static Optional<? extends Class<?>> wrapperFor(String primitiveType) {
        checkNotEmptyOrBlank(primitiveType);
        for (var simpleType : values()) {
            if (simpleType.matchesName(primitiveType)) {
                return Optional.of(simpleType.wrapperClass);
            }
        }

        return Optional.empty();
    }

    /**
     * Returns the {@link JavaTypeName} of the Java primitive with the given name.
     *
     * @param primitiveType
     *         the primitive type name
     * @return the type name of the Java primitive or {@link Optional#empty() Optional.empty()}
     *         if the specified primitive name does not belong to {@link ScalarType}.
     */
    static Optional<JavaTypeName> typeNameFor(String primitiveType) {
        // Only the primitives representing Protobuf scalar types are looked up.
        if (wrapperFor(primitiveType).isEmpty()) {
            return Optional.empty();
        }
        return JavaTypeName.Companion.getKnownPrimitives()
                                     .stream()
                                     .filter(type -> type.getCanonical().equals(primitiveType))
                                     .findFirst();
    }

    private boolean matchesName(String typeName) {
        return name.equals(typeName);
    }
}
