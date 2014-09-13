package uk.ac.susx.tag.classificationframework.classifiers;

import uk.ac.susx.tag.classificationframework.datastructures.ProcessedInstance;

/**
 * Created by thomas on 9/8/14.
 */
public interface InstanceBasedTrainableClassifier extends Classifier {

    // TODO: Add the weighted train methods as well?
    public void train(Iterable<ProcessedInstance> labelledDocuments);
    public void train(Iterable<ProcessedInstance> labelledDocuments, Iterable<ProcessedInstance> unlabelledDocuments);
}
