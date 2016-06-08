package uk.ac.susx.tag.classificationframework.algorithms.patternmatching.ahocorasick;

import com.google.common.collect.ImmutableList;
import org.ahocorasick.interval.Interval;
import org.ahocorasick.interval.Intervalable;

/**
 * Created by adr27 on 26/05/2016.
 */
public class Emit<E> extends Interval implements Intervalable {

    private final ImmutableList<E> pattern;

    public Emit(int start, int end, final ImmutableList<E> pattern) {
        super(start, end);

        this.pattern = pattern;
    }

    public ImmutableList<E> getPattern() {
        return pattern;
    }

    public String toString() {
        return super.toString() + "=" + pattern.toString();
    }
}
