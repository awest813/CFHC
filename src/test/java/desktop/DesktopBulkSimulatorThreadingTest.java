package desktop;

import org.junit.Test;

import javax.swing.SwingUtilities;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the bulk-sim contract: mutation callbacks intended for the EDT
 * run on the event dispatch thread when invoked via invokeAndWait.
 */
public class DesktopBulkSimulatorThreadingTest {

    @Test
    public void invokeAndWait_runsOnEdtFromBackgroundThread() throws Exception {
        AtomicBoolean onEdt = new AtomicBoolean(false);
        AtomicInteger runs = new AtomicInteger();
        Thread worker = new Thread(() -> {
            try {
                SwingUtilities.invokeAndWait(() -> {
                    onEdt.set(SwingUtilities.isEventDispatchThread());
                    runs.incrementAndGet();
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, "bulk-sim-test-worker");
        worker.start();
        worker.join(5000);
        assertTrue(!worker.isAlive());
        assertEquals(1, runs.get());
        assertTrue(onEdt.get());
    }
}
