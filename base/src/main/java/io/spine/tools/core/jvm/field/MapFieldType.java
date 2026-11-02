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
import com.google.protobuf.Descriptors.FieldDescriptor;
import io.spine.code.proto.FieldDeclaration;
import io.spine.tools.compiler.jvm.ClassName;
import io.spine.tools.compiler.jvm.JavaTypeName;
import io.spine.tools.compiler.jvm.ParameterizedTypeName;

import java.util.AbstractMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static io.spine.tools.core.jvm.field.Accessor.prefix;
import static io.spine.tools.core.jvm.field.Accessor.prefixAndPostfix;
import static io.spine.tools.core.jvm.field.StandardAccessor.clear;
import static io.spine.tools.core.jvm.field.StandardAccessor.get;
import static io.spine.tools.core.jvm.field.StandardAccessor.getCount;
import static io.spine.tools.core.jvm.field.StandardAccessor.getMap;
import static io.spine.tools.core.jvm.field.StandardAccessor.put;
import static io.spine.tools.core.jvm.field.StandardAccessor.putAll;
import static io.spine.tools.core.jvm.field.StandardAccessor.remove;

/**
 * Represents map {@linkplain FieldType field type}.
 */
@Immutable
final class MapFieldType implements FieldType {

    private static final String GET = "get";

    private static final ImmutableSet<Accessor> ACCESSORS = ImmutableSet.of(
            get(),
            getCount(),
            getMap(),
            prefixAndPostfix(GET, "OrDefault"),
            prefixAndPostfix(GET, "OrThrow"),
            prefix("contains"),
            clear(),
            put(),
            remove(),
            putAll()
    );

    @SuppressWarnings("Immutable") // effectively
    private final JavaTypeName typeName;

    /**
     * Constructs the new instance based on the key and the value type names.
     */
    MapFieldType(FieldDeclaration field) {
        var entryTypeNames = entryTypeNames(field);
        var keyType = boxIfPrimitive(entryTypeNames.getKey());
        var valueType = boxIfPrimitive(entryTypeNames.getValue());
        this.typeName = new ParameterizedTypeName(new ClassName(Map.class), keyType, valueType);
    }

    @Override
    public JavaTypeName name() {
        return typeName;
    }

    /**
     * Returns "putAll" setter prefix,
     * used to initialize a map field using a protobuf message builder.
     *
     * <p>Call should be like `builder.putAllFieldName({@link Map})`.
     *
     * <p>{@inheritDoc}
     */
    @Override
    public Accessor primarySetter() {
        return putAll();
    }

    @Override
    public ImmutableSet<Accessor> accessors() {
        return ACCESSORS;
    }

    private static JavaTypeName boxIfPrimitive(JavaTypeName typeName) {
        var wrapper = PrimitiveType.wrapperFor(typeName.getCanonical());
        if (wrapper.isPresent()) {
            return new ClassName(wrapper.get());
        }
        return typeName;
    }

    @Override
    public String toString() {
        return typeName.toString();
    }

    /**
     * Returns the key and the value type names for the map field
     * based on the passed nested types.
     */
    private static Map.Entry<JavaTypeName, JavaTypeName> entryTypeNames(FieldDeclaration mapField) {
        checkArgument(mapField.isMap());
        var keyFieldIndex = 0;
        var valueFieldIndex = 1;
        var mapEntry = mapField.descriptor().getMessageType();
        var fields = mapEntry.getFields();
        var keyField = fields.get(keyFieldIndex);
        var valueField = fields.get(valueFieldIndex);
        var keyTypeName = typeNameOf(keyField);
        var valueTypeName = typeNameOf(valueField);
        return new AbstractMap.SimpleEntry<>(keyTypeName, valueTypeName);
    }

    private static JavaTypeName typeNameOf(FieldDescriptor descr) {
        var decl = new FieldDeclaration(descr);
        return FieldType.of(decl).name();
    }
}
