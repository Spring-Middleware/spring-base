package io.github.spring.middleware.mongo.search;

import io.github.spring.middleware.mongo.annotations.MongoSearchClass;
import io.github.spring.middleware.mongo.annotations.MongoSearchProperty;
import io.github.spring.middleware.mongo.types.ConditionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestDataSearch implements MongoSearch {

    @MongoSearchProperty
    private String key;
    @MongoSearchProperty
    private String value;
    @MongoSearchProperty("name")
    private String naming;
    @MongoSearchProperty(conditionType = ConditionType.OR)
    private String orA;
    @MongoSearchProperty(conditionType = ConditionType.OR)
    private String orB;
    @MongoSearchClass
    private TestSubDataSearch subSearch;
    @MongoSearchClass(isCollection = true)
    private TestItemSearch itemSearch;
    @MongoSearchClass(isCollection = true)
    private Set<TestItemSearch> itemsSearch;

}
