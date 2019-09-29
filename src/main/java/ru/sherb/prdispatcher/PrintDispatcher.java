package ru.sherb.prdispatcher;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;

/**
 * @author mshherbakov
 * @since 23.09.2019
 */
public interface PrintDispatcher {

    List<Document> stop();

    void print(Document document);

    Document cancel(String typeName);

    List<Document> printedDocs();

    List<Document> printedDocs(Comparator<Document> comparator);

    Duration calcAvgPrintDuration();
}
