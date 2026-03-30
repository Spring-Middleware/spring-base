package io.github.spring.middleware.jpa.repository;

import io.github.spring.middleware.data.Pagination;
import io.github.spring.middleware.jpa.annotations.SearchFilter;
import io.github.spring.middleware.jpa.filter.FilterDB;
import io.github.spring.middleware.jpa.search.Search;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SearchRepositoryImplTest {

    @Mock
    private EntityManager entityManager;

    @Test
    public void countBySearch_noFilters_returnsSingleResult() throws Exception {
        SimpleSearch search = new SimpleSearch();

        Query q = mock(Query.class);
        when(q.getSingleResult()).thenReturn(123L);
        when(entityManager.createQuery(anyString())).thenReturn(q);

        TestSearchRepository repo = new TestSearchRepository(entityManager);

        Long result = repo.countBySearch(search);
        assertEquals(Long.valueOf(123L), result);
    }

    @Test
    public void findBySearch_withFilters_appliesFilterAndInMemoryPagination() throws Exception {
        // create a search instance that carries a filter value
        FilterSearch search = new FilterSearch();
        search.setMyFilter("KEY");

        Query q = mock(Query.class);
        // return a list of strings where only one contains "KEY"
        List<String> data = new java.util.ArrayList<>(Arrays.asList("alpha", "beta", "contains-KEY-value", "gamma"));
        when(q.getResultList()).thenReturn(data);
        when(entityManager.createQuery(anyString())).thenReturn(q);

        TestSearchRepository repo = new TestSearchRepository(entityManager);

        Pagination pagination = Pagination.from(1, 2);
        List<?> results = repo.findBySearch(search, pagination);
        // MyFilter will filter only entries containing "KEY" then pagination is applied in-memory
        // since pageSize=2 and only one element matches, results size should be 1
        assertNotNull(results);
        assertEquals(1, results.size());
        assertTrue(results.get(0).toString().contains("KEY"));
    }

    @Test
    public void findBySearch_withPagination_andNoFilters_setsQueryPagination() throws Exception {
        SimpleSearch search = new SimpleSearch();

        // spy the query to verify pagination methods called
        Query spyQuery = mock(Query.class);
        when(spyQuery.getResultList()).thenReturn(new java.util.ArrayList<>(Arrays.asList("1", "2", "3"))); // return some results
        when(entityManager.createQuery(anyString())).thenReturn(spyQuery);

        // create a repo that uses the mocked entityManager
        TestSearchRepository repo2 = new TestSearchRepository(entityManager);

        Pagination pagination = Pagination.from(1, 2); // page 1, size 2 => firstResult=2
        List<?> results = repo2.findBySearch(search, pagination);

        // verify that pagination was applied on the query
        verify(spyQuery).setFirstResult(2);
        verify(spyQuery).setMaxResults(2);
        assertNotNull(results);
    }

    // --- helper test classes ---
    public static class TestSearchRepository extends SearchRepositoryImpl<Object, Search> {
        // expose protected method in base class signature for overrides in tests
        public TestSearchRepository(EntityManager em) {
            injectEntityManager(em);
        }

        private void injectEntityManager(EntityManager em) {
            try {
                Field field = SearchRepositoryImpl.class.getDeclaredField("entityManager");
                field.setAccessible(true);
                field.set(this, em);
            } catch (Exception ex) {
                throw new RuntimeException("Failed to inject EntityManager for test", ex);
            }
        }

        @Override
        protected Class getEntityClass() {
            return Object.class;
        }

    }

    public static class SimpleSearch implements Search {
    }

    public static class FilterSearch implements Search {

        @SearchFilter(filter = MyFilter.class)
        private String myFilter;

        public String getMyFilter() {
            return myFilter;
        }

        public void setMyFilter(String myFilter) {
            this.myFilter = myFilter;
        }
    }

    public static class MyFilter implements FilterDB<Object, Object> {

        @Override
        public void prepare(java.util.Collection<Object> entites, Object p) {
            // nothing to prepare
        }

        @Override
        public boolean apply(io.github.spring.middleware.jpa.annotations.SearchFilter searchFilter, Object t, Object p) {
            try {
                if (t == null || p == null) return false;
                return t.toString().contains(p.toString());
            } catch (Exception ex) {
                return false;
            }
        }
    }

}
