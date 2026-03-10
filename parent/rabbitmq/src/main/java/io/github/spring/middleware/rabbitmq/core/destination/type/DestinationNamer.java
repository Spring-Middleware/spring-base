package io.github.spring.middleware.rabbitmq.core.destination.type;

import io.github.spring.middleware.rabbitmq.annotations.JmsDestination;
import io.github.spring.middleware.rabbitmq.core.ApplicationContextProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class DestinationNamer  {

    private static Logger logger = LoggerFactory.getLogger(DestinationNamer.class);

    public static String getDestinationSuffixName(JmsDestination jmsDestination) {

        DestinationSuffix destinationSuffix = Optional.ofNullable(jmsDestination.clazzSuffix()).map(c -> {
            DestinationSuffix desSuf = null;
            try {
                ApplicationContext applicationContext = ApplicationContextProvider.getApplicationContext();
                if (applicationContext != null) {
                    desSuf = applicationContext.getBean(c);
                } else {
                    desSuf = c.newInstance();
                }
            } catch (Exception ex) {
                logger.error("Error retrieving destination suffix name", ex);
            }
            return desSuf;
        }).orElseGet(() -> (() -> null));
        return STR."\{jmsDestination.name()}\{destinationSuffix.version() == null ? "" : STR."-\{destinationSuffix.version()}"}";
    }

    public static String getExchangeSuffixName(JmsDestination jmsDestination) {

        DestinationSuffix destinationSuffix = Optional.ofNullable(jmsDestination.clazzSuffix()).map(c -> {
            DestinationSuffix desSuf = null;
            try {
                desSuf = c.newInstance();
            } catch (Exception ex) {
                logger.error("Error retrieving exchange suffix name", ex);
            }
            return desSuf;
        }).orElse(null);
        return jmsDestination.exchange() +
                (destinationSuffix.version() == null ? "" : "-" + destinationSuffix.version());
    }

}
