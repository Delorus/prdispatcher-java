package ru.sherb.prdispatcher;

import java.time.Instant;

/**
 * @author maksim
 * @since 23.09.2019
 */
public class DryPrinter implements Printer {

    private Document printingDocument;
    private boolean stop = false;

    @Override
    public synchronized void print(Document document) throws InterruptedException {
        stop = false;
        printingDocument = document;

        var printDuration = printingDocument.printDuration();
        var deadline = Instant.now().plus(printDuration);

        while (Instant.now().isBefore(deadline)) {
            wait(printDuration.toMillis());
            if (stop) { break; }
        }

        printingDocument = null;
    }

    @Override
    public synchronized Document stop() {
        stop = true;
        notifyAll();
        return printingDocument;
    }
}
