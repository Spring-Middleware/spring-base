package com.middleware.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class TimeZoneUtil {

    public static LocalDateTime atZoneTime(LocalDateTime dateTime, String timezone) {

        return Optional.ofNullable(timezone)
                .map(tz -> dateTime.atZone(ZoneId.of("UTC")).withZoneSameInstant(ZoneId.of(tz)).toLocalDateTime())
                .orElse(dateTime);
    }

    public static LocalTime atZoneTime(LocalTime time, String timezone) {

        LocalTime localTime = null;
        if (time != null) {
            localTime = Optional.ofNullable(timezone)
                    .map(tz -> LocalDate.now().atTime(time).atZone(ZoneId.of("UTC")).withZoneSameInstant(ZoneId.of(tz))
                            .toLocalTime())
                    .orElse(time);
        }
        return localTime;
    }

    public static LocalTime atZoneTime(String time, String timezone) {

        LocalTime localTime = null;
        if (time != null) {
            localTime = Optional.ofNullable(timezone)
                    .map(tz -> LocalDate.now().atTime(LocalTime.parse(time, DateTimeFormatter.ISO_LOCAL_TIME))
                            .atZone(ZoneId.of("UTC")).withZoneSameInstant(ZoneId.of(tz))
                            .toLocalTime())
                    .orElse(LocalTime.parse(time, DateTimeFormatter.ISO_LOCAL_TIME));
        }
        return localTime;
    }

}
