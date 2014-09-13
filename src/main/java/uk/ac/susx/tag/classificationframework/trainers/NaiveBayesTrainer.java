package uk.ac.susx.tag.classificationframework.trainers;

import uk.ac.susx.tag.classificationframework.classifiers.NaiveBayesClassifier;
import uk.ac.susx.tag.classificationframework.datastructures.ProcessedInstance;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;

import java.util.Collection;

/**
 * Created by simon on 22/04/14.
 */
public interface NaiveBayesTrainer {

    NaiveBayesClassifier train(FeatureExtractionPipeline pipeline,
                               Collection<ProcessedInstance> labelledData,
                               Collection<ProcessedInstance> unlabelledData,
                               NaiveBayesClassifier classifier);
}
