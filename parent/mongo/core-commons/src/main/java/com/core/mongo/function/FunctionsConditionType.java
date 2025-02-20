package com.core.mongo.function;

import com.core.mongo.exception.MongoSearchException;
import com.core.mongo.types.ConditionType;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.BiFunction;

public enum FunctionsConditionType {

    AND(ConditionType.AND, Criteria::andOperator),
    OR(ConditionType.OR, Criteria::orOperator);

    private ConditionType conditionType;
    private BiFunction<Criteria, Collection<Criteria>, Criteria> conditionFunction;

    FunctionsConditionType(ConditionType conditionType,
                           BiFunction<Criteria, Collection<Criteria>, Criteria> conditionFunction) {

        this.conditionType = conditionType;
        this.conditionFunction = conditionFunction;
    }

    public static Criteria applyCondition(ConditionType conditionType, Criteria criteria,
                                          Collection<Criteria> criterias) {

        Criteria composedCriteria = null;
        if (!criterias.isEmpty()) {
            composedCriteria = Arrays.stream(FunctionsConditionType.values())
                    .filter(c -> c.conditionType == conditionType)
                    .map(c -> c.conditionFunction.apply(criteria, criterias)).findFirst()
                    .orElseThrow(() -> new MongoSearchException("No found conditionType " + conditionType));
        }
        return composedCriteria;
    }

}
