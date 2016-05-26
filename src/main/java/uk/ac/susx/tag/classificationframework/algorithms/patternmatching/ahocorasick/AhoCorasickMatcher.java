package uk.ac.susx.tag.classificationframework.algorithms.patternmatching.ahocorasick;

import com.google.common.collect.ImmutableList;
import org.ahocorasick.interval.IntervalTree;
import org.ahocorasick.interval.Intervalable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by Andrew D. Robertson on 26/05/2016.
 */
public class AhoCorasickMatcher<E> {

    private State<E> rootState;

    public AhoCorasickMatcher() {
        rootState = new State<>();
    }

    public void addPattern(ImmutableList<E> pattern) {
        State<E> currentState = rootState;

        for (E e : pattern) {
            currentState = currentState.addState(e);
        }

        currentState.addEmit(pattern);
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

    public Collection<Emit<E>> parse(List<E> query){
        EmitHandlerDefault<E> emitHandler = new EmitHandlerDefault<>();
        parse(query, emitHandler);
        List<Emit<E>> collectedEmits = emitHandler.getEmits();

        IntervalTree intervalTree = new IntervalTree((List<Intervalable>)(List<?>)collectedEmits);
        intervalTree.removeOverlaps((List<Intervalable>) (List<?>) collectedEmits);

        return collectedEmits;
    }

    public void parse(List<E> query, EmitHandler<E> emitHandler){
        State<E> currentState = rootState;
        for (int position = 0; position < query.size(); position++){
            E element = query.get(position);
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


    public static void main(String[] args){
        AhoCorasickMatcher<String> m = new AhoCorasickMatcher<>();
        m.addPattern(ImmutableList.of("b", "c", "d"));
        m.addPattern(ImmutableList.of("a", "b", "c"));
//        m.addPattern(ImmutableList.of());
        m.constructFailureStates();

        for (Chunk c  :m.tokenise(ImmutableList.of("x", "y", "a", "b", "c", "d", "x", "y") )){
            System.out.println(c);
        }

//        ImmutableList<String> f = ImmutableList.of("a", "b", "c", "d");
//        ImmutableList<String> g = ImmutableList.copyOf(f);
//        ImmutableList<String> h = ImmutableList.of("a", "b", "c", "d");
//        System.out.println(f.equals(g));
    }
}
