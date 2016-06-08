package uk.ac.susx.tag.classificationframework.algorithms.patternmatching.ahocorasick;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.ahocorasick.interval.IntervalTree;
import org.ahocorasick.interval.Intervalable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Immplementation of the Aho-Corasick algorithm for finding exact sub sets.
 *
 * Use AhoCorasickMatcher.builder() to get a builder for configuring and adding
 * patterns to the matcher. Then call build() and either:
 *
 *  1. Use tokenise() to split a list by pattern
 *  2. Use parse() to obtain a list of each match
 *
 * Usage example with a PreProcessor that lower-cases everything first:
 *
 * Builder<String> b = builder(String::toLowerCase);
 * b.addPattern(ImmutableList.of("a", "b", "C"));
 * b.addPattern(ImmutableList.of("b", "c", "d"));
 * b.build().parse(Lists.newArrayList("x", "A", "b", "c", "d", "x"))
 *                   .forEach(System.out::println);
 *
 * Based on the java code at https://github.com/robert-bor/aho-corasick
 * But generalised from finding strings within strings to immutable lists of anything.
 *
 * License: http://www.apache.org/licenses/LICENSE-2.0
 *
 * // TODO: Add wildcard capability, by splitting the pattern on the wildcard and making it multiple patterns,
 * // TODO: then once matched, do a second pass to join wildcarded patterns or veto patterns that didn't make it
 *
 * Created by Andrew D. Robertson on 26/05/2016.
 */
public class AhoCorasickMatcher<E> {

    private State<E> rootState;
    private boolean removeOverlaps;
    private PreProcessor<E> preProcessor;

    private AhoCorasickMatcher(PreProcessor<E> preProcessor) {
        this.preProcessor = preProcessor!=null? preProcessor : e->e;
        rootState = new State<>();
        removeOverlaps = true;
    }

    private void addPattern(ImmutableList<E> pattern) {
        State<E> currentState = rootState;

        for (E e : pattern) {
            e = preProcessor.preProcess(e);
            currentState = currentState.addState(e);
        }

        currentState.addEmit(pattern);
    }

    public void removeOverlaps(boolean remove){
        removeOverlaps = remove;
    }

    public Collection<Chunk<E>> tokenise(List<E> query) {

        Collection<Chunk<E>> chunks = new ArrayList<>();

        Collection<Emit<E>> collectedEmits = parse(query);

        int lastPos = -1;

        for (Emit<E> emit : collectedEmits) {
            if (emit.getStart() - lastPos > 1){
                chunks.add(new Chunk<>(query.subList(lastPos+1, emit.getStart()), false));
            }
            chunks.add(new Chunk<>(query.subList(emit.getStart(), emit.getEnd()+1), true));
            lastPos = emit.getEnd();
        }

        if (query.size() - lastPos > 1) {
            chunks.add(new Chunk<>(query.subList(lastPos+1, query.size()), false));
        }
        return chunks;
    }

    public List<Emit<E>> parse(List<E> query){
        EmitHandlerDefault<E> emitHandler = new EmitHandlerDefault<>();
        parse(query, emitHandler);
        List<Emit<E>> collectedEmits = emitHandler.getEmits();

        if (removeOverlaps) {
            IntervalTree intervalTree = new IntervalTree((List<Intervalable>) (List<?>) collectedEmits);
            intervalTree.removeOverlaps((List<Intervalable>) (List<?>) collectedEmits);
        }

        return collectedEmits;
    }

    private void parse(List<E> query, EmitHandler<E> emitHandler){
        State<E> currentState = rootState;
        for (int position = 0; position < query.size(); position++){
            E element = query.get(position);
            element = preProcessor.preProcess(element);
            currentState = getState(currentState, element);
            storeEmits(position, currentState, emitHandler);
        }
    }

    private State<E> getState(State<E> currentState, E element) {
        State<E> newCurrentState = currentState.nextState(element);
        while(newCurrentState == null){
            currentState = currentState.failure();
            newCurrentState = currentState.nextState(element);
        }
        return newCurrentState;
    }

    private boolean storeEmits(int position, State<E> currentState, EmitHandler<E> emitHandler){
        boolean emitted = false;
        Collection<ImmutableList<E>> emits = currentState.emit();
        if (emits != null && !emits.isEmpty()){
            for (ImmutableList<E> emit : emits){
                emitHandler.emit(new Emit<>(position - emit.size() + 1, position, emit));
                emitted = true;
            }
        }
        return emitted;
    }

    private void constructFailureStates(){
        Queue<State<E>> queue = new LinkedBlockingDeque<>();

        for (State<E> depthOneState : rootState.getStates()){
            depthOneState.setFailure(rootState);
            queue.add(depthOneState);
        }

        while(!queue.isEmpty()){
            State<E> currentState = queue.remove();

            for (E transition : currentState.getTransitions()){
                State<E> targetState = currentState.nextState(transition);
                queue.add(targetState);

                State<E> traceFailureState = currentState.failure();
                while(traceFailureState.nextState(transition) == null){
                    traceFailureState = traceFailureState.failure();
                }
                State<E> newFailureState = traceFailureState.nextState(transition);
                targetState.setFailure(newFailureState);
                targetState.addEmit(newFailureState.emit());
            }
        }
    }

    public static <E> Builder<E> builder(){
        return builder(null);
    }

    public static <E> Builder<E> builder(PreProcessor<E> preProcessor){
        return new Builder<>(preProcessor);
    }

    public static class Builder<E> {

        private AhoCorasickMatcher<E> matcher;

        private Builder(PreProcessor<E> preProcessor) {
            matcher = new AhoCorasickMatcher<>(preProcessor);
        }

        public Builder<E> addPattern(ImmutableList<E> pattern){
            matcher.addPattern(pattern);
            return this;
        }

        public Builder allowOverlaps(){
            matcher.removeOverlaps(false);
            return this;
        }

        public AhoCorasickMatcher<E> build(){
            matcher.constructFailureStates();
            return matcher;
        }
    }

    public interface PreProcessor<E> {
        E preProcess(E element);
    }

    public static void main(String[] args){
        Builder<String> b = builder(String::toLowerCase);
        b.addPattern(ImmutableList.of("a", "b", "C"));
        b.addPattern(ImmutableList.of("b", "c", "d"));
        b.allowOverlaps();

        b.build().parse(Lists.newArrayList("x", "A", "b", "c", "d", "x"))
                .forEach(System.out::println);
    }

}
