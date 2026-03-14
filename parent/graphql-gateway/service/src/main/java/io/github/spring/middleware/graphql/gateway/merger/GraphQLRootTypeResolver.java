package io.github.spring.middleware.graphql.gateway.merger;

import graphql.language.ObjectTypeDefinition;
import graphql.language.OperationTypeDefinition;
import graphql.language.TypeName;
import graphql.schema.idl.TypeDefinitionRegistry;

import java.util.List;
import java.util.Optional;

public final class GraphQLRootTypeResolver {

    private GraphQLRootTypeResolver() {
    }

    public static Optional<ObjectTypeDefinition> getQuery(TypeDefinitionRegistry registry) {
        return getRootType(registry, "query", "Query");
    }

    public static Optional<ObjectTypeDefinition> getMutation(TypeDefinitionRegistry registry) {
        return getRootType(registry, "mutation", "Mutation");
    }

    private static Optional<ObjectTypeDefinition> getRootType(
            TypeDefinitionRegistry registry,
            String operationName,
            String defaultTypeName
    ) {
        if (registry.schemaDefinition().isPresent()) {
            List<OperationTypeDefinition> operationTypes =
                    registry.schemaDefinition().get().getOperationTypeDefinitions();

            Optional<OperationTypeDefinition> operation = operationTypes.stream()
                    .filter(op -> operationName.equals(op.getName()))
                    .findFirst();

            if (operation.isEmpty()) {
                return Optional.empty();
            }

            TypeName typeName = operation.get().getTypeName();
            return registry.getType(typeName.getName())
                    .filter(ObjectTypeDefinition.class::isInstance)
                    .map(ObjectTypeDefinition.class::cast);
        }

        return registry.getType(defaultTypeName)
                .filter(ObjectTypeDefinition.class::isInstance)
                .map(ObjectTypeDefinition.class::cast);
    }
}