package util;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * DateUtil
 * ------------------------------------
 * Date & time related helpers
 */
public class DateUtil {

    private DateUtil() {
        // utility class
    }

    /**
     * Rule:
     * Ticket can be cancelled only if
     * journey is at least 24 hours ahead.
     */
    public static boolean canCancel(LocalDateTime journeyTime) {

        if (journeyTime == null) {
            return false;
        }

        long hours =
                Duration
                        .between(LocalDateTime.now(), journeyTime)
                        .toHours();

        return hours >= 24;
    }
}
