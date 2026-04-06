package util;

import java.time.Duration;
import java.time.LocalDateTime;






public class DateUtil {

    private DateUtil() {
        
    }

    




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
