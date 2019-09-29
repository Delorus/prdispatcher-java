package ru.sherb.prdispatcher;

import java.time.Duration;

/**
 * All classes that implements this interface may be print on {@link PrintDispatcher}
 *
 * @author mshherbakov
 * @since 23.09.2019
 */
public interface Document {

    /**
     * @return Formatted document type name
     */
    String typeName();

    /**
     * @return Size of paper that document must be printing
     */
    PaperSize paperSize();

    /**
     * @return Estimated print time
     */
    Duration printDuration();
}
