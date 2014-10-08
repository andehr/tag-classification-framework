package uk.ac.susx.tag.classificationframework.trainers;

import uk.ac.susx.tag.classificationframework.classifiers.NaiveBayesClassifier;
import uk.ac.susx.tag.classificationframework.classifiers.NaiveBayesClassifierSFE;
import uk.ac.susx.tag.classificationframework.datastructures.ProcessedInstance;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;

import java.util.Collection;

/**
 * Created by thk22 on 08/10/2014.
 */
public class StandardSFETrainer extends AbstractNaiveBayesTrainer {
	@Override
	public NaiveBayesClassifier train(FeatureExtractionPipeline pipeline,
									  Collection<ProcessedInstance> labelledData,
									  Collection<ProcessedInstance> unlabelledData,
									  NaiveBayesClassifier classifier) {

		NaiveBayesClassifierSFE model = new NaiveBayesClassifierSFE(classifier.getLabels());

		super.copyLabelMultipliers(classifier, model);
		super.copyFeatureAlphas(classifier, model);

		model.empiricalLabelPriors(classifier.empiricalLabelPriors());

		model.train(labelledData, unlabelledData);

		return model;
	}
}
