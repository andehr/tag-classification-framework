package uk.ac.susx.tag.classificationframework.trainers;

import uk.ac.susx.tag.classificationframework.classifiers.NaiveBayesClassifier;
import uk.ac.susx.tag.classificationframework.classifiers.NaiveBayesClassifierFeatureMarginals;
import uk.ac.susx.tag.classificationframework.classifiers.NaiveBayesOVRClassifier;
import uk.ac.susx.tag.classificationframework.datastructures.ProcessedInstance;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;

import java.util.Collection;

/**
 * Created by thomas on 9/8/14.
 */
public class FeatureMarginalsOVRTrainer extends AbstractNaiveBayesTrainer {

    @Override
    public NaiveBayesClassifier train(FeatureExtractionPipeline pipeline, Collection<ProcessedInstance> labelledData, Collection<ProcessedInstance> unlabelledData, NaiveBayesClassifier classifier) {
        NaiveBayesOVRClassifier<NaiveBayesClassifierFeatureMarginals> model = new NaiveBayesOVRClassifier<>(classifier.getLabels(), NaiveBayesClassifierFeatureMarginals.class);

		super.copyLabelMultipliers(classifier, model);
		super.copyFeatureAlphas(classifier, model);

		model.empiricalLabelPriors(classifier.empiricalLabelPriors());

		model.train(labelledData, unlabelledData);

		return model;
    }
}