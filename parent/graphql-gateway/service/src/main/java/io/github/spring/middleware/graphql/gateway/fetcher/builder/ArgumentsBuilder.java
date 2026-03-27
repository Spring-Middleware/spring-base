package io.github.spring.middleware.graphql.gateway.fetcher.builder;

import java.util.Map;
import java.util.stream.Collectors;

public class ArgumentsBuilder extends CommonBuilder {


     public void appendArguments(Map<String, Object> arguments) {
         if (arguments.isEmpty()) {
             return;
         }

         builder.append("(");
         String argsAsVariables = arguments.keySet().stream()
                 .map(argumentName -> STR."\{argumentName}: $\{argumentName}")
                 .collect(Collectors.joining(", "));
         builder.append(argsAsVariables).append(")");
    }

}
