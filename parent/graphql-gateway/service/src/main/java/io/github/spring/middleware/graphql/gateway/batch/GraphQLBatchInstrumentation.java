package io.github.spring.middleware.graphql.gateway.batch;

import graphql.GraphQLContext;
import graphql.execution.MergedField;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.SimpleInstrumentationContext;
import graphql.execution.instrumentation.parameters.InstrumentationFieldCompleteParameters;
import graphql.execution.instrumentation.parameters.InstrumentationFieldParameters;
import graphql.language.Field;
import graphql.language.FragmentSpread;
import graphql.language.InlineFragment;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import graphql.schema.GraphQLModifiedType;
import graphql.schema.GraphQLNamedType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;
import io.github.spring.middleware.graphql.gateway.loader.GraphQLLinkTypesMap;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;


@Slf4j
public class GraphQLBatchInstrumentation implements Instrumentation {

    private final GraphQLBatchExecutor graphQLBatchExecutor;
    private final GraphQLLinkTypesMap graphQLLinkTypesMap;

    public GraphQLBatchInstrumentation(final GraphQLBatchExecutor graphQLBatchExecutor, GraphQLLinkTypesMap graphQLLinkTypesMap) {
        this.graphQLBatchExecutor = graphQLBatchExecutor;
        this.graphQLLinkTypesMap = graphQLLinkTypesMap;
    }

    @Override
    public InstrumentationContext<Object> beginFieldExecution(InstrumentationFieldParameters parameters, InstrumentationState state) {
        GraphQLContext context = parameters.getExecutionContext().getGraphQLContext();
        GraphQLLinkResolvedBatchedRegistry registry = context.get("batchedRegistry");
        final var parentTypeName = getParentTypeName(parameters);
        if (registry != null && parentTypeName != null && !isLinkKeyAlreadyInstrumented(context, parentTypeName, parameters.getField().getName()) && graphQLLinkTypesMap.isFieldLinked(parentTypeName, parameters.getField().getName())) {
            addBatchedLinkKeyToInstrumented(context, parentTypeName, parameters.getField().getName());
            return new InstrumentationContext() {

                @Override
                public void onDispatched() {
                    dispatchPendingBatches(registry).whenComplete((v, ex) -> removeBatchedLinkKeyFromInstrumented(context, parentTypeName, parameters.getField().getName()));
                }

                @Override
                public void onCompleted(@Nullable Object result, @Nullable Throwable t) {

                }
            };
        }
        return SimpleInstrumentationContext.noOp();
    }

    @Override
    public InstrumentationContext<Object> beginFieldListCompletion(InstrumentationFieldCompleteParameters parameters, InstrumentationState state) {
        GraphQLContext context = parameters.getExecutionContext().getGraphQLContext();
        GraphQLLinkResolvedBatchedRegistry registry = context.get("batchedRegistry");
        final var innerTypeName = getInnerTypeName(parameters.getField().getType());
        final var childrenFieldNames = getSelectedChildrenFieldNames(parameters);

        List<String> linkedChildrenToInstrument = childrenFieldNames.stream()
                .filter(fieldName -> graphQLLinkTypesMap.isFieldLinked(innerTypeName, fieldName))
                .filter(fieldName -> !isLinkKeyAlreadyInstrumented(context, innerTypeName, fieldName))
                .toList();

        if (registry != null && innerTypeName != null && !linkedChildrenToInstrument.isEmpty()) {
            linkedChildrenToInstrument.forEach(fieldName ->
                    addBatchedLinkKeyToInstrumented(context, innerTypeName, fieldName)
            );

            return new InstrumentationContext() {

                @Override
                public void onDispatched() {
                    dispatchPendingBatches(registry).whenComplete((v, ex) -> linkedChildrenToInstrument.forEach(fieldName ->
                            removeBatchedLinkKeyFromInstrumented(context, innerTypeName, fieldName)
                    ));
                }

                @Override
                public void onCompleted(@Nullable Object result, @Nullable Throwable t) {

                }
            };
        }
        return SimpleInstrumentationContext.noOp();
    }

