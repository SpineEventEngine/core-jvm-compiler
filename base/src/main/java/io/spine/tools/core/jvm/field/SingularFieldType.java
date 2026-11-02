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
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import io.spine.code.proto.FieldDeclaration;
import io.spine.tools.compiler.jvm.ClassName;
import io.spine.tools.compiler.jvm.JavaTypeName;

import static com.google.protobuf.Descriptors.FieldDescriptor.JavaType.STRING;
import static io.spine.tools.core.jvm.field.Accessor.prefix;
import static io.spine.tools.core.jvm.field.Accessor.prefixAndPostfix;
import static io.spine.tools.core.jvm.field.StandardAccessor.clear;
import static io.spine.tools.core.jvm.field.StandardAccessor.get;
import static io.spine.tools.core.jvm.field.StandardAccessor.set;

/**
 * Represents singular {@linkplain FieldType field type}.
 */
@Immutable
public final class SingularFieldType implements FieldType {

    private static final String BYTES = "Bytes";

    private static final ImmutableSet<Accessor> ACCESSORS = ImmutableSet.of(
            prefix("has"),
            get(),
            set(),
            clear()
    );

    private static final ImmutableSet<Accessor> STRING_ACCESSORS = ImmutableSet.of(
            prefixAndPostfix("get", BYTES),
            prefixAndPostfix("set", BYTES)
    );

    @SuppressWarnings("Immutable") // effectively
    private final JavaTypeName typeName;
    private final JavaType javaType;

    /**
     * Creates a new instance based on field type name.
     */
    SingularFieldType(FieldDeclaration declaration) {
        this.typeName = constructTypeNameFor(declaration.javaTypeName());
        this.javaType = declaration.javaType();
    }

    @Override
    public JavaTypeName name() {
        return typeName;
    }

    @Override
    public ImmutableSet<Accessor> accessors() {
        return javaType == STRING
             ? ImmutableSet.<Accessor>builder()
                           .addAll(ACCESSORS)
                           .addAll(STRING_ACCESSORS)
                           .build()
             : ACCESSORS;
    }

    /**
     * Returns "set" setter template used to initialize a singular field using a Protobuf message
     * builder.
     *
     * <p>The call should have the following structure: {@code builder.setFieldName(FieldType)}.
     */
    @Override
    public Accessor primarySetter() {
        return set();
    }

    /**
     * Obtains the name of the given Java type.
     *
     * @param name
     *         the name of a Java primitive type, such as {@code int}, or the canonical name
     *         of a class; the binary name of a nested class is also accepted
     * @return the name of the primitive type, or the name of the class
     */
    public static JavaTypeName constructTypeNameFor(String name) {
        var scalarPrimitive = PrimitiveType.typeNameFor(name);

        if (scalarPrimitive.isPresent()) {
            return scalarPrimitive.get();
        }

        // Make a possibly nested class name use the dot notation.
        var dottedName = name.replace('$', '.');
        var result = ClassName.Companion.guess(dottedName);
        return result;
    }
}
