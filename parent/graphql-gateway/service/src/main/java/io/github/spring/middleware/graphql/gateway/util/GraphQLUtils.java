package io.github.spring.middleware.graphql.gateway.util;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.language.FieldDefinition;
import graphql.language.ImplementingTypeDefinition;
import graphql.language.InputObjectTypeDefinition;
import graphql.language.InputValueDefinition;
import graphql.language.SourceLocation;
import graphql.language.Type;
import graphql.language.TypeDefinition;
import graphql.language.TypeName;
import graphql.language.UnionTypeDefinition;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeUtil;
import graphql.schema.GraphQLUnionType;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.github.spring.middleware.graphql.gateway.exception.GraphQLErrorCodes;
import io.github.spring.middleware.graphql.gateway.exception.GraphQLException;
import io.github.spring.middleware.graphql.gateway.loader.GraphQLLinkTypesMap;
import lombok.AllArgsConstructor;
import org.apache.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class GraphQLUtils {

    private static Logger logger = Logger.getLogger(GraphQLUtils.class);

    public static boolean isRootType(String typeName) {
        return "Query".equals(typeName) || "Mutation".equals(typeName) || "Subscription".equals(typeName);
    }

    public static void mergeTypeDefinitions(TypeDefinition<?> source, TypeDefinition<?> target, TypeDefinitionRegistry registry) {
        if (!source.getName().equals(target.getName())) {
            throw new GraphQLException(GraphQLErrorCodes.SCHEMA_MERGE_ERROR, STR."Type names do not match: \{source.getName()} vs \{target.getName()}");
        }

        if (source instanceof ImplementingTypeDefinition s && target instanceof ImplementingTypeDefinition t) {
            mergeFields(s, t, registry);
            return;
        }

        if (source instanceof UnionTypeDefinition s && target instanceof UnionTypeDefinition t) {
            mergeUnionTypes(s, t, registry);
            return;
        }

        if (source instanceof InputObjectTypeDefinition s && target instanceof InputObjectTypeDefinition t) {
            mergeInputFields(s, t, registry);
            return;
        }

        // Para tipos sin campos (Scalar, Enum, etc.)
        if (!source.isEqualTo(target)) {
            throw new GraphQLException(GraphQLErrorCodes.SCHEMA_MERGE_ERROR, STR."Type definitions do not match for type \{source.getName()}");
        }
    }

    private static void mergeFields(ImplementingTypeDefinition source, ImplementingTypeDefinition target, TypeDefinitionRegistry registry) {

        Map<String, Type> sourceMap = (Map<String, Type>) source.getFieldDefinitions().stream().collect(Collectors.toMap(FieldDefinition::getName, FieldDefinition::getType));
        final ImplementingTypeDefinitionMetadata sourceMetadata = new ImplementingTypeDefinitionMetadata(source, sourceMap);

        Map<String, Type> targetMap = (Map<String, Type>) target.getFieldDefinitions().stream().collect(Collectors.toMap(FieldDefinition::getName, FieldDefinition::getType));
        final ImplementingTypeDefinitionMetadata targetMetadata = new ImplementingTypeDefinitionMetadata(target, targetMap);

        ImplementingTypeDefinitionMetadata smallest = getSmallestMetadata(sourceMetadata, targetMetadata);
        ImplementingTypeDefinitionMetadata biggest = getBiggestMetadata(sourceMetadata, targetMetadata);

        for (String fieldName : smallest.fields.keySet()) {
            if (!biggest.fields.containsKey(fieldName)) {
                throw new GraphQLException(GraphQLErrorCodes.SCHEMA_MERGE_ERROR, STR."Field \{fieldName} is missing in one of the type definitions");
            }
            if (!areEqualsTypes(biggest.fields.get(fieldName), smallest.fields.get(fieldName))) {
                throw new GraphQLException(GraphQLErrorCodes.SCHEMA_MERGE_ERROR, STR."Field \{fieldName} has different types in the type definitions: \{biggest.fields.get(fieldName)} vs \{smallest.fields.get(fieldName)}");
            }
        }

        registry.remove(smallest.typeDefinition);
        registry.remove(biggest.typeDefinition);
        registry.add(biggest.typeDefinition);
    }

    private static ImplementingTypeDefinitionMetadata getBiggestMetadata(ImplementingTypeDefinitionMetadata source, ImplementingTypeDefinitionMetadata target) {
        return source.fields.size() >= target.fields.size() ? source : target;
    }

    private static InputObjectTypeDefinitionMetadata getBiggestMetadata(InputObjectTypeDefinitionMetadata source, InputObjectTypeDefinitionMetadata target) {
        return source.validations.size() >= target.validations.size() ? source : target;
    }

    private static ImplementingTypeDefinitionMetadata getSmallestMetadata(ImplementingTypeDefinitionMetadata source, ImplementingTypeDefinitionMetadata target) {
        return source.fields.size() < target.fields.size() ? source : target;
    }

    private static InputObjectTypeDefinitionMetadata getSmallestMetadata(InputObjectTypeDefinitionMetadata source, InputObjectTypeDefinitionMetadata target) {
        return source.validations.size() < target.validations.size() ? source : target;
    }

    private static void mergeInputFields(InputObjectTypeDefinition source, InputObjectTypeDefinition target, TypeDefinitionRegistry registry) {

        Map<String, Type<?>> sourceMap = source.getInputValueDefinitions().stream()
                .collect(Collectors.toMap(InputValueDefinition::getName, InputValueDefinition::getType));
        final InputObjectTypeDefinitionMetadata sourceMetadata = new InputObjectTypeDefinitionMetadata(source, sourceMap);

        Map<String, Type<?>> targetMap = target.getInputValueDefinitions().stream()
                .collect(Collectors.toMap(InputValueDefinition::getName, InputValueDefinition::getType));
        final InputObjectTypeDefinitionMetadata targetMetadata = new InputObjectTypeDefinitionMetadata(target, targetMap);

        final InputObjectTypeDefinitionMetadata smallest = getSmallestMetadata(sourceMetadata, targetMetadata);
        final InputObjectTypeDefinitionMetadata biggest = getBiggestMetadata(sourceMetadata, targetMetadata);

        for (String fieldName : smallest.validations.keySet()) {
            if (!biggest.validations.containsKey(fieldName)) {
                throw new GraphQLException(GraphQLErrorCodes.SCHEMA_MERGE_ERROR, STR."Input field \{fieldName} is missing in one of the input object type definitions");
            }
            if (!areEqualsTypes(biggest.validations.get(fieldName), smallest.validations.get(fieldName))) {
                throw new GraphQLException(GraphQLErrorCodes.SCHEMA_MERGE_ERROR, STR."Input field \{fieldName} has different types in the input object type definitions: \{biggest.validations.get(fieldName)} vs \{smallest.validations.get(fieldName)}");
            }
        }
        registry.remove(smallest.inputObjectTypeDefinition);
        registry.remove(biggest.inputObjectTypeDefinition);
        registry.add(biggest.inputObjectTypeDefinition);
    }

    private static void mergeUnionTypes(UnionTypeDefinition source, UnionTypeDefinition target, TypeDefinitionRegistry registry) {
        Set<String> mergedMembers = Stream.concat(source.getMemberTypes().stream(), target.getMemberTypes().stream())
                .map(type -> ((TypeName) type).getName())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        UnionTypeDefinition richest = source.getMemberTypes().size() >= target.getMemberTypes().size() ? source : target;

        registry.remove(source);
        registry.remove(target);
        registry.add(richest.transform(builder -> builder.memberTypes(
                mergedMembers.stream().map(TypeName::new).collect(Collectors.toList())
        )));
    }

    private static boolean areEqualsTypes(Type source, Type target) {
        return source.toString().equals(target.toString());
    }

    @AllArgsConstructor
    private static class ImplementingTypeDefinitionMetadata {
        private ImplementingTypeDefinition typeDefinition;
        private Map<String, Type> fields;
    }

    @AllArgsConstructor
    private static class InputObjectTypeDefinitionMetadata {
        private InputObjectTypeDefinition inputObjectTypeDefinition;
        private Map<String, Type<?>> validations;
    }


    public static Object normalizeValue(Object fieldData, String fieldName, DataFetchingEnvironment environment, List<GraphQLLinkTypesMap.GraphQLResolvedLink> graphQLResolvedLinks) {
        Object normalizedData;

        if (fieldData instanceof Map<?, ?> map) {
            normalizedData = normalizeValue(
                    map,
                    fieldName,
                    environment.getFieldType(),
                    graphQLResolvedLinks
            );

        } else if (fieldData instanceof List<?> list) {
            normalizedData = list.stream()
                    .map(item -> {
                        if (item instanceof Map<?, ?> itemMap) {
                            return normalizeValue(
                                    itemMap,
                                    fieldName,
                                    GraphQLTypeUtil.unwrapAll(environment.getFieldType()),
                                    graphQLResolvedLinks
                            );
                        }
                        return item;
                    })
                    .toList();

        } else {
            normalizedData = fieldData;
        }
        return normalizedData;
    }

    private static Object normalizeValue(Object value, String fieldName, GraphQLType type, List<GraphQLLinkTypesMap.GraphQLResolvedLink> graphQLResolvedLinks) {
        if (value == null) {
            return null;
        }
        if (type instanceof GraphQLNonNull) {
            return normalizeValue(value, fieldName, ((GraphQLNonNull) type).getWrappedType(), graphQLResolvedLinks);
        }
        if (type instanceof GraphQLList) {
            if (!(value instanceof Iterable<?> iterable)) {
                throw new GraphQLException(GraphQLErrorCodes.VALUE_NORMALIZATION_ERROR, STR."Expected a list for type \{type}, but got: \{value}");
            }
            return StreamSupport.stream(iterable.spliterator(), false)
                    .map(item -> normalizeValue(item, fieldName, ((GraphQLList) type).getWrappedType(), graphQLResolvedLinks))
                    .collect(Collectors.toList());
        }

        if (type instanceof GraphQLScalarType scalar
                && "GraphQLLinkArguments".equals(scalar.getName())
                && value instanceof Map<?, ?> map) {

            GraphQLLinkTypesMap.GraphQLResolvedLink link =
                    resolveLinkArgumentsType(map, fieldName, graphQLResolvedLinks);

            if (link == null) {
                return value;
            }

            Map<String, Object> normalized = new LinkedHashMap<>();

            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String argumentName  = String.valueOf(entry.getKey());
                Object argumentValue  = entry.getValue();

                // 👇 AQUÍ es donde haces magia
                GraphQLType argumentType =
                        link.getTargetFieldArgumentTypes().get(argumentName);

                normalized.put(
                        argumentName,
                        normalizeValue(argumentValue, argumentName, argumentType, graphQLResolvedLinks)
                );
            }

            return normalized;
        }

        if (type instanceof GraphQLScalarType scalar) {
            return normalizeScalar(value, scalar);
        }

        if (type instanceof GraphQLEnumType) {
            return value;
        }
        if (type instanceof GraphQLObjectType objectType) {
            if (!(value instanceof Map map)) {
                throw new GraphQLException(GraphQLErrorCodes.VALUE_NORMALIZATION_ERROR, STR."Expected an object for type \{type}, but got: \{value}");
            }
            for (GraphQLFieldDefinition fieldDefinition : objectType.getFieldDefinitions()) {
                String childFieldName = fieldDefinition.getName();
                if (map.containsKey(childFieldName)) {
                    Object fieldValue = map.get(childFieldName);
                    GraphQLType resolvedType = resolveType(childFieldName, fieldDefinition.getType(), graphQLResolvedLinks);
                    Object normalizedValue = normalizeValue(fieldValue, childFieldName, resolvedType, graphQLResolvedLinks);
                    map.put(childFieldName, normalizedValue);
                }
            }
            return map;
        }
        if (type instanceof GraphQLUnionType unionType) {
            if (!(value instanceof Map map)) {
                throw new GraphQLException(GraphQLErrorCodes.VALUE_NORMALIZATION_ERROR, STR."Expected an object for union type \{type}, but got: \{value}");
            }
            String typeName = (String) map.get("__typename");
            if (typeName == null) {
                throw new GraphQLException(GraphQLErrorCodes.VALUE_NORMALIZATION_ERROR, STR."Missing __typename field for union type \{type}");
            }
            GraphQLObjectType matchedType = (GraphQLObjectType) unionType.getTypes().stream()
                    .filter(t -> t.getName().equals(typeName))
                    .findFirst()
                    .orElseThrow(() -> new GraphQLException(GraphQLErrorCodes.VALUE_NORMALIZATION_ERROR, STR."No matching type found for __typename: \{typeName} in union type \{type}"));
            return normalizeValue(value, fieldName, matchedType, graphQLResolvedLinks);
        }

        return value;
    }


    private static GraphQLLinkTypesMap.GraphQLResolvedLink resolveLinkArgumentsType(
            Map<?, ?> value,
            String fieldName,
            List<GraphQLLinkTypesMap.GraphQLResolvedLink> graphQLResolvedLinks
    ) {
        Set<String> valueKeys = value.keySet().stream()
                .map(String::valueOf)
                .collect(Collectors.toSet());

        return graphQLResolvedLinks.stream()
                .filter(link -> link.getFieldLinkDefinition() != null)
                .filter(link -> fieldName.equals(link.getFieldLinkDefinition().getFieldName()))
                .filter(link -> link.getTargetFieldArgumentTypes() != null)
                .filter(link -> link.getTargetFieldArgumentTypes().keySet().containsAll(valueKeys))
                .findFirst()
                .orElse(null);
    }


    private static GraphQLType resolveType(String fieldName, GraphQLType originType, List<GraphQLLinkTypesMap.GraphQLResolvedLink> graphQLResolvedLinks) {
        return graphQLResolvedLinks.stream()
                .filter(link -> link.getFieldLinkDefinition().getFieldName().equals(fieldName))
                .findFirst()
                .map(link -> link.getOriginOperationReturnType())
                .orElse(originType);
    }


    private static Object normalizeScalar(Object value, GraphQLScalarType scalar) {

        try {
            return scalar.getCoercing().parseValue(value);
        } catch (Exception e) {
            logger.warn(STR."Failed to parse value \{value} for scalar type \{scalar.getName()} using coercing, trying manual parsing", e);
        }

        return switch (scalar.getName()) {
            case "Instant" -> value instanceof String s ? java.time.Instant.parse(s) : value;
            case "UUID" -> value instanceof String s ? java.util.UUID.fromString(s) : value;
            case "BigDecimal" -> value instanceof String s ? new java.math.BigDecimal(s) : value;
            case "BigInteger" -> value instanceof String s ? new java.math.BigInteger(s) : value;
            case "Date" -> value instanceof String s ? java.time.LocalDate.parse(s) : value;
            case "OffsetDateTime" -> value instanceof String s ? java.time.OffsetDateTime.parse(s) : value;
            case "LocalDateTime" -> value instanceof String s ? java.time.LocalDateTime.parse(s) : value;
            case "LocalTime" -> value instanceof String s ? java.time.LocalTime.parse(s) : value;
            case "ZonedDateTime" -> value instanceof String s ? java.time.ZonedDateTime.parse(s) : value;
            case "Duration" -> value instanceof String s ? java.time.Duration.parse(s) : value;
            case "Period" -> value instanceof String s ? java.time.Period.parse(s) : value;
            case "OffsetTime" -> value instanceof String s ? java.time.OffsetTime.parse(s) : value;
            case "Year" -> value instanceof String s ? java.time.Year.parse(s) : value;
            case "YearMonth" -> value instanceof String s ? java.time.YearMonth.parse(s) : value;
            case "URL" -> {
                try {
                    yield value instanceof String s ? URI.create(s).toURL() : value;
                } catch (MalformedURLException e) {
                    throw new GraphQLException(GraphQLErrorCodes.VALUE_NORMALIZATION_ERROR, STR."Failed to parse value \{value} as URL", e);
                }
            }
            case "URI" -> value instanceof String s ? URI.create(s) : value;
            case "Byte" -> value instanceof String s ? Byte.parseByte(s) : value;
            case "Short" -> value instanceof String s ? Short.parseShort(s) : value;
            case "Integer" -> value instanceof String s ? Integer.parseInt(s) : value;
            case "Long" -> value instanceof String s ? Long.parseLong(s) : value;
            case "Float" -> value instanceof String s ? Float.parseFloat(s) : value;
            case "Double" -> value instanceof String s ? Double.parseDouble(s) : value;
            case "Boolean" -> value instanceof String s ? Boolean.parseBoolean(s) : value;
            default -> value;
        };
    }

    public static List<GraphQLError> mapErrors(Object errorsObject, DataFetchingEnvironment environment) {
        if (!(errorsObject instanceof List<?> list)) {
            return List.of();
        }

        return list.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(error -> GraphqlErrorBuilder.newError(environment)
                        .message(String.valueOf(error.get("message")))
                        .locations(toSourceLocations(error.get("locations")))
                        .path((List<Object>) error.get("path"))
                        .extensions(error.get("extensions") instanceof Map ? (Map<String, Object>) error.get("extensions") : null)
                        .build())
                .toList();
    }

    @SuppressWarnings("unchecked")
    public static List<SourceLocation> toSourceLocations(Object locationsObj) {
        if (!(locationsObj instanceof List<?> locations)) {
            return List.of();
        }

        return locations.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(loc -> new SourceLocation(
                        (Integer) loc.get("line"),
                        (Integer) loc.get("column")
                ))
                .toList();
    }


}
