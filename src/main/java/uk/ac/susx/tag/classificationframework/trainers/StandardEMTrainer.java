package uk.ac.susx.tag.classificationframework.trainers;

import uk.ac.susx.tag.classificationframework.classifiers.NaiveBayesClassifier;
import uk.ac.susx.tag.classificationframework.datastructures.ProcessedInstance;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;

import java.util.Collection;

/**
 * Created by simon on 22/04/14.
 */
public class StandardEMTrainer extends AbstractNaiveBayesTrainer {


    @Override
    public NaiveBayesClassifier train(FeatureExtractionPipeline pipeline,
                                      Collection<ProcessedInstance> labelledData,
                                      Collection<ProcessedInstance> unlabelledData,
                                      NaiveBayesClassifier classifier) {
        NaiveBayesClassifier model = new NaiveBayesClassifier(classifier.getLabels());

        copyLabelMultipliers(classifier, model);

        model.empiricalLabelPriors(classifier.empiricalLabelPriors());

        model.train(labelledData);

        NaiveBayesClassifier featureAlphaModel = new NaiveBayesClassifier(classifier.getLabels());

        copyFeatureAlphas(classifier, featureAlphaModel);

        featureAlphaModel.empiricalLabelPriors(classifier.empiricalLabelPriors());

        model.emTrain(unlabelledData, featureAlphaModel);

        copyFeatureAlphas(classifier, model);

        return model;
    }
}
