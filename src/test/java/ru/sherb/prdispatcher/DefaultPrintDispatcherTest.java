package ru.sherb.prdispatcher;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Comparator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TransferQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author maksim
 * @since 23.09.2019
 */
class DefaultPrintDispatcherTest {

    @Test
    public void testPrintOneFile() throws InterruptedException {
        // Setup
        var printer = new MockPrinter();
        var printDispatcher = new DefaultPrintDispatcher(printer);
        var expectedDocument = new MockDocument().typeName("test document");

        // When
        printDispatcher.print(expectedDocument);

        // Then
        assertEquals(expectedDocument, printer.printedDocument());

        // Cleanup
        printDispatcher.stop();
    }

    @Test
    public void testPrintMultipleFiles() throws InterruptedException {
        // Setup
        var printer = new MockPrinter();
        var printDispatcher = new DefaultPrintDispatcher(printer);

        var documents = Stream
                .iterate(0, i -> i + 1)
                .limit(10)
                .map(i -> new MockDocument().typeName(String.valueOf(i)))
                .collect(Collectors.toList());

        // When
        documents.forEach(printDispatcher::print); // from 0 to 9

        // Then
        assertEquals(documents.get(0), printer.printedDocument());
        assertEquals(documents.get(1), printer.printedDocument());
        assertEquals(documents.get(2), printer.printedDocument());
        assertEquals(documents.get(3), printer.printedDocument());
        assertEquals(documents.get(4), printer.printedDocument());
        assertEquals(documents.get(5), printer.printedDocument());
        assertEquals(documents.get(6), printer.printedDocument());
        assertEquals(documents.get(7), printer.printedDocument());
        assertEquals(documents.get(8), printer.printedDocument());
        assertEquals(documents.get(9), printer.printedDocument());

        // Cleanup
        printDispatcher.stop();
    }

    @Test
    public void testStopInFilledQueue() throws InterruptedException {
        // Setup
        var printer = new MockPrinter();
        var printDispatcher = new DefaultPrintDispatcher(printer);
        var first = new MockDocument().typeName("first");
        var second = new MockDocument().typeName("second");
        printDispatcher.print(first);
        printDispatcher.print(second);
        printer.waitForStartPrinting();

        // When
        var actualDocs = printDispatcher.stop();

        // Then
        assertEquals(2, actualDocs.size(), actualDocs::toString);
        assertEquals(first, actualDocs.get(0));
        assertEquals(second, actualDocs.get(1));

        // Cleanup
        printDispatcher.stop();
    }

    @Test
    public void testStopEmptyQueue() {
        // Setup
        var printer = new MockPrinter();
        var printDispatcher = new DefaultPrintDispatcher(printer);

        // When
        var actualDocs = printDispatcher.stop();

        // Then
        assertNotNull(actualDocs);
        assertEquals(0, actualDocs.size());

        // Cleanup
        printDispatcher.stop();
    }

    @Test
    public void testCancelActiveDoc() throws InterruptedException {
        // Setup
        var printer = new MockPrinter();
        var printDispatcher = new DefaultPrintDispatcher(printer);
        var expectedDocument = new MockDocument().typeName("test document");
        printDispatcher.print(expectedDocument);
        printer.waitForStartPrinting();

        // When
        var actual = printDispatcher.cancel(expectedDocument.typeName());

        // Then
        assertEquals(expectedDocument, actual);

        // Cleanup
        printDispatcher.stop();
    }

    @Test
    public void testSuccessPrintAfterCancelDoc() throws InterruptedException {
        // Setup
        var printer = new MockPrinter();
        var printDispatcher = new DefaultPrintDispatcher(printer);
        var cancelled = new MockDocument().typeName("cancelled");
        var printed = new MockDocument().typeName("printed");

        printDispatcher.print(cancelled);
        printer.waitForStartPrinting();
        printDispatcher.cancel("cancelled");
        printer.waitForCancel();

        // When
        printDispatcher.print(printed);

        // Then
        assertEquals(printed, printer.printedDocument());

        // Cleanup
        printDispatcher.stop();
    }

    @Test
    public void testGetPrintedOrderDocs() throws InterruptedException, ExecutionException {
        // Setup
        var printer = new MockPrinter();
        var printDispatcher = new DefaultPrintDispatcher(printer);
        var first = new MockDocument().typeName("a");
        var second = new MockDocument().typeName("b");
        printDispatcher.print(first);
        printDispatcher.print(second);
        printer.skip();
        printer.skip();
        printDispatcher.action("b").waitForFinish();

        // When
        var printedList = printDispatcher.printedDocs();

        // Then
        assertEquals(2, printedList.size());
        assertEquals(first, printedList.get(0));
        assertEquals(second, printedList.get(1));

        // Cleanup
        printDispatcher.stop();
    }

    @Test
    public void testReturnOnlyPrintedDocs() throws InterruptedException, ExecutionException {
        // Setup
        var printer = new MockPrinter();
        var printDispatcher = new DefaultPrintDispatcher(printer);
        var printed = new MockDocument().typeName("a");
        var notPrinted = new MockDocument().typeName("b");
        printDispatcher.print(printed);
        printDispatcher.print(notPrinted);
        printer.skip();
        printDispatcher.action("a").waitForFinish();
        printer.waitForStartPrinting();
        printDispatcher.cancel("b");

        // When
        var printedList = printDispatcher.printedDocs();

        // Then
        assertEquals(1, printedList.size(), printedList::toString);
        assertEquals(printed, printedList.get(0));

        // Cleanup
        printDispatcher.stop();
    }

