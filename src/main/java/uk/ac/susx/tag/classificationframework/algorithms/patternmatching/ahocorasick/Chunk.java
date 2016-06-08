package uk.ac.susx.tag.classificationframework.algorithms.patternmatching.ahocorasick;

import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * Created by adr27 on 26/05/2016.
 */
public class Chunk<E> {

    private List<E> elements;
    private boolean matched;

    public Chunk(List<E> elements, boolean matched) {
        this.elements = elements;
        this.matched = matched;
    }

    @Override
    public String toString() {
        return "Chunk{" +
                "elements=" + elements +
                ", matched=" + matched +
                '}';
    }
}
