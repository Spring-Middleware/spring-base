package com.middleware.mongo.function;

import com.middleware.mongo.exception.MongoSearchException;
import com.middleware.mongo.types.OperationType;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public enum FunctionsOperationType {

    IS(OperationType.IS, Criteria::is),
    IN(OperationType.IN, (c, o) -> c.in(toCollectionString((Collection) o))),
    NOT_IN(OperationType.NOT_IN, (c, o) -> c.nin(toCollectionString((Collection) o))),
    EXISTS(OperationType.EXISTS, (c, o) -> c.exists(true)),
    NOT_EXISTS(OperationType.NOT_EXISTS, (c, o) -> c.exists(false)),
    GREATER(OperationType.GREATER, Criteria::gt),
    LIKE(OperationType.LIKE, (c, s) -> c.regex((String) s, "i")),
    LESS(OperationType.LESS, Criteria::lt),
    GREATER_OR_EQUAL(OperationType.GREATER_OR_EQUAL, Criteria::gte),
    LESS_OR_EQUAL(OperationType.LESS_OR_EQUAL, Criteria::lte);

    FunctionsOperationType(OperationType operationType, BiFunction<Criteria, Object, Criteria> criteriaFunction) {

        this.operationType = operationType;
        this.criteriaFunction = criteriaFunction;
    }

    private OperationType operationType;
    private BiFunction<Criteria, Object, Criteria> criteriaFunction;

    public static Criteria applyOperation(OperationType operationType, Criteria criteria,
            Object value) {

        return Arrays.stream(FunctionsOperationType.values()).filter(t -> t.operationType == operationType)
                .map(t -> t.criteriaFunction.apply(criteria, value)).findFirst()
                .orElseThrow(() -> new MongoSearchException("No found operation type " + operationType));
    }

    private static Collection<String> toCollectionString(Collection<?> objects) {

        return objects.stream().map(Objects::toString).collect(Collectors.toSet());
    }

}