    @Test
    public void testReturnEmptyPrintedList() {
        // Setup
        var printer = new MockPrinter();
        var printDispatcher = new DefaultPrintDispatcher(printer);

        // When
        var printedList = printDispatcher.printedDocs();

        // Then
        assertNotNull(printedList);
        assertEquals(0, printedList.size());

        // Cleanup
        printDispatcher.stop();
    }

    @Test
    public void testReturnCustomSortedPrintedList() throws InterruptedException, ExecutionException {
        // Setup
        var printer = new MockPrinter();
        var printDispatcher = new DefaultPrintDispatcher(printer);
        var first = new MockDocument()
                .printDuration(Duration.ofSeconds(1))
                .typeName("a")
                .paperSize(PaperSize.A4);
        var second = new MockDocument()
                .printDuration(Duration.ofSeconds(0))
                .typeName("b")
                .paperSize(PaperSize.A5);
        var third = new MockDocument()
                .printDuration(Duration.ofSeconds(2))
                .typeName("c")
                .paperSize(PaperSize.A3);

        printDispatcher.print(first);
        printDispatcher.print(second);
        printDispatcher.print(third);
        printer.skip();
        printer.skip();
        printer.skip();
        printDispatcher.action("c").waitForFinish();

        // When
        var paperSizeSorted = printDispatcher.printedDocs(Comparator.comparing(Document::paperSize));

        // Then
        assertEquals(3, paperSizeSorted.size(), paperSizeSorted::toString);
        assertEquals(third, paperSizeSorted.get(0));
        assertEquals(first, paperSizeSorted.get(1));
        assertEquals(second, paperSizeSorted.get(2));

        // When
        var printDurationSorted = printDispatcher.printedDocs(Comparator.comparing(Document::printDuration));

        // Then
        assertEquals(3, printDurationSorted.size(), printDurationSorted::toString);
        assertEquals(second, printDurationSorted.get(0));
        assertEquals(first, printDurationSorted.get(1));
        assertEquals(third, printDurationSorted.get(2));

        // When
        var typeNameSorted = printDispatcher.printedDocs(Comparator.comparing(Document::typeName));

        // Then
        assertEquals(3, typeNameSorted.size(), typeNameSorted::toString);
        assertEquals(first, typeNameSorted.get(0));
        assertEquals(second, typeNameSorted.get(1));
        assertEquals(third, typeNameSorted.get(2));

        // Cleanup
        printDispatcher.stop();
    }

    @Test
    public void testCorrectCalcPrintDurationAvg() throws InterruptedException, ExecutionException {
        // Setup
        var printer = new MockPrinter();
        var printDispatcher = new DefaultPrintDispatcher(printer);
        var first = new MockDocument()
                .typeName("first")
                .printDuration(Duration.ofSeconds(1));
        var second = new MockDocument()
                .typeName("second")
                .printDuration(Duration.ofSeconds(2));

        printDispatcher.print(first);
        printDispatcher.print(second);
        printer.skip();
        printer.skip();
        printDispatcher.action("second").waitForFinish();

        // When
        var avg = printDispatcher.calcAvgPrintDuration();

        // Then
        assertEquals(1500, avg.toMillis());

        // Cleanup
        printDispatcher.stop();
    }

    private static class MockPrinter implements Printer {

        private final TransferQueue<Document> queue = new LinkedTransferQueue<>();

        private volatile boolean cancelled = false;

        private volatile CountDownLatch startPrintingNotifier = new CountDownLatch(1);
        private volatile CountDownLatch cancelPrintingNotifier = new CountDownLatch(1);

        @Override
        public void print(Document document) throws InterruptedException {
            rechargeCancelPrintingBarrier();
            startPrintingNotifier.countDown();
            queue.transfer(document);
            rechargeStartPrintingBarrier();
        }

        private void rechargeStartPrintingBarrier() {
            if (startPrintingNotifier.getCount() == 0) {
                startPrintingNotifier = new CountDownLatch(1);
            }
        }

        private void rechargeCancelPrintingBarrier() {
            if (cancelPrintingNotifier.getCount() == 0) {
                cancelPrintingNotifier = new CountDownLatch(1);
            }
        }

        public void skip() throws InterruptedException {
            queue.poll(100, TimeUnit.MILLISECONDS);
        }

        @Override
        public Document stop() {
            queue.clear();
            cancelPrintingNotifier.countDown();
            return null;
        }

        public void waitForCancel() throws InterruptedException {
            cancelPrintingNotifier.await(100, TimeUnit.MILLISECONDS);
        }

        public Document printedDocument() throws InterruptedException {
            return queue.poll(100, TimeUnit.MILLISECONDS);
        }

        public void waitForStartPrinting() throws InterruptedException {
            startPrintingNotifier.await(100, TimeUnit.MILLISECONDS);
            while (queue.peek() == null) {
                Thread.yield();
            }
        }
    }

}