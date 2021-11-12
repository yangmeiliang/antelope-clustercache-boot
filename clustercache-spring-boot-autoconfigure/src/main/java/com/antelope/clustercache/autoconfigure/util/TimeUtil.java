package com.antelope.clustercache.autoconfigure.util;

import org.springframework.lang.NonNull;

import java.time.Duration;
import java.time.format.DateTimeParseException;

/**
 * @author yaml
 * @since 2021/7/1
 */
public final class TimeUtil {

    public static Duration simpleParse(@NonNull String time) {
        if (time.startsWith("-")) {
            return Duration.ofMillis(-1);
        }
        String timeLower = time.toLowerCase();
        if (timeLower.endsWith("ns")) {
            return Duration.ofNanos(parse(timeLower, 2));
        }
        if (timeLower.endsWith("ms")) {
            return Duration.ofMillis(parse(timeLower, 2));
        }
        if (timeLower.endsWith("s")) {
            return Duration.ofSeconds(parse(timeLower, 1));
        }
        if (timeLower.endsWith("m")) {
            return Duration.ofMinutes(parse(timeLower, 1));
        }
        if (timeLower.endsWith("h")) {
            return Duration.ofHours(parse(timeLower, 1));
        }
        if (timeLower.endsWith("d")) {
            return Duration.ofDays(parse(timeLower, 1));
        }
        throw new DateTimeParseException("Unable to parse " + time + " into duration", time, 0);
    }

    private static long parse(String value, int size) {
        return Long.parseLong(value.substring(0, value.length() - size));
    }
}
