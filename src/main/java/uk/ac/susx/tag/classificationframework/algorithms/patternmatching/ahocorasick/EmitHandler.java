package uk.ac.susx.tag.classificationframework.algorithms.patternmatching.ahocorasick;

/**
 * Created by Andrew D. Robertson on 26/05/2016.
 */
public interface EmitHandler<E> {
    void emit(Emit<E> emit);
}
