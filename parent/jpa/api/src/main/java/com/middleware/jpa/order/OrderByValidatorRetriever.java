package com.middleware.jpa.order;

import com.middleware.jpa.annotations.OrderByValid;
import com.middleware.sort.SortedSearch;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;


public class OrderByValidatorRetriever implements ConstraintValidator<OrderByValid, SortedSearch>, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void initialize(OrderByValid searchValid) {
    }

    public boolean isValid(SortedSearch sortedSearch, ConstraintValidatorContext context) {
        OrderByValidator orderByValidator = applicationContext.getBean(OrderByValidator.class);
        return orderByValidator.orderByValid(sortedSearch, context);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