    private CompletableFuture<Void> dispatchPendingBatches(GraphQLLinkResolvedBatchedRegistry registry) {
        List<CompletableFuture<Void>> batchFutures = registry.getAllBatchedLinks().stream()
                .map(graphQLBatchExecutor::executeBatch)
                .toList();
        if (batchFutures.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.allOf(batchFutures.toArray(new CompletableFuture[0]));
    }

    private String getParentTypeName(InstrumentationFieldParameters parameters) {
        GraphQLType type = parameters.getExecutionStepInfo().getParent().getType();

        while (type instanceof GraphQLModifiedType modifiedType) {
            type = modifiedType.getWrappedType();
        }

        if (type instanceof GraphQLNamedType namedType) {
            return namedType.getName();
        }

        return null;
    }

    private List<String> getSelectedChildrenFieldNames(InstrumentationFieldCompleteParameters parameters) {
        MergedField mergedField = parameters.getExecutionStepInfo().getField();

        if (mergedField == null || mergedField.getSingleField() == null) {
            return List.of();
        }

        SelectionSet selectionSet = mergedField.getSingleField().getSelectionSet();
        if (selectionSet == null) {
            return List.of();
        }

        List<String> fieldNames = new ArrayList<>();
        collectSelectedFieldNames(selectionSet, fieldNames);

        return fieldNames.stream().distinct().toList();
    }

    private void collectSelectedFieldNames(SelectionSet selectionSet, List<String> fieldNames) {
        if (selectionSet == null) {
            return;
        }

        for (Selection<?> selection : selectionSet.getSelections()) {
            if (selection instanceof Field field) {
                fieldNames.add(field.getName());
            } else if (selection instanceof InlineFragment inlineFragment) {
                collectSelectedFieldNames(inlineFragment.getSelectionSet(), fieldNames);
            } else if (selection instanceof FragmentSpread) {
                // si luego quieres soportar fragment spreads nombrados,
                // aquí habría que resolverlos desde el Document/ExecutionContext
            }
        }
    }

    private String getInnerTypeName(GraphQLOutputType type) {
        GraphQLType current = type;

        while (current instanceof GraphQLModifiedType modifiedType) {
            current = modifiedType.getWrappedType();
        }

        if (current instanceof GraphQLNamedType namedType) {
            return namedType.getName();
        }

        return null;
    }

    private record BatchedLinkKeyInstrumented(String parentTypeName, String fieldName) {
    }

    private List<BatchedLinkKeyInstrumented> getBatchedLinkKeyInstrumented(GraphQLContext context) {
        return context.computeIfAbsent("batchedLinkKeyInstrumented", key -> new ArrayList());
    }

    private void addBatchedLinkKeyToInstrumented(GraphQLContext context, String parentTypeName, String fieldName) {
        List<BatchedLinkKeyInstrumented> instrumentedKeys = getBatchedLinkKeyInstrumented(context);
        BatchedLinkKeyInstrumented key = new BatchedLinkKeyInstrumented(parentTypeName, fieldName);
        if (!instrumentedKeys.contains(key)) {
            instrumentedKeys.add(key);
        }
    }

    private void removeBatchedLinkKeyFromInstrumented(GraphQLContext context, String parentTypeName, String fieldName) {
        List<BatchedLinkKeyInstrumented> instrumentedKeys = getBatchedLinkKeyInstrumented(context);
        instrumentedKeys.remove(new BatchedLinkKeyInstrumented(parentTypeName, fieldName));
    }

    public boolean isLinkKeyAlreadyInstrumented(GraphQLContext context, String parentTypeName, String fieldName) {
        List<BatchedLinkKeyInstrumented> instrumentedKeys = getBatchedLinkKeyInstrumented(context);
        return instrumentedKeys.contains(new BatchedLinkKeyInstrumented(parentTypeName, fieldName));
    }

}
