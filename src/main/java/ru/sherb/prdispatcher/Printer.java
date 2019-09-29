package ru.sherb.prdispatcher;

/**
 * @author maksim
 * @since 23.09.2019
 */
public interface Printer {

    void print(Document document) throws InterruptedException;

    Document stop();
}
