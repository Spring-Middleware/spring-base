package io.github.spring.middleware.graphql.gateway.builder;

import graphql.language.ListType;
import graphql.language.ObjectTypeDefinition;
import graphql.language.Type;
import graphql.language.TypeDefinition;
import graphql.language.TypeName;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLType;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.github.spring.middleware.graphql.gateway.loader.GraphQLLinkTypesMap;
import io.github.spring.middleware.graphql.gateway.loader.GraphQLTypeRegistryMap;
import io.github.spring.middleware.graphql.gateway.merger.GraphQLMerged;
import io.github.spring.middleware.graphql.gateway.merger.GraphQLOperationType;
import io.github.spring.middleware.graphql.metadata.GraphQLArgumentLinkDefinition;
import org.springframework.stereotype.Component;

import static io.github.spring.middleware.graphql.gateway.util.GraphQLUtils.isRootType;
import static io.github.spring.middleware.graphql.gateway.util.GraphQLUtils.mergeTypeDefinitions;

@Component
public class GraphQLSchemaDefinitionBuilder {

    public TypeDefinitionRegistry build(
            GraphQLMerged merged,
            GraphQLTypeRegistryMap typeRegistryMap,
            GraphQLLinkTypesMap linkTypesMap
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
                TypeDefinition<?> adapted = adaptLinkedFields(typeDefinition, typeName, linkTypesMap, typeRegistryMap);
                addTypeIfAbsentOrMerge(registry, typeName, adapted);
            });

            sourceRegistry.scalars().forEach((scalarName, scalarDefinition) -> {
                if (!registry.scalars().containsKey(scalarName)) {
                    registry.add(scalarDefinition);
                }
            });
        });
        return registry;

    }

    private void addTypeIfAbsentOrMerge(
            TypeDefinitionRegistry targetRegistry,
            String typeName,
            TypeDefinition<?> typeDefinition
    ) {
        var existing = targetRegistry.getTypeOrNull(typeName);
        if (existing == null) {
            targetRegistry.add(typeDefinition);
            return;
        }

        mergeTypeDefinitions(existing, typeDefinition, targetRegistry);
    }

    private TypeDefinition<?> adaptLinkedFields(
            TypeDefinition<?> typeDefinition,
            String typeName,
            GraphQLLinkTypesMap linkTypesMap,
            GraphQLTypeRegistryMap typeRegistryMap

    ) {
        if (!(typeDefinition instanceof ObjectTypeDefinition objectType)) {
            return typeDefinition;
        }

        var newFields = objectType.getFieldDefinitions().stream()
                .map(field -> {
                    GraphQLLinkTypesMap.GraphQLResolvedLink link = linkTypesMap.findGraphQLResolvedLink(typeName, field.getName());
                    if (link == null) {
                        return field;
                    }

                    GraphQLType graphQLTypeOriginalType = typeRegistryMap.getGraphQLTypeForFieldInType(link.getSchemaLocation(), objectType.getName(), field.getName(), linkTypesMap);

                    Type<?> patchedType =
                            link.getFieldLinkDefinition().isCollection()
                                    ? ListType.newListType(
                                    TypeName.newTypeName(link.getFieldLinkDefinition().getTargetTypeName()).build()
                            ).build()
                                    : TypeName.newTypeName(link.getFieldLinkDefinition().getTargetTypeName()
                            ).build();

                    link.setOriginOperationReturnType(graphQLTypeOriginalType);

                    GraphQLFieldDefinition targetFieldType = typeRegistryMap.getGraphQLFieldDefinition(
                            link.getTargetSchemaLocation(),
                            link.getFieldLinkDefinition().getQuery(), linkTypesMap
                    );

                    for (GraphQLArgumentLinkDefinition argDef : link.getFieldLinkDefinition().getArgumentLinkDefinitions()) {
                        GraphQLArgument targetArgument = targetFieldType.getArgument(argDef.getArgumentName());
                        if (targetArgument == null) {
                            throw new IllegalStateException(
                                    STR."Target argument not found: \{link.getFieldLinkDefinition().getQuery()}.\{argDef.getArgumentName()}"
                            );
                        }
                        link.addTargetFieldArgumentType(argDef.getArgumentName(), targetArgument.getType());
                    }

                    return field.transform(builder -> builder.type(patchedType));
                })
                .toList();

        return objectType.transform(builder -> builder.fieldDefinitions(newFields));
    }

}