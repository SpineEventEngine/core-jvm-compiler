/*
 * Copyright 2025, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.spine.tools.core.jvm.field;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.Immutable;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import io.spine.code.proto.FieldDeclaration;

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
    private final TypeName typeName;

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
    public TypeName name() {
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

    public static TypeName typeNameFor(String componentTypeName) {
        var wrapper = PrimitiveType.wrapperFor(componentTypeName);
        var componentType = wrapper.isPresent()
                            ? TypeName.get(wrapper.get())
                            : ClassName.bestGuess(componentTypeName);
        var listClass = ClassName.get(List.class);
        var result = ParameterizedTypeName.get(listClass, componentType);
        return result;
    }

    @Override
    public String toString() {
        return typeName.toString();
    }
}
