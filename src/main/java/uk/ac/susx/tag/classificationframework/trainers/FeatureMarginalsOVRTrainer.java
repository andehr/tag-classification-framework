package uk.ac.susx.tag.classificationframework.trainers;

import uk.ac.susx.tag.classificationframework.classifiers.NaiveBayesClassifierFeatureMarginals;
import uk.ac.susx.tag.classificationframework.classifiers.NaiveBayesClassifier;
import uk.ac.susx.tag.classificationframework.classifiers.OVRClassifier;
import uk.ac.susx.tag.classificationframework.datastructures.ProcessedInstance;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;

import java.util.Collection;

/**
 * Created by thomas on 9/8/14.
 */
public class FeatureMarginalsOVRTrainer extends AbstractNaiveBayesOVRTrainer {

    public OVRClassifier train(FeatureExtractionPipeline pipeline, Collection<ProcessedInstance> labelledData, Collection<ProcessedInstance> unlabelledData, NaiveBayesClassifier classifier)
    {
        OVRClassifier<NaiveBayesClassifierFeatureMarginals> model = new OVRClassifier<>(classifier.getLabels(), NaiveBayesClassifierFeatureMarginals.class);

        super.copyLabelMultipliers(classifier, model);
        super.copyFeatureAlphas(classifier, model);
        super.setEmpiricalLabelPriors(classifier, model);

        model.train(labelledData, unlabelledData);

        return model;
    }
}