package io.github.spring.middleware.jpa.order;

import io.github.spring.middleware.jpa.annotations.SearchForClass;
import io.github.spring.middleware.sort.SortedSearch;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OrderByValidatorImpl implements OrderByValidator {

    private ArrayList<String> valids = new ArrayList<>();
    private Logger logger = Logger.getLogger(OrderByValidatorImpl.class);

    private Map<Class<?>, List<PropertyDescriptor>> cacheValids = Collections.synchronizedMap(new HashMap<>());


    @Override
    public boolean orderByValid(SortedSearch sortedSearch, ConstraintValidatorContext context) {

        if (sortedSearch == null)
            return true;
        Class<?> clazz = getClassForSearch(sortedSearch);
        if (clazz == null)
            return true;
        List<PropertyDescriptor> valids = null;
        return Optional.ofNullable(sortedSearch.getSortCriteria()).map(sortCriteria -> {
            if (!CollectionUtils.emptyIfNull(sortCriteria.getProperties()).isEmpty() &&
                    sortCriteria.getProperties().stream().allMatch(order -> isValid(order, getPropertyDescriptors(clazz)))) {
                return false;
            }else{
                return true;
            }
        }).orElse(true);
    }

    private List<PropertyDescriptor> getPropertyDescriptors(Class clazz) {

        List<PropertyDescriptor> valids = cacheValids.get(clazz);
        Class originalClass = clazz;
        if (valids == null) {
            valids = new ArrayList<>();
            while (clazz != null) {
                List<PropertyDescriptor> validPartial = new ArrayList<>();
                try {
                    validPartial = Arrays.asList(Introspector.getBeanInfo(clazz).getPropertyDescriptors()).stream()
                            .collect(Collectors.toList());
                } catch (Exception ex) {
                    logger.error("Can't instrospect " + clazz);
                }
                valids.addAll(validPartial);
                clazz = clazz.getSuperclass();
            }
            cacheValids.put(originalClass, valids);
        }
        return valids;
    }

    private boolean isValid(String order, List<PropertyDescriptor> valids) {

        String[] orders = order.split("\\.");
        boolean isValid = valids.stream().map(PropertyDescriptor::getName).anyMatch(valid -> orders[0].equals(valid));
        if (orders.length > 1 && isValid) {
            PropertyDescriptor propertyDescriptor = valids.stream().filter(pD -> pD.getName().equals(orders[0]))
                    .findFirst().orElse(null);
            if (propertyDescriptor != null) {
                try {
                    return isValid(order.substring(order.indexOf(".") + 1),
                            getPropertyDescriptors(propertyDescriptor.getPropertyType()));
                } catch (Exception ex) {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return isValid;
        }
    }

    private Class<?> getClassForSearch(SortedSearch search) {

        if (search.getClass().isAnnotationPresent(SearchForClass.class)) {
            return search.getClass().getAnnotation(SearchForClass.class).value();
        } else {
            return null;
        }
    }

}
