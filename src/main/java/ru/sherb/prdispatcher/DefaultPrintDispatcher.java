package ru.sherb.prdispatcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

/**
 * @author mshherbakov
 * @since 23.09.2019
 */
public class DefaultPrintDispatcher implements PrintDispatcher {

    private static final Logger log = LoggerFactory.getLogger(DefaultPrintDispatcher.class);
    
    private final BlockingQueue<Document> printQueue = new LinkedBlockingQueue<>();
    private final ConcurrentMap<String, PrintAction> actions = new ConcurrentHashMap<>();

    private final Thread background;

    public DefaultPrintDispatcher(Printer printer) {
        background = new Thread(() -> this.dispatch(printer));
        background.start();
    }

    private void dispatch(Printer printer) {
        var printTask = Executors.newSingleThreadExecutor();
        while (!Thread.interrupted()) {
            Document document;
            try {
                log.info("waiting for new document...");
                document = printQueue.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            var future = printTask.submit(() -> {
                log.info("printing: {}", document);
                printer.print(document);
                return document;
            });
            PrintAction action = new PrintAction(document, future);
            actions.put(document.typeName(), action);
            log.info("put action: {}", action);

            try {
                action.waitForFinish();
                log.info("finish: {}", action);
            } catch (CancellationException | ExecutionException ignored) {
                // cancel current document and continue printing
                log.info("cancel: {}", action);
                printer.stop();
            } catch (InterruptedException e) {
                log.info("interrupt: {}", action);
                Thread.currentThread().interrupt();
            }
        }
        printTask.shutdownNow();
    }

    @Override
    public List<Document> stop() {
        cancelAllActiveTask();
        background.interrupt();
        return notPrintedDocuments();
    }

    private void cancelAllActiveTask() {
        actions.values().stream()
               .filter(PrintAction::isActive)
               .forEach(PrintAction::abort);
    }

    private List<Document> notPrintedDocuments() {
        var result = actions
                .values().stream()
                .filter(PrintAction::isAborted)
                .map(PrintAction::document)
                .collect(Collectors.toList());

        printQueue.drainTo(result);

        return result;
    }

    @Override
    public void print(Document document) {
        printQueue.add(document);
    }

    @Override
    public Document cancel(String typeName) {
        log.info("attempt to cancel doc: {}", typeName);
        actions.computeIfPresent(typeName, (__, action) -> {
            action.abort();
            log.info("cancel action: {}", action);
            return action;
        });
        var action = actions.getOrDefault(typeName, EMPTY_ACTION);
        return action.document();
    }

    @Override
    public List<Document> printedDocs() {
        return actions.values().stream()
                      .filter(PrintAction::isPrinted)
                      .map(PrintAction::document)
                      .collect(Collectors.toList());
    }

    @Override
    public List<Document> printedDocs(Comparator<Document> comparator) {
        var printed = printedDocs();
        printed.sort(comparator);
        return printed;
    }

    @Override
    public Duration calcAvgPrintDuration() {
        double avg = printedDocs()
                .stream()
                .mapToLong(doc -> doc.printDuration().toSeconds())
                .average()
                .orElse(0);

        long nanos = convertSecondsToNanos(avg);
        return Duration.ofNanos(nanos);
    }

    private static final long NANOS_IN_SECOND = 1_000_000_000;

    private long convertSecondsToNanos(double seconds) {
        return (long) (seconds * NANOS_IN_SECOND);
    }

    // for test only
    PrintAction action(String typeName) {
        return actions.getOrDefault(typeName, EMPTY_ACTION);
    }

    static class PrintAction {
        private final Document document;
        private final Future<Document> future;

        private PrintAction(Document document, Future<Document> future) {
            this.document = document;
            this.future = future;
        }

        public boolean isPrinted() {
            return future.isDone() && !future.isCancelled();
        }

        public boolean isAborted() {
            return future.isCancelled();
        }

        public boolean isActive() {
            return !future.isDone();
        }

        public Document document() {
            return document;
        }

        public void abort() {
            future.cancel(true);
        }

        public void waitForFinish() throws ExecutionException, InterruptedException {
            future.get();
        }

        @Override
        public String toString() {
            return String.format("[status: %s, doc: %s]",
                    isActive() ? "active" : isAborted() ? "aborted" : "done",
                    document);
        }
    }

    private static final PrintAction EMPTY_ACTION = new PrintAction(null, null) {
        @Override
        public boolean isPrinted() {
            return false;
        }

        @Override
        public boolean isAborted() {
            return false;
        }

        @Override
        public boolean isActive() {
            return false;
        }

        @Override
        public Document document() {
            return null;
        }

        @Override
        public void abort() {
        }

        @Override
        public void waitForFinish() {
        }
    };
}
