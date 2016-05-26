package uk.ac.susx.tag.classificationframework.algorithms.patternmatching.ahocorasick;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by Andrew D Robertson on 26/05/2016.
 */
public class State<E> {

    private final int depth;

    private final State<E> rootState;

    private Map<E, State<E>> success = new HashMap<>();

    private State failure = null;

    private Set<ImmutableList<E>> emits = null;

    public State() {
        this(0);
    }

    public State(int depth) {
        this.depth = depth;

        rootState = depth == 0? this : null;
    }

    private State<E> nextState(E element, boolean ignoreRootState) {
        State<E> nextState = success.get(element);
        if (!ignoreRootState && nextState == null && rootState != null){
            nextState = rootState;
        }
        return nextState;
    }

    public State<E> nextState(E element) {
        return nextState(element, false);
    }

    public State<E> nextStateIgnoreRootState(E element) {
        return nextState(element, true);
    }

    public State<E> addState(E element) {
        State<E> nextState = nextStateIgnoreRootState(element);
        if (nextState == null) {
            nextState = new State<>(this.depth + 1);
            success.put(element, nextState);
        }
        return nextState;
    }

    public int getDepth() {
        return depth;
    }

    public void addEmit(ImmutableList<E> pattern){

        if (emits == null) {
            emits = new HashSet<>();
        }
        emits.add(pattern);
    }

    public void addEmit(Collection<ImmutableList<E>> patterns){
        for (ImmutableList<E> pattern : patterns){
            addEmit(pattern);
        }
    }

    public Collection<ImmutableList<E>> emit() {
        return emits==null? Lists.newArrayList() : emits;
    }

    public State<E> failure(){
        return failure;
    }

    public void setFailure(State<E> failState){
        failure = failState;
    }

    public Collection<State<E>> getStates() {
        return success.values();
    }

    public Collection<E> getTransitions() {
        return success.keySet();
    }
}
