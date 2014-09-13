package uk.ac.susx.tag.classificationframework.trainers;

import uk.ac.susx.tag.classificationframework.classifiers.NaiveBayesClassifier;
import uk.ac.susx.tag.classificationframework.datastructures.ProcessedInstance;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;

import java.util.Collection;

/**
 * Created by simon on 22/04/14.
 */
public class BootstrapEMTrainer extends AbstractNaiveBayesTrainer {

    @Override
    public NaiveBayesClassifier train(FeatureExtractionPipeline pipeline,
                                      Collection<ProcessedInstance> labelledData,
                                      Collection<ProcessedInstance> unlabelledData,
                                      NaiveBayesClassifier classifier) {

        NaiveBayesClassifier model = new NaiveBayesClassifier(classifier.getLabels());

        copyLabelMultipliers(classifier, model);

        copyFeatureAlphas(classifier, model);

        model.empiricalLabelPriors(classifier.empiricalLabelPriors());

        model.train(labelledData);

        model.emTrain(unlabelledData, model);

        return model;
    }
}
