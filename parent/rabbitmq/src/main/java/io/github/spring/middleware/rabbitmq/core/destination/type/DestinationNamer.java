package io.github.spring.middleware.rabbitmq.core.destination.type;

import io.github.spring.middleware.rabbitmq.annotations.JmsDestination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.Optional;

public class DestinationNamer {

    private static Logger logger = LoggerFactory.getLogger(DestinationNamer.class);

    public static String getDestinationSuffixName(JmsDestination jmsDestination) {

        DestinationSuffix destinationSuffix = Optional.ofNullable(jmsDestination.clazzSuffix()).map(c -> {
            DestinationSuffix desSuf = null;
            try {
                desSuf = c.newInstance();
            } catch (Exception ex) {
                logger.error("Error retrieving destination suffix name", ex);
            }
            return desSuf;
        }).orElse(null);
        return jmsDestination.name() + (destinationSuffix.version() == null ? "" : "-" + destinationSuffix.version());
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
