package uk.ac.susx.tag.classificationframework.algorithms.patternmatching.ahocorasick;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by adr27 on 26/05/2016.
 */
public class EmitHandlerDefault<E> implements EmitHandler<E>{

    private List<Emit<E>> emits = new ArrayList<>();

    @Override
    public void emit(Emit<E> emit) {
        emits.add(emit);
    }

    public List<Emit<E>> getEmits() {
        return emits;
    }
}
