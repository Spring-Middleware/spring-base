package com.middleware.mongo.components;

import com.middleware.mongo.search.MongoSearch;
import com.middleware.mongo.search.MongoTextSearch;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.TextQuery;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Component
public class QueryCreatorComponent<S extends MongoSearch> {

    public Query createQuery(S mongoSearch) {

        Query query;
        if (mongoSearch instanceof MongoTextSearch) {
            MongoTextSearch mongoTextSearch = (MongoTextSearch) mongoSearch;
            query = Optional.ofNullable(mongoTextSearch.getText())
                    .map(text -> (Query) TextQuery
                            .queryText(TextCriteria.forDefaultLanguage().matching("\"" + text + "\"")))
                    .orElseGet(() -> new Query());

        } else {
            query = new Query();
        }
        return query;
    }

    public Mono<Query> createQueryAsync(Mono<S> mongoSearch) {
        return mongoSearch.map(search -> {
            return createQuery(search);
        });
    }

}
