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
import io.spine.tools.compiler.jvm.ClassName;
import io.spine.tools.compiler.jvm.JavaTypeName;
import io.spine.tools.compiler.jvm.ParameterizedTypeName;

import java.util.List;

import static io.spine.tools.core.jvm.field.StandardAccessor.add;
import static io.spine.tools.core.jvm.field.StandardAccessor.addAll;
import static io.spine.tools.core.jvm.field.StandardAccessor.clear;
import static io.spine.tools.core.jvm.field.StandardAccessor.get;
import static io.spine.tools.core.jvm.field.StandardAccessor.getCount;
import static io.spine.tools.core.jvm.field.StandardAccessor.getList;
import static io.spine.tools.core.jvm.field.StandardAccessor.set;

/**
 * Represents repeated {@linkplain FieldType field type}.
 */
@Immutable
public final class RepeatedFieldType implements FieldType {

    private static final ImmutableSet<Accessor> ACCESSORS = ImmutableSet.of(
            get(),
            getList(),
            getCount(),
            set(),
            add(),
            addAll(),
            clear()
    );

    @SuppressWarnings("Immutable") // effectively
    private final JavaTypeName typeName;

    /**
     * Constructs a new instance based on component type.
     *
     * @param declaration
     *         the declaration of the field
     */
    RepeatedFieldType(FieldDeclaration declaration) {
        this.typeName = typeNameFor(declaration.javaTypeName());
    }

    @Override
    public JavaTypeName name() {
        return typeName;
    }

    @Override
    public ImmutableSet<Accessor> accessors() {
        return ACCESSORS;
    }

    /**
     * Returns "addAll" setter prefix, used to initialize a repeated field using with a call to
     * Protobuf message builder.
     */
    @Override
    public Accessor primarySetter() {
        return addAll();
    }

    /**
     * Obtains the name of the {@code List} type with the given element type.
     *
     * <p>A primitive element type is replaced with its wrapper class.
     *
     * @param componentTypeName
     *         the name of a Java primitive type, such as {@code int}, or the canonical name
     *         of a class; the binary name of a nested class is also accepted
     */
    public static JavaTypeName typeNameFor(String componentTypeName) {
        var wrapper = PrimitiveType.wrapperFor(componentTypeName);
        var componentType = wrapper.isPresent()
                            ? new ClassName(wrapper.get())
                            : ClassName.Companion.guess(componentTypeName.replace('$', '.'));
        var listClass = new ClassName(List.class);
        var result = new ParameterizedTypeName(listClass, componentType);
        return result;
    }

    @Override
    public String toString() {
        return typeName.toString();
    }
}
