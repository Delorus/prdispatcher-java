package ru.sherb.prdispatcher;

import java.time.Duration;
import java.util.Objects;

/**
 * @author maksim
 * @since 24.09.2019
 */
public class MockDocument implements Document {

    private String typeName = "";
    private PaperSize paperSize = PaperSize.A4;
    private Duration printDuration = Duration.ZERO;

    @Override
    public String typeName() {
        return typeName;
    }

    @Override
    public PaperSize paperSize() {
        return paperSize;
    }

    @Override
    public Duration printDuration() {
        return printDuration;
    }

    public MockDocument typeName(String typeName) {
        this.typeName = typeName;
        return this;
    }

    public MockDocument paperSize(PaperSize paperSize) {
        this.paperSize = paperSize;
        return this;
    }

    public MockDocument printDuration(Duration printDuration) {
        this.printDuration = printDuration;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MockDocument that = (MockDocument) o;
        return paperSize == that.paperSize &&
                Objects.equals(typeName, that.typeName) &&
                Objects.equals(printDuration, that.printDuration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeName, paperSize, printDuration);
    }

    @Override
    public String toString() {
        return "MockDocument[" + "typeName: '" + typeName + '\'' + ']';
    }
}
