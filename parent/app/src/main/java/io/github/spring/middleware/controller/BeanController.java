package io.github.spring.middleware.controller;

import io.github.spring.middleware.scope.RefresahbleSingletonOnDemandScope;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping(BeanController.BASE_MAPPING)
@OpenAPIDefinition(info = @Info(title = "Bean Controller"))
public class BeanController {

    public static final String BASE_MAPPING = "/bean";
    @Autowired
    private ApplicationContext applicationContext;

    private RefresahbleSingletonOnDemandScope refresahbleSingletonOnDemandScope;

    @PostConstruct
    public void init() {

        refresahbleSingletonOnDemandScope = (RefresahbleSingletonOnDemandScope) ((DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory()).getRegisteredScope(
                "refreshable-singleton");
    }

    @GetMapping("/refresh")
    public void refresh(@RequestParam(value = "name") String name) {

        refresahbleSingletonOnDemandScope.refreshInstance(name);
    }

}
