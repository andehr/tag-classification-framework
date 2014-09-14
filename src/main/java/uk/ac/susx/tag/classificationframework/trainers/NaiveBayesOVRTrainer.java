package uk.ac.susx.tag.classificationframework.trainers;

import uk.ac.susx.tag.classificationframework.classifiers.NaiveBayesClassifier;
import uk.ac.susx.tag.classificationframework.classifiers.OVRClassifier;
import uk.ac.susx.tag.classificationframework.datastructures.ProcessedInstance;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;

import java.util.Collection;

/**
 * Created by thomas on 9/14/14.
 */
public interface NaiveBayesOVRTrainer {
    OVRClassifier train(FeatureExtractionPipeline pipeline,
                               Collection<ProcessedInstance> labelledData,
                               Collection<ProcessedInstance> unlabelledData,
                               NaiveBayesClassifier classifier);
}
