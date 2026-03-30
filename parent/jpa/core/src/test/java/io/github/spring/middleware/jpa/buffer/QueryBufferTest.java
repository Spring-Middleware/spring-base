package io.github.spring.middleware.jpa.buffer;

import io.github.spring.middleware.jpa.annotations.Join;
import io.github.spring.middleware.jpa.annotations.PreCondition;
import io.github.spring.middleware.jpa.annotations.SearchForClass;
import io.github.spring.middleware.jpa.annotations.SearchProperty;
import io.github.spring.middleware.jpa.annotations.SubSearch;
import io.github.spring.middleware.jpa.buffer.builder.SearchPropertiesConditionBufferBuilderImpl;
import io.github.spring.middleware.jpa.buffer.builder.SearchPropertyConditionBufferBuilderImpl;
import io.github.spring.middleware.jpa.buffer.builder.SearchPropertyExistsConditionBufferBuilderImpl;
import io.github.spring.middleware.jpa.buffer.builder.SubSearchConditionBufferBuilderImpl;
import io.github.spring.middleware.jpa.buffer.factory.ConditionBufferBuilderFactory;
import io.github.spring.middleware.jpa.buffer.testentities.Catalog;
import io.github.spring.middleware.jpa.buffer.testentities.Product;
import io.github.spring.middleware.jpa.order.OrderBy;
import io.github.spring.middleware.jpa.search.Search;
import io.github.spring.middleware.jpa.types.CompareOperator;
import io.github.spring.middleware.jpa.types.ConditionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class QueryBufferTest {

    private static final Logger log = LoggerFactory.getLogger(QueryBufferTest.class);

    @BeforeEach
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void setupApplicationContext() {
        ApplicationContext mockCtx = Mockito.mock(ApplicationContext.class);
        // return appropriate builder implementation based on requested class
        when(mockCtx.getBean(any(Class.class))).thenAnswer(invocation -> {
            Class<?> requested = invocation.getArgument(0);
            log.debug("[MOCK getBean requested] -> {}", requested.getName());
            if (requested.equals(SearchPropertyConditionBufferBuilderImpl.class)
                    || requested.equals(SearchPropertyConditionBufferBuilder.class)) {
                return new SearchPropertyConditionBufferBuilderImpl();
            }
            if (requested.equals(SubSearchConditionBufferBuilder.class)) {
                return new SubSearchConditionBufferBuilderImpl();
            }
            if (requested.equals(SearchPropertiesConditionBufferBuilder.class)) {
                return new SearchPropertiesConditionBufferBuilderImpl();
            }
            if (requested.equals(SearchPropertyExistsConditionBufferBuilder.class)) {
                return new SearchPropertyExistsConditionBufferBuilderImpl();
            }
            // fallback: try instantiate
            return requested.getDeclaredConstructor().newInstance();
        });

        // set the static applicationContext inside the factory
        new ConditionBufferBuilderFactory().setApplicationContext(mockCtx);
    }

    @Test
    public void queryBuffer_noSearch_buildsSelectFrom() {
        QueryBufferParameters<Catalog, CatalogSearchWithValues> params = new QueryBufferParameters<>(Catalog.class, new OrderBy());
        QueryBuffer<Catalog, CatalogSearchWithValues> qb = new QueryBuffer<>(params);
        String q = qb.toString();
        String up = q.toUpperCase();
        assertTrue(up.contains("SELECT") && up.contains("FROM"));
        assertTrue(up.contains("CATALOG"));
    }

    @Test
    public void selectBuffer_distinct_whenSearchForClass() {
        CatalogSearchDistinct s = new CatalogSearchDistinct();
        QueryBufferParameters<Catalog, Search> params = new QueryBufferParameters<>(s, Catalog.class, new OrderBy(), false);
        QueryBuffer<Catalog, Search> qb = new QueryBuffer<>(params);
        String q = qb.toString();
        assertTrue(q.toUpperCase().startsWith("SELECT DISTINCT"));
    }

    @Test
    public void joinBuffer_appendsConfiguredJoinClause() {
        CatalogSearchWithJoin s = new CatalogSearchWithJoin();
        QueryBufferParameters<Catalog, Search> params = new QueryBufferParameters<>(s, Catalog.class, new OrderBy(), false);
        QueryBuffer<Catalog, Search> qb = new QueryBuffer<>(params);
        String q = qb.toString();
        log.debug("[JOIN TEST QUERY] -> {}", q);
        // join clause should be present
        assertTrue(q.contains("JOIN c.products p"));
    }

    @Test
    public void whereBuffer_generatesConditionsForScalarAndCollectionProperties() {
        CatalogSearchWithValues s = new CatalogSearchWithValues();
        QueryBufferParameters<Catalog, Search> params = new QueryBufferParameters<>(s, Catalog.class, new OrderBy(), false);
        QueryBuffer<Catalog, Search> qb = new QueryBuffer<>(params);
        String q = qb.toString();
        log.debug("[DEBUG QUERY] {}", q);
        // should contain WHERE and at least one property name
        assertTrue(q.toUpperCase().contains("WHERE"));
        assertTrue(q.contains("c.name"));
        assertTrue(q.contains("c.products IN :param1"));
    }

    @Test
    public void orderByBuffer_buildsOrderByClause() {
        OrderBy orderBy = new OrderBy();
        orderBy.setOrderBy(List.of("field1", "field2"));
        orderBy.setOrderType(io.github.spring.middleware.jpa.types.OrderType.DESC);

        QueryBufferParameters<Object, Search> params = new QueryBufferParameters<>(Object.class, orderBy);
        QueryBuffer<Object, Search> qb = new QueryBuffer<>(params);
        String q = qb.toString();
        assertTrue(q.contains("ORDER BY"));
        assertTrue(q.contains("c.field1"));
        assertTrue(q.contains("c.field2"));
        assertTrue(q.contains("DESC"));
    }

    @Test
    public void searchPropertiesBuilder_handlesMultipleProperties_withOr() {
        SearchPropertiesSearch s = new SearchPropertiesSearch();
        QueryBufferParameters<Catalog, Search> params = new QueryBufferParameters<>(s, Catalog.class, new OrderBy(), false);
        QueryBuffer<Catalog, Search> qb = new QueryBuffer<>(params);
        String q = qb.toString();
        // should contain WHERE and an OR between properties
        assertTrue(q.toUpperCase().contains("WHERE"));
        assertTrue(q.contains("name") || q.contains("description"));
        assertTrue(q.contains(" OR ") || q.contains(" or "));
    }

    @Test
    public void searchPropertyExistsBuilder_generatesIsNotEmptyOrIsEmpty() {
        SearchPropertyExistsSearch s = new SearchPropertyExistsSearch();
        QueryBufferParameters<Catalog, Search> params = new QueryBufferParameters<>(s, Catalog.class, new OrderBy(), false);
        QueryBuffer<Catalog, Search> qb = new QueryBuffer<>(params);
        String q = qb.toString();
        // should contain is not empty (exists true)
        assertTrue(q.toLowerCase().contains("is not empty"));
    }

    @Test
    public void subSearchBuilder_embedsNestedSearchCondition() {
        SubSearchWrapper s = new SubSearchWrapper();
        QueryBufferParameters<Catalog, Search> params = new QueryBufferParameters<>(s, Catalog.class, new OrderBy(), false);
        QueryBuffer<Catalog, Search> qb = new QueryBuffer<>(params);
        String q = qb.toString();
        assertTrue(q.toUpperCase().contains("WHERE"));

        // Subsearch (OR interno)
        assertTrue(q.contains("c.description = :param1"));
        assertTrue(q.contains("c.owner = :param2"));
        assertTrue(q.contains(" OR "));

        // Condición principal (AND externo)
        assertTrue(q.contains("c.name = :param0"));
        assertTrue(q.contains(" AND "));
    }

    @Test
    public void joinBuffer_leftJoin_appended() {
        CatalogSearchWithLeftJoin s = new CatalogSearchWithLeftJoin();
        QueryBufferParameters<Catalog, Search> params = new QueryBufferParameters<>(s, Catalog.class, new OrderBy(), false);
        QueryBuffer<Catalog, Search> qb = new QueryBuffer<>(params);
        String q = qb.toString();
        assertTrue(q.contains("LEFT JOIN"), "Expected LEFT JOIN in query but was: " + q);
    }

    @Test
    public void joinBuffer_rightJoin_appended() {
        CatalogSearchWithRightJoin s = new CatalogSearchWithRightJoin();
        QueryBufferParameters<Catalog, Search> params = new QueryBufferParameters<>(s, Catalog.class, new OrderBy(), false);
        QueryBuffer<Catalog, Search> qb = new QueryBuffer<>(params);
        String q = qb.toString();
        assertTrue(q.contains("RIGHT JOIN"), "Expected RIGHT JOIN in query but was: " + q);
    }

    @Test
    public void joinBuffer_fetchFlag_appended() {
        CatalogSearchWithFetchJoin s = new CatalogSearchWithFetchJoin();
        QueryBufferParameters<Catalog, Search> params = new QueryBufferParameters<>(s, Catalog.class, new OrderBy(), false);
        QueryBuffer<Catalog, Search> qb = new QueryBuffer<>(params);
        String q = qb.toString();
        assertTrue(q.contains("FETCH"), "Expected FETCH in query but was: " + q);
    }

    @Test
    public void joinBuffer_leftFetch_combination() {
        CatalogSearchWithLeftFetch s = new CatalogSearchWithLeftFetch();
        QueryBufferParameters<Catalog, Search> params = new QueryBufferParameters<>(s, Catalog.class, new OrderBy(), false);
        QueryBuffer<Catalog, Search> qb = new QueryBuffer<>(params);
        String q = qb.toString();
        // must contain LEFT JOIN and FETCH and the join value
        assertTrue(q.contains("LEFT JOIN"));
        assertTrue(q.contains("FETCH"));
        assertTrue(q.contains("c.products p"));
    }

    @Test
    public void joinBuffer_rightFetch_combination() {
        CatalogSearchWithRightFetch s = new CatalogSearchWithRightFetch();
        QueryBufferParameters<Catalog, Search> params = new QueryBufferParameters<>(s, Catalog.class, new OrderBy(), false);
        QueryBuffer<Catalog, Search> qb = new QueryBuffer<>(params);
        String q = qb.toString();
        // must contain RIGHT JOIN and FETCH and the join value
        assertTrue(q.contains("RIGHT JOIN"));
        assertTrue(q.contains("FETCH"));
        assertTrue(q.contains("c.products p"));
    }

    @Test
    public void joinBuffer_sameCollectionWithDifferentAliases_generatesIndependentJoins() {
        CatalogSearchWithDuplicateJoins s = new CatalogSearchWithDuplicateJoins();
        QueryBufferParameters<Catalog, Search> params = new QueryBufferParameters<>(s, Catalog.class, new OrderBy(), false);
        QueryBuffer<Catalog, Search> qb = new QueryBuffer<>(params);
        String q = qb.toString();
        assertTrue(q.contains("SELECT DISTINCT c"));
        assertTrue(q.contains("JOIN c.products p1"));
        assertTrue(q.contains("JOIN c.products p2"));
        assertTrue(q.contains("p1 IN :param0"));
        assertTrue(q.contains("p2 IN :param1"));
    }

    @Test
    public void joinBuffer_multipleDifferentJoins_allPresent() {
        CatalogSearchWithTwoJoins s = new CatalogSearchWithTwoJoins();
        QueryBufferParameters<Catalog, Search> params = new QueryBufferParameters<>(s, Catalog.class, new OrderBy(), false);
        QueryBuffer<Catalog, Search> qb = new QueryBuffer<>(params);
        String q = qb.toString();
        assertTrue(q.contains("c.products p"));
        assertTrue(q.contains("c.tags t"));
    }

    @Test
    public void preCondition_isPrependedToCondition() {
        PreConditionSearch s = new PreConditionSearch();
        QueryBufferParameters<Catalog, Search> params = new QueryBufferParameters<>(s, Catalog.class, new OrderBy(), false);
        QueryBuffer<Catalog, Search> qb = new QueryBuffer<>(params);
        String q = qb.toString();
        // PreCondition should be present followed by OR (conditionType)
        assertTrue(q.contains("c.status = 'ACTIVE'") || q.toUpperCase().contains("C.STATUS = 'ACTIVE'"));
    }

    @Test
    public void concat_buildsUpperConcat_expression() {
        ConcatSearch s = new ConcatSearch();
        QueryBufferParameters<Catalog, Search> params = new QueryBufferParameters<>(s, Catalog.class, new OrderBy(), false);
        QueryBuffer<Catalog, Search> qb = new QueryBuffer<>(params);
        String q = qb.toString();
        // should contain UPPER(CONCAT and both property names
        assertTrue(q.toUpperCase().contains("UPPER(CONCAT") || q.toUpperCase().contains("CONCAT("));
        assertTrue(q.toLowerCase().contains("name") && q.toLowerCase().contains("description"));
    }

    @Test
    public void inclusionOperator_notIn_usedWhenCollection() {
        InclusionNotInSearch s = new InclusionNotInSearch();
        QueryBufferParameters<Catalog, Search> params = new QueryBufferParameters<>(s, Catalog.class, new OrderBy(), false);
        QueryBuffer<Catalog, Search> qb = new QueryBuffer<>(params);
        String q = qb.toString();
        // inclusion operator name should appear
        String up = q.toUpperCase();
        assertTrue(up.contains("NOT IN") || up.contains("NOT_IN") || up.contains("NOTIN") );
    }

    @Test
    public void compareOperator_greaterThan_generatesGreaterThanCondition() {
        CompareOperatorSearch s = new CompareOperatorSearch();
        QueryBufferParameters<Catalog, Search> params = new QueryBufferParameters<>(s, Catalog.class, new OrderBy(), false);
        QueryBuffer<Catalog, Search> qb = new QueryBuffer<>(params);
        String q = qb.toString();
        // check for '>' symbol present in condition
        assertTrue(q.contains("c.price > :param0"));
    }

    @Test
    public void searchForNull_appendsIsNull_whenValueNull() {
        SearchForNullSearch s = new SearchForNullSearch();
        QueryBufferParameters<Catalog, Search> params = new QueryBufferParameters<>(s, Catalog.class, new OrderBy(), false);
        QueryBuffer<Catalog, Search> qb = new QueryBuffer<>(params);
        String q = qb.toString();
        assertTrue(q.toUpperCase().contains("IS NULL") || q.toUpperCase().contains("ISNULL"));
    }

    // Helper to build query string from a Search instance and optional entity class
    @SuppressWarnings({"rawtypes", "unchecked"})
    private String buildQuery(Search search, Class<?> entityClass) {
        QueryBufferParameters params;
        if (search == null) {
            Class<?> cls = entityClass == null ? String.class : entityClass;
            params = new QueryBufferParameters(cls, new OrderBy());
        } else {
            Class<?> cls = entityClass == null ? String.class : entityClass;
            params = new QueryBufferParameters(search, cls, new OrderBy(), false);
        }
        QueryBuffer qb = new QueryBuffer(params);
        return qb.toString();
    }

    @Test
    public void clauseOrder_from_join_where_orderBy() {
        // create a search that will generate JOIN and WHERE
        CatalogSearchWithLeftFetch s = new CatalogSearchWithLeftFetch();
        OrderBy orderBy = new OrderBy();
        orderBy.setOrderBy(List.of("name"));
        QueryBufferParameters<Catalog, Search> params = new QueryBufferParameters<>(s, Catalog.class, orderBy, false);
        QueryBuffer<Catalog, Search> qb = new QueryBuffer<>(params);
        String q = qb.toString();
        String up = q.toUpperCase();
        int idxFrom = up.indexOf("FROM");
        int idxJoin = up.indexOf("JOIN");
        int idxWhere = up.indexOf("WHERE");
        int idxOrder = up.indexOf("ORDER BY");
        assertTrue(idxFrom >= 0, STR."FROM missing: \{q}");
        assertTrue(idxJoin > idxFrom, STR."JOIN must come after FROM: \{q}");
        assertTrue(idxWhere > idxJoin, STR."WHERE must come after JOIN: \{q}");
        assertTrue(idxOrder > idxWhere, STR."ORDER BY must come after WHERE: \{q}");
    }

    @Test
    public void preCondition_and_prependsConditionWithAnd() {
        // OR case already tested; add AND case and assert contains AND
        PreConditionAndSearch s = new PreConditionAndSearch();
        String q = buildQuery(s, Catalog.class);
        assertTrue(q.contains("c.status = 'ACTIVE' AND"));
    }

    @Test
    public void concat_empty_doesNotGenerateConcatExpression() {
        String q = buildQuery(new EmptyConcatSearch(), Catalog.class);
        // Ensure no UPPER(CONCAT appears when concat list is empty
        String up = q.toUpperCase();
        assertFalse(up.contains("UPPER(CONCAT("));
    }

    @Test
    public void inclusionOperator_in_generatesInClause() {
        String q = buildQuery(new InclusionInSearch(), Catalog.class);
        String up = q.toUpperCase();
        assertTrue(up.contains("C.TAGS IN :PARAM0"));
    }

    @Test
    public void searchForNull_nonNull_doesNotProduceIsNull() {
        String q = buildQuery(new SearchForNullNonNull(), Catalog.class);
        assertFalse(q.toUpperCase().contains("IS NULL"));
    }

    // Simple test entity classes to make FROM clauses cleaner in debugging
    public static class CatalogEntity {
        private String id;
        private String name;
    }

    public static class ProductEntity {
        private String id;
        private String name;
    }

    // Static search helper classes to avoid local-class introspection issues
    @SearchForClass(value = Catalog.class, distinct = true)
    public static class CatalogSearchDistinct implements Search {
    }

    public static class CatalogSearchWithJoin implements Search {

        @SearchProperty(value = "products", isLike = false, join = @io.github.spring.middleware.jpa.annotations.Join(value = " JOIN c.products p "))
        private List<Product> products = List.of(new Product("p1", "prod1"));

        public List<Product> getProducts() { return products; }
    }

    public static class CatalogSearchWithValues implements Search {

        @SearchProperty(value = "name", isLike = false)
        private String name = "john";

        @SearchProperty(value = "products", isLike = false)
        private List<Product> products = List.of(new Product("p1","prod1"));

        public String getName() { return name; }
        public List<Product> getProducts() { return products; }
    }

    // New helper search classes to exercise specific builders
    public static class SearchPropertiesSearch implements Search {
        @io.github.spring.middleware.jpa.annotations.SearchProperties({
                @SearchProperty(value = "name", isLike = false),
                @SearchProperty(value = "description", isLike = false)
        })
        private String q = "foo";

        public String getQ() { return q; }
    }

    public static class SearchPropertyExistsSearch implements Search {
        @io.github.spring.middleware.jpa.annotations.SearchPropertyExists(value = "products")
        private Boolean productsExists = true;

        public Boolean getProductsExists() { return productsExists; }
    }

    public static class SubSearchInner implements Search {

        @SearchProperty(value = "description", conditionType = ConditionType.OR)
        private String description = "nested";

        @SearchProperty(value = "owner", conditionType = ConditionType.OR)
        private String owner = "owner1";

        public String getDescription() { return description; }
        public String getOwner() { return owner; }
    }

    @SearchForClass(value = Catalog.class)
    public static class SubSearchWrapper implements Search {

        @SearchProperty("name")
        private String name = "outer";

        @SubSearch
        private SubSearchInner inner = new SubSearchInner();

        public String getName() { return name; }

        public SubSearchInner getInner() { return inner; }

    }

    public static class CatalogSearchWithLeftJoin implements Search {
        @SearchProperty(value = "products", isLike = false, join = @io.github.spring.middleware.jpa.annotations.Join(value = "c.products p", left = true))
        private List<Product> products = List.of(new Product("p1","prod1"));
        public List<Product> getProducts() { return products; }
    }

    public static class CatalogSearchWithRightJoin implements Search {
        @SearchProperty(value = "products", isLike = false, join = @io.github.spring.middleware.jpa.annotations.Join(value = "c.products p", right = true))
        private List<Product> products = List.of(new Product("p1","prod1"));
        public List<Product> getProducts() { return products; }
    }

    public static class CatalogSearchWithFetchJoin implements Search {
        @SearchProperty(value = "products", isLike = false, join = @io.github.spring.middleware.jpa.annotations.Join(value = "c.products p", fetch = true))
        private List<Product> products = List.of(new Product("p1","prod1"));
        public List<Product> getProducts() { return products; }
    }

    public static class CatalogSearchWithLeftFetch implements Search {
        @SearchProperty(value = "products", isLike = false, join = @io.github.spring.middleware.jpa.annotations.Join(value = "c.products p", left = true, fetch = true))
        private List<Product> products = List.of(new Product("p1","prod1"));
        public List<Product> getProducts() { return products; }
    }

    public static class CatalogSearchWithRightFetch implements Search {
        @SearchProperty(value = "products", isLike = false, join = @io.github.spring.middleware.jpa.annotations.Join(value = "c.products p", right = true, fetch = true))
        private List<Product> products = List.of(new Product("p1","prod1"));
        public List<Product> getProducts() { return products; }
    }

    @SearchForClass(value = Catalog.class, distinct = true)
    public static class CatalogSearchWithDuplicateJoins implements Search {
        @SearchProperty(value = "p1", isLike = false, join = @Join(value = "c.products p1"))
        private List<Product> products1 = List.of(new Product("p1","prod1"));

        @SearchProperty(value = "p2", isLike = false, join = @Join(value = "c.products p2"))
        private List<Product> products2 = List.of(new Product("p2","prod2"));

        public List<Product> getProducts1() { return products1; }
        public List<Product> getProducts2() { return products2; }
    }

    public static class CatalogSearchWithTwoJoins implements Search {
        @SearchProperty(value = "products", isLike = false, join = @io.github.spring.middleware.jpa.annotations.Join(value = "c.products p"))
        private List<Product> products = List.of(new Product("p1","prod1"));

        @SearchProperty(value = "tags", isLike = false, join = @io.github.spring.middleware.jpa.annotations.Join(value = "c.tags t"))
        private List<String> tags = List.of("t1");

        public List<Product> getProducts() { return products; }
        public List<String> getTags() { return tags; }
    }

    // Helper searches
     public static class PreConditionSearch implements Search {
         @SearchProperty(value = "name", isLike = false,
                 preCondition = @PreCondition(condition = "c.status = 'ACTIVE'", conditionType = io.github.spring.middleware.jpa.types.ConditionType.OR))
         private String name = "john";
         public String getName() { return name; }
     }

     public static class ConcatSearch implements Search {
         @SearchProperty(value = "name", isLike = true, concat = @io.github.spring.middleware.jpa.annotations.Concat(value = {"name","description"}))
         private String name = "john";
         public String getName() { return name; }
     }

     // Moved from method-local classes to static nested classes to allow reflection to access
     public static class PreConditionAndSearch implements Search {
         @SearchProperty(value = "name", isLike = false,
                 preCondition = @PreCondition(condition = "c.status = 'ACTIVE'", conditionType = io.github.spring.middleware.jpa.types.ConditionType.AND))
         private String name = "john";
         public String getName() { return name; }
     }

     public static class EmptyConcatSearch implements Search {
         @io.github.spring.middleware.jpa.annotations.SearchProperty(value = "name", isLike = true, concat = @io.github.spring.middleware.jpa.annotations.Concat(value = {}))
         private String name = "john";
         public String getName() { return name; }
     }

     public static class InclusionInSearch implements Search {
         @SearchProperty(value = "tags", isLike = false, inclusionOperator = io.github.spring.middleware.jpa.types.IncusionOperator.IN)
         private java.util.List<String> tags = java.util.List.of("a","b");
         public java.util.List<String> getTags() { return tags; }
     }

     public static class SearchForNullNonNull implements Search {
         @SearchProperty(value = "description", isLike = false, searchForNull = true)
         private String description = "not-null";
         public String getDescription() { return description; }
     }

     public static class InclusionNotInSearch implements Search {
         @SearchProperty(value = "tags", isLike = false, inclusionOperator = io.github.spring.middleware.jpa.types.IncusionOperator.NOT_IN)
         private java.util.List<String> tags = java.util.List.of("a","b");
         public java.util.List<String> getTags() { return tags; }
     }

    public static class CompareOperatorSearch implements Search {
        @SearchProperty(value = "price", isLike = false, compareOperator = CompareOperator.GREATER_THAN)
        private Integer price = 10;
        public Integer getPrice() { return price; }
    }

    public static class SearchForNullSearch implements Search {
        @SearchProperty(value = "description", isLike = false, searchForNull = true)
        private String description = null;
        public String getDescription() { return description; }
    }

}
