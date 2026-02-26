package com.middleware.jpa.buffer;

import com.middleware.jpa.annotations.PreCondition;
import com.middleware.jpa.annotations.SearchProperties;
import com.middleware.jpa.annotations.SearchProperty;
import com.middleware.jpa.annotations.SearchPropertyExists;
import com.middleware.jpa.annotations.SubSearch;
import com.middleware.jpa.query.ParameterCounter;
import com.middleware.jpa.query.Value;
import com.middleware.jpa.search.Search;
import com.middleware.jpa.types.ConditionType;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Stream;

public class ConditionBuffer {

    private StringBuffer conditionBuffer = new StringBuffer();
    private JoinBuffer joinBuffer;
    private ConditionType conditionType;
    private ParameterCounter parameterCounter;

    public ConditionBuffer(JoinBuffer joinBuffer, ParameterCounter parameterCounter, ConditionType conditionType) {

        this.joinBuffer = joinBuffer;
        this.parameterCounter = parameterCounter;
        this.conditionType = conditionType;
    }

    public void buildSearchPropertyCondition(SearchProperty searchProperty, Object value) {

        if (isSearchPropertyValidValue(searchProperty, value)) {
            conditionBuffer.append(" (");
            processSearchProperty(searchProperty, value);
            conditionBuffer.append(")");
        }
    }

    public void buildSearchPropertiesCondition(SearchProperties searchProperties, Object value) {

        if (Value.isValid((value)) || isSearchForNull(searchProperties)) {
            conditionBuffer.append(" ( ");
            Arrays.asList(searchProperties.value()).forEach(searchProperty -> {
                if (isSearchPropertyValidValue(searchProperty, value)) {
                    processSearchProperty(searchProperty, value);
                    conditionBuffer.append(" OR ");
                }
            });
            conditionBuffer.setLength(conditionBuffer.length() - 4);
            conditionBuffer.append(")");
        }
    }

    private boolean isSearchForNull(SearchProperties searchProperties) {

        return Stream.of(searchProperties.value()).anyMatch(sp -> sp.searchForNull());
    }

    public <S extends Search> void buildSubSearch(SubSearch subSearch, S searchOr) throws Exception {

        if (searchOr != null) {
            conditionBuffer.append(" ( ");
            WhereBuffer whereBuffer = new WhereBuffer(joinBuffer, parameterCounter, false);
            whereBuffer.buildWhere(searchOr);
            conditionBuffer.append(whereBuffer).append(")");
        }
    }

    public <S extends Search> void buldSearchPropertyExists(SearchPropertyExists searchPropertyExists, Boolean exists) {

        if (exists != null) {
            if (joinBuffer.isReferencedInJoin(searchPropertyExists.value().trim())) {
                conditionBuffer.append(" c.");
            }
            conditionBuffer.append(searchPropertyExists.value()).append(exists ? " is not empty " : " is empty ");
        }
    }

    private boolean isSearchPropertyValidValue(SearchProperty searchProperty, Object value) {

        return Value.isValid(value) || searchProperty.searchForNull();
    }

    private void processSearchProperty(SearchProperty searchProperty, Object value) {

        joinBuffer.processJoinSearchProperty(searchProperty.join());
        if (!searchProperty.isLike()) {
            processNotIsLike(searchProperty, value);
        } else {
            processLike(searchProperty, value);
        }
    }

    private void processNotIsLike(SearchProperty searchProperty, Object value) {

        if (Value.isValid(value)) {
            processSearchPropertyValue(searchProperty, value);
            conditionBuffer.append((value instanceof Collection) ? getInclusionOperator(searchProperty) :
                    " " + searchProperty.compareOperator().getValue() + " ")
                    .append(":").append(parameterCounter.next());
        } else {
            if (!joinBuffer.isReferencedInJoin(searchProperty.value().trim())) {
                conditionBuffer.append(" c.");
            }
            conditionBuffer.append(searchProperty.value()).append(" IS NULL");
        }
    }

    private void processLike(SearchProperty searchProperty, Object value) {

        processSearchPropertyValue(searchProperty, value);
        conditionBuffer.append(" LIKE ")
                .append(":").append(parameterCounter.next());
    }

    private void processSearchPropertyValue(SearchProperty searchProperty, Object value) {

        processPreCondition(searchProperty.preCondition());
        boolean isDate = isDateValue(value);
        if (searchProperty.concat().value().length == 0) {
            processNoConcat(searchProperty, isDate);
        } else {
            processConcat(searchProperty);
        }
    }

    private void processNoConcat(SearchProperty searchProperty, boolean isDate) {

        if (!searchProperty.isLike()) {
            processNotIsLike(searchProperty, isDate);
        } else {
            proccessLike(searchProperty, isDate);
        }
    }

    private void processConcat(SearchProperty searchProperty) {

        conditionBuffer.append("UPPER(CONCAT(");
        Stream.of(searchProperty.concat().value()).forEach(value -> {
            if (!joinBuffer.isReferencedInJoin(value.trim())) {
                conditionBuffer.append(" c.");
            }
            conditionBuffer.append(value.trim()).append(",' ',");
        });
        conditionBuffer.setLength(conditionBuffer.length() - 1);
        conditionBuffer.append("))");
    }

    private void processNotIsLike(SearchProperty searchProperty, boolean isDate) {

        if (isDate) {
            conditionBuffer.append("date_trunc('day',");
        }
        if (!joinBuffer.isReferencedInJoin(searchProperty)) {
            conditionBuffer.append(" c.");
        }
        conditionBuffer.append(searchProperty.value().trim());
        if (isDate) {
            conditionBuffer.append(")");
        }
    }

    private void proccessLike(SearchProperty searchProperty, boolean isDate) {

        conditionBuffer.append(" UPPER(");
        if (!joinBuffer.isReferencedInJoin(searchProperty)) {
            conditionBuffer.append(" c.");
        }
        conditionBuffer.append(searchProperty.value().trim()).append(")");
    }

    private void processPreCondition(PreCondition preCondition) {

        if (!preCondition.condition().isEmpty()) {
            conditionBuffer.append(preCondition.condition()).append(" " + preCondition.conditionType().name() + " ");
        }
    }

    private boolean isDateValue(Object value) {

        return (value instanceof Date) && !(value instanceof Timestamp);
    }

    public void prependAndOr() {

        StringBuffer newConditionBuffer = new StringBuffer();
        conditionBuffer = newConditionBuffer.append(" ").append(conditionType.name()).append(" ")
                .append(conditionBuffer);
    }

    private String getInclusionOperator(SearchProperty searchProperty) {

        return " " + searchProperty.inclusionOperator().name() + " ";
    }

    public boolean isEmpty() {

        return conditionBuffer.length() == 0;
    }

    @Override
    public String toString() {

        return conditionBuffer.toString();
    }
}
