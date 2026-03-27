package io.github.spring.middleware.graphql.gateway.fetcher.builder;

import graphql.language.Argument;
import graphql.language.BooleanValue;
import graphql.language.EnumValue;
import graphql.language.Field;
import graphql.language.FloatValue;
import graphql.language.IntValue;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLScalarType;
import io.github.spring.middleware.graphql.gateway.exception.GraphQLErrorCodes;
import io.github.spring.middleware.graphql.gateway.exception.GraphQLException;
import io.github.spring.middleware.graphql.gateway.fetcher.GraphQLVariableDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class QueryBuildContext {

    public static Map<String, Object> extractArguments(Field field, Map<String, GraphQLVariableDefinition> variableDefinitions) {
        Map<String, Object> map = new LinkedHashMap<>();

        for (Argument arg : field.getArguments()) {
            map.put(arg.getName(), extractValue(arg.getValue()));
            variableDefinitions.put(arg.getName(), new GraphQLVariableDefinition(arg.getName(), inferGraphQLType(arg.getValue())));
        }
        return map;
    }

    private static Object extractValue(Value<?> value) {
        if (value instanceof StringValue v) return v.getValue();
        if (value instanceof IntValue v) return v.getValue();
        if (value instanceof FloatValue v) return v.getValue();
        if (value instanceof BooleanValue v) return v.isValue();
        if (value instanceof EnumValue v) return v.getName();

        throw new RuntimeException(STR."Unsupported value: \{value.getClass()}");
    }

    private static String inferGraphQLType(Object value) {
        if (value instanceof String) return "String";
        if (value instanceof Integer) return "Int";
        if (value instanceof Long) return "Long";
        if (value instanceof Boolean) return "Boolean";
        if (value instanceof UUID) return "UUID";
        return "String"; // fallback
    }

    public static String buildVariablesDefinition(DataFetchingEnvironment environment, Map<String, GraphQLVariableDefinition> variableDefinitions) {
        List<String> definitions = new ArrayList<>();

        // 1. Variables originales del schema (ej: $id: UUID)
        List<GraphQLArgument> arguments = environment.getFieldDefinition().getArguments();
        if (arguments != null && !arguments.isEmpty()) {
            for (GraphQLArgument argument : arguments) {
                definitions.add(STR."$\{argument.getName()}: \{renderInputType(argument.getType())}");
            }
        }

        // 2. Variables dinámicas (ej: $productsByNames_name: String)
        if (variableDefinitions != null && !variableDefinitions.isEmpty()) {
            for (GraphQLVariableDefinition var : variableDefinitions.values()) {

                // Evitar duplicados por nombre
                boolean alreadyExists = arguments.stream()
                        .anyMatch(arg -> arg.getName().equals(var.getName()));

                if (!alreadyExists) {
                    definitions.add(STR."$\{var.getName()}: \{var.getType()}");
                }
            }
        }

        if (definitions.isEmpty()) {
            return "";
        }

        return String.join(", ", definitions);
    }

    public static String renderInputType(GraphQLInputType type) {
        if (type instanceof GraphQLNonNull nonNull) {
            return STR."\{renderInputType((GraphQLInputType) nonNull.getWrappedType())}!";
        }

        if (type instanceof GraphQLList list) {
            return STR."[\{renderInputType((GraphQLInputType) list.getWrappedType())}]";
        }

        if (type instanceof GraphQLScalarType scalar) {
            return scalar.getName();
        }

        if (type instanceof GraphQLEnumType enumType) {
            return enumType.getName();
        }

        if (type instanceof GraphQLInputObjectType inputObject) {
            return inputObject.getName();
        }

        throw new GraphQLException(GraphQLErrorCodes.SCHEMA_FETCH_ERROR, STR."Unsupported GraphQLInputType: \{type.getClass()}");
    }

}
