/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.protocols.json.internal.marshall;

import static software.amazon.awssdk.utils.CollectionUtils.isNullOrEmpty;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.traits.JsonValueTrait;
import software.amazon.awssdk.core.traits.ListTrait;
import software.amazon.awssdk.protocols.core.ValueToStringConverter;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.StringUtils;

@SdkInternalApi
public final class HeaderMarshaller {

    public static final JsonMarshaller<String> STRING = new SimpleHeaderMarshaller<>(
        (val, field) -> field.containsTrait(JsonValueTrait.class) ?
                        BinaryUtils.toBase64(val.getBytes(StandardCharsets.UTF_8)) : val);

    public static final JsonMarshaller<Integer> INTEGER = new SimpleHeaderMarshaller<>(ValueToStringConverter.FROM_INTEGER);

    public static final JsonMarshaller<Long> LONG = new SimpleHeaderMarshaller<>(ValueToStringConverter.FROM_LONG);

    public static final JsonMarshaller<Short> SHORT = new SimpleHeaderMarshaller<>(ValueToStringConverter.FROM_SHORT);

    public static final JsonMarshaller<Double> DOUBLE = new SimpleHeaderMarshaller<>(ValueToStringConverter.FROM_DOUBLE);

    public static final JsonMarshaller<Float> FLOAT = new SimpleHeaderMarshaller<>(ValueToStringConverter.FROM_FLOAT);

    public static final JsonMarshaller<Boolean> BOOLEAN = new SimpleHeaderMarshaller<>(ValueToStringConverter.FROM_BOOLEAN);

    public static final JsonMarshaller<Instant> INSTANT
        = new SimpleHeaderMarshaller<>(JsonProtocolMarshaller.INSTANT_VALUE_TO_STRING);

    public static final JsonMarshaller<List<?>> LIST = (list, context, paramName, sdkField) -> {
        // Null or empty lists cannot be meaningfully (or safely) represented in an HTTP header message since header-fields must
        // typically have a non-empty field-value. https://datatracker.ietf.org/doc/html/rfc7230#section-3.2
        if (isNullOrEmpty(list)) {
            return;
        }
        SdkField memberFieldInfo = sdkField.getRequiredTrait(ListTrait.class).memberFieldInfo();
        for (Object listValue : list) {
            if (shouldSkipElement(listValue)) {
                continue;
            }
            JsonMarshaller marshaller = context.marshallerRegistry().getMarshaller(MarshallLocation.HEADER, listValue);
            marshaller.marshall(listValue, context, paramName, memberFieldInfo);
        }
    };

    private HeaderMarshaller() {
    }

    private static boolean shouldSkipElement(Object element) {
        return element instanceof String && StringUtils.isBlank((String) element);
    }

    private static class SimpleHeaderMarshaller<T> implements JsonMarshaller<T> {

        private final ValueToStringConverter.ValueToString<T> converter;

        private SimpleHeaderMarshaller(ValueToStringConverter.ValueToString<T> converter) {
            this.converter = converter;
        }

        @Override
        public void marshall(T val, JsonMarshallerContext context, String paramName, SdkField<T> sdkField) {
            context.request().appendHeader(paramName, converter.convert(val, sdkField));
        }
    }
}
