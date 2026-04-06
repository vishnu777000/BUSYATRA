package util;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class UiLagMonitor {

    private static final AtomicBoolean INSTALLED = new AtomicBoolean(false);
    private static final long WARN_MS = Long.getLong("busyatra.ui.lag.warn.ms", 150L);
    private static final boolean ENABLED =
            Boolean.parseBoolean(System.getProperty("busyatra.ui.lag.monitor", "true"));

    private UiLagMonitor() {
    }

    public static void install() {
        if (!ENABLED || GraphicsEnvironment.isHeadless()) {
            return;
        }
        if (!INSTALLED.compareAndSet(false, true)) {
            return;
        }

        Toolkit.getDefaultToolkit().getSystemEventQueue().push(new EventQueue() {
            @Override
            protected void dispatchEvent(AWTEvent event) {
                long start = System.nanoTime();
                try {
                    super.dispatchEvent(event);
                } finally {
                    long elapsedMs = Math.max(0L, (System.nanoTime() - start) / 1_000_000L);
                    if (elapsedMs >= WARN_MS) {
                        System.out.println("[Lag] EDT " + describe(event) + " took " + elapsedMs + "ms");
                    }
                }
            }
        });
    }

    private static String describe(AWTEvent event) {
        if (event == null) {
            return "unknown-event";
        }

        Object source = event.getSource();
        String sourceName = source == null ? "unknown" : source.getClass().getSimpleName();
        return event.getClass().getSimpleName() + "@" + sourceName + "#" + event.getID();
    }
}
