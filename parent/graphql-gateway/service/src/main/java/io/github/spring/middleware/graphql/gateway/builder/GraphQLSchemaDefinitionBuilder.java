package io.github.spring.middleware.graphql.gateway.builder;

import graphql.language.ObjectTypeDefinition;
import graphql.language.TypeDefinition;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.github.spring.middleware.graphql.gateway.exception.GraphQLErrorCodes;
import io.github.spring.middleware.graphql.gateway.exception.GraphQLException;
import io.github.spring.middleware.graphql.gateway.loader.GraphQLTypeRegistryMap;
import io.github.spring.middleware.graphql.gateway.merger.GraphQLMerged;
import io.github.spring.middleware.graphql.gateway.merger.GraphQLOperationType;
import org.springframework.stereotype.Component;

@Component
public class GraphQLSchemaDefinitionBuilder {

    public TypeDefinitionRegistry build(
            GraphQLMerged merged,
            GraphQLTypeRegistryMap typeRegistryMap
    ) {
        TypeDefinitionRegistry registry = new TypeDefinitionRegistry();

        var queryFields = merged.getFieldDefinitionsByOperationType(GraphQLOperationType.QUERY);
        if (!queryFields.isEmpty()) {
            ObjectTypeDefinition query = ObjectTypeDefinition.newObjectTypeDefinition()
                    .name("Query")
                    .fieldDefinitions(queryFields)
                    .build();
            registry.add(query);
        }

        var mutationFields = merged.getFieldDefinitionsByOperationType(GraphQLOperationType.MUTATION);
        if (!mutationFields.isEmpty()) {
            ObjectTypeDefinition mutation = ObjectTypeDefinition.newObjectTypeDefinition()
                    .name("Mutation")
                    .fieldDefinitions(mutationFields)
                    .build();
            registry.add(mutation);
        }


        typeRegistryMap.registryMap().values().forEach(sourceRegistry -> {
            sourceRegistry.types().forEach((typeName, typeDefinition) -> {
                if (isRootType(typeName)) {
                    return;
                }
                addTypeIfAbsentOrFail(registry, typeName, typeDefinition);
            });

            sourceRegistry.scalars().forEach((scalarName, scalarDefinition) -> {
                if (!registry.scalars().containsKey(scalarName)) {
                    registry.add(scalarDefinition);
                }
            });
        });
        return registry;

    }

    private void addTypeIfAbsentOrFail(
            TypeDefinitionRegistry targetRegistry,
            String typeName,
            TypeDefinition<?> typeDefinition
    ) {
        var existing = targetRegistry.getTypeOrNull(typeName);
        if (existing == null) {
            targetRegistry.add(typeDefinition);
            return;
        }

        if (!existing.equals(typeDefinition)) {
            throw new GraphQLException(
                    GraphQLErrorCodes.SCHEMA_MERGE_ERROR,
                    STR."Conflicting GraphQL type definition detected for type: \{typeName}"
            );
        }
    }

    private boolean isRootType(String typeName) {
        return "Query".equals(typeName) || "Mutation".equals(typeName) || "Subscription".equals(typeName);
    }

}