package io.github.spring.middleware.graphql.gateway.scalars;

import graphql.language.ArrayValue;
import graphql.language.BooleanValue;
import graphql.language.FloatValue;
import graphql.language.IntValue;
import graphql.language.NullValue;
import graphql.language.ObjectField;
import graphql.language.ObjectValue;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;

import java.util.LinkedHashMap;
import java.util.Map;

public class MapStringObjectScalar {

    public static final GraphQLScalarType INSTANCE = GraphQLScalarType.newScalar()
            .name("Map_String_ObjectScalar")
            .description("Built-in scalar for map-like structures (Map<String,Object>)")
            .coercing(new Coercing<Map<String, Object>, Object>() {

                @Override
                public Object serialize(Object dataFetcherResult) {
                    if (dataFetcherResult instanceof Map<?, ?> map) {
                        return toStringObjectMap(map);
                    }
                    throw new CoercingSerializeException(
                            STR."Expected Map<String,Object> but was: \{dataFetcherResult.getClass()}"
                    );
                }

                @Override
                public Map<String, Object> parseValue(Object input) {
                    if (input instanceof Map<?, ?> map) {
                        return toStringObjectMap(map);
                    }
                    throw new CoercingParseValueException(
                            STR."Expected Map<String,Object> input but was: \{input.getClass()}"
                    );
                }

                @Override
                public Map<String, Object> parseLiteral(Object input) {
                    if (input instanceof ObjectValue objectValue) {
                        Map<String, Object> result = new LinkedHashMap<>();

                        for (ObjectField field : objectValue.getObjectFields()) {
                            result.put(field.getName(), parseLiteralValue(field.getValue()));
                        }

                        return result;
                    }

                    throw new CoercingParseLiteralException(
                            STR."Expected AST ObjectValue but was: \{input.getClass()}"
                    );
                }

                private Map<String, Object> toStringObjectMap(Map<?, ?> map) {
                    Map<String, Object> result = new LinkedHashMap<>();
                    map.forEach((k, v) -> result.put(String.valueOf(k), v));
                    return result;
                }

                private Object parseLiteralValue(Value<?> value) {
                    if (value instanceof StringValue sv) {
                        return sv.getValue();
                    }
                    if (value instanceof IntValue iv) {
                        return iv.getValue();
                    }
                    if (value instanceof FloatValue fv) {
                        return fv.getValue();
                    }
                    if (value instanceof BooleanValue bv) {
                        return bv.isValue();
                    }
                    if (value instanceof NullValue) {
                        return null;
                    }
                    if (value instanceof ObjectValue ov) {
                        Map<String, Object> map = new LinkedHashMap<>();
                        for (ObjectField field : ov.getObjectFields()) {
                            map.put(field.getName(), parseLiteralValue(field.getValue()));
                        }
                        return map;
                    }
                    if (value instanceof ArrayValue av) {
                        return av.getValues().stream()
                                .map(this::parseLiteralValue)
                                .toList();
                    }

                    throw new CoercingParseLiteralException(
                            "Unsupported literal type: " + value.getClass()
                    );
                }
            })
            .build();
}