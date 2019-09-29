package ru.sherb.prdispatcher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.platform.commons.util.ExceptionUtils;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author maksim
 * @since 24.09.2019
 */
class DryPrinterTest {

    @Test
    public void testPrintDocAfterDuration() throws InterruptedException {
        // Setup
        var printer = new DryPrinter();
        var duration = Duration.ofMillis(100);
        var doc = new MockDocument()
                .typeName("test doc")
                .printDuration(duration);

        // Expect
        assertAfterDuration(duration, () -> printer.print(doc));
    }

    @Test
    public void testCancelPrinting() throws InterruptedException {
        // Setup
        var printer = new DryPrinter();
        var duration = Duration.ofMillis(1000);
        var doc = new MockDocument()
                .typeName("test doc")
                .printDuration(duration);

        var barrier = new CountDownLatch(1);
        var thread = new Thread(() -> assertTimeout(duration, () -> {
            barrier.countDown();
            printer.print(doc);
        }));
        thread.start();
        barrier.await(100, TimeUnit.MILLISECONDS);

        // When
        var current = printer.stop();
        thread.join(100);

        // Then
        assertEquals(Thread.State.TERMINATED, thread.getState());
        assertEquals(doc, current);
    }

    /**
     * @see org.junit.jupiter.api.AssertTimeout#assertTimeout
     */
    private void assertAfterDuration(Duration duration, Executable executable) {
        long timeoutInMillis = duration.toMillis();
        long start = System.currentTimeMillis();
        try {
            executable.execute();
        } catch (Throwable ex) {
            ExceptionUtils.throwAsUncheckedException(ex);
        }

        long timeElapsed = System.currentTimeMillis() - start;
        if (timeElapsed < timeoutInMillis) {
            fail("execution less than planned duration " + timeoutInMillis + " ms by " + (timeElapsed - timeoutInMillis) + " ms");
        }
    }
}