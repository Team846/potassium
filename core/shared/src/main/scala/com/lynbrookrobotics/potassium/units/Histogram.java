package com.lynbrookrobotics.potassium.units;

import java.util.function.DoubleConsumer;

/**
 * Created by the-magical-llamicorn on 4/19/17.
 */
public class Histogram implements DoubleConsumer {

    protected final long[] histogram;
    public final double min, max;
    public final double interval;
    public final int bins;

    public Histogram(final double min, final double max, final int bins) {
        this.min = min;
        this.max = max;
        this.bins = bins;
        interval = (max - min) / bins;
        histogram = new long[bins + 2]; // bins and "less than min" and "greater than max"
    }

    public void accept(final double value) {
        if (value < min) histogram[0]++;
        else if (value > max) histogram[bins + 1]++;
        else histogram[1 + (int) ((value - min) / interval)]++;
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("<");
        sb.append(min);
        sb.append(" : ");
        sb.append(histogram[0]);
        sb.append('\n');

        for (int i = 1; i < bins + 1; i++) {
            sb.append((float) (min + (interval * (i - 1))));
            sb.append(" to ");
            sb.append((float) (min + (interval * i)));
            sb.append(" : ");
            sb.append(histogram[i]);
            sb.append('\n');
        }

        sb.append(">");
        sb.append(max);
        sb.append(" : ");
        sb.append(histogram[bins+1]);
        sb.append('\n');

        return sb.toString();
    }
}
