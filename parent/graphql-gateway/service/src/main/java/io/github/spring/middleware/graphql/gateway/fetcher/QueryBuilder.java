package io.github.spring.middleware.graphql.gateway.fetcher;

import graphql.language.Field;
import graphql.language.FragmentSpread;
import graphql.language.InlineFragment;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNamedType;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeUtil;
import io.github.spring.middleware.graphql.gateway.exception.GraphQLErrorCodes;
import io.github.spring.middleware.graphql.gateway.exception.GraphQLException;
import io.github.spring.middleware.graphql.gateway.loader.GraphQLLinkTypesMap;
import io.github.spring.middleware.graphql.gateway.merger.GraphQLOperationType;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class QueryBuilder {

    public static String buildGraphQLQueryWithVariables(
            DataFetchingEnvironment environment,
            GraphQLOperationType operationType,
            String fieldName,
            Map<String,GraphQLLinkTypesMap.GraphQLResolvedLink> resolvedLinksByFieldName
    ) {
        StringBuilder sb = new StringBuilder();
        String operation = operationType == GraphQLOperationType.MUTATION ? "mutation" : "query";

        sb.append(operation).append(" ").append(fieldName);

        String variablesDefinition = buildVariablesDefinition(environment);
        if (!variablesDefinition.isEmpty()) {
            sb.append("(").append(variablesDefinition).append(")");
        }

        sb.append(" {");
        appendRootFieldWithVariables(environment.getField(), environment, sb, "\n  ", resolvedLinksByFieldName);
        sb.append("\n}");

        return sb.toString();
    }

    private static String buildVariablesDefinition(DataFetchingEnvironment environment) {
        List<GraphQLArgument> arguments = environment.getFieldDefinition().getArguments();
        if (arguments.isEmpty()) {
            return "";
        }

        return arguments.stream()
                .map(argument -> STR."$\{argument.getName()}: \{renderInputType(argument.getType())}")
                .collect(Collectors.joining(", "));
    }

    private static String renderInputType(GraphQLInputType type) {
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

    private static void appendRootFieldWithVariables(
            Field field,
            DataFetchingEnvironment environment,
            StringBuilder sb,
            String indent,
            Map<String,GraphQLLinkTypesMap.GraphQLResolvedLink> resolvedLinksByFieldName
    ) {
        sb.append(indent).append(field.getName());
        appendArguments(environment.getArguments(), sb);

        if (field.getSelectionSet() != null && !field.getSelectionSet().getSelections().isEmpty()) {
            appendSelectionSet(field.getSelectionSet(), sb, indent, resolvedLinksByFieldName);
        }
    }

    public static void appendSelectionSet(SelectionSet selectionSet, StringBuilder sb, String indent, Map<String,GraphQLLinkTypesMap.GraphQLResolvedLink> resolvedLinksByFieldName) {
        sb.append(" {");
        for (Selection<?> selection : selectionSet.getSelections()) {
            if (selection instanceof Field childField) {
                appendNestedField(childField, sb, STR."\{indent}  ", resolvedLinksByFieldName);
            } else if (selection instanceof InlineFragment inlineFragment) {
                appendInlineFragment(inlineFragment, sb, indent, resolvedLinksByFieldName);
            } else if (selection instanceof FragmentSpread) {
                throw new GraphQLException(GraphQLErrorCodes.SCHEMA_FETCH_ERROR, "FragmentSpread is not supported in this implementation");
            }
        }
        sb.append("\n").append(indent).append("}");
    }


    private static void appendInlineFragment(InlineFragment inlineFragment, StringBuilder sb, String indent,  Map<String,GraphQLLinkTypesMap.GraphQLResolvedLink> resolvedLinksByFieldName) {
        sb.append("\n").append(indent).append("... on ").append(inlineFragment.getTypeCondition().getName());
        appendSelectionSet(inlineFragment.getSelectionSet(), sb, STR."\{indent}  ", resolvedLinksByFieldName);
    }


    private static void appendArguments(Map<String, Object> arguments, StringBuilder sb) {
        if (arguments.isEmpty()) {
            return;
        }

        sb.append("(");
        String argsAsVariables = arguments.keySet().stream()
                .map(argumentName -> STR."\{argumentName}: $\{argumentName}")
                .collect(Collectors.joining(", "));
        sb.append(argsAsVariables).append(")");
    }


    private static void appendNestedField(Field field, StringBuilder sb, String indent, Map<String,GraphQLLinkTypesMap.GraphQLResolvedLink> resolvedLinksByFieldName) {
        sb.append("\n").append(indent).append(field.getName());

        GraphQLLinkTypesMap.GraphQLResolvedLink link = resolvedLinksByFieldName.get(field.getName());

        if (link != null) {
            GraphQLType originalType = GraphQLTypeUtil.unwrapAll(link.getOriginOperationReturnType());

            if (originalType instanceof GraphQLNamedType namedType
                    && "GraphQLLinkArguments".equals(namedType.getName())) {

                sb.append(" {");
                sb.append("\n").append(indent).append("  values");
                sb.append("\n").append(indent).append("}");
                return;
            }

            // caso normal (UUID, lista, etc.)
            return;
        }

        SelectionSet selectionSet = field.getSelectionSet();
        if (selectionSet != null && !selectionSet.getSelections().isEmpty()) {
            sb.append(" {");
            for (Selection<?> selection : selectionSet.getSelections()) {
                if (selection instanceof Field childField) {
                    appendNestedField(childField, sb, STR."\{indent}  ", resolvedLinksByFieldName);
                } else if (selection instanceof InlineFragment inlineFragment) {
                    appendInlineFragment(inlineFragment, sb, indent, resolvedLinksByFieldName);
                } else if (selection instanceof FragmentSpread) {
                    throw new GraphQLException(GraphQLErrorCodes.SCHEMA_FETCH_ERROR, "FragmentSpread is not supported in this implementation");
                }
            }
            sb.append("\n").append(indent).append("}");
        }
    }

}
