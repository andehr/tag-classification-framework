package uk.ac.susx.tag.classificationframework.trainers;

import uk.ac.susx.tag.classificationframework.classifiers.InstanceBasedTrainableClassifier;
import uk.ac.susx.tag.classificationframework.classifiers.NaiveBayesClassifierFeatureMarginals;
import uk.ac.susx.tag.classificationframework.classifiers.OVRClassifier;
import uk.ac.susx.tag.classificationframework.datastructures.ProcessedInstance;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;

import java.util.Collection;

/**
 * Created by thomas on 9/8/14.
 */
public class FeatureMarginalsOVRTrainer {
    public OVRClassifier train(FeatureExtractionPipeline pipeline, Collection<ProcessedInstance> labelledData, Collection<ProcessedInstance> unlabelledData, NaiveBayesClassifierFeatureMarginals classifier)
    {
        OVRClassifier<NaiveBayesClassifierFeatureMarginals> model = new OVRClassifier<>(classifier.getLabels(), NaiveBayesClassifierFeatureMarginals.class);

        model.train(labelledData, unlabelledData);

        return model;
    }
}