package uk.ac.susx.tag.classificationframework.classifiers;

import it.unimi.dsi.fastutil.ints.IntSet;

/**
 * Created by thk22 on 03/10/2014.
 */
public class NaiveBayesOVRClassifier<T extends AbstractNaiveBayesClassifier> extends OVRClassifier {

    public NaiveBayesOVRClassifier(IntSet labels, Class<? extends AbstractNaiveBayesClassifier> learnerClass) {
        super(labels, learnerClass);
    }

}
