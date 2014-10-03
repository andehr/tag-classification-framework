package uk.ac.susx.tag.classificationframework.trainers;

import uk.ac.susx.tag.classificationframework.classifiers.NaiveBayesClassifierFeatureMarginals;
import uk.ac.susx.tag.classificationframework.classifiers.NaiveBayesClassifier;
import uk.ac.susx.tag.classificationframework.classifiers.NaiveBayesOVRClassifier;
import uk.ac.susx.tag.classificationframework.classifiers.OVRClassifier;
import uk.ac.susx.tag.classificationframework.datastructures.ProcessedInstance;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;

import java.util.Collection;

/**
 * Created by thomas on 9/8/14.
 */
public class FeatureMarginalsOVRTrainer extends AbstractNaiveBayesTrainer {

    @Override
    public NaiveBayesClassifier train(FeatureExtractionPipeline pipeline, Collection<ProcessedInstance> labelledData, Collection<ProcessedInstance> unlabelledData, NaiveBayesClassifier classifier) {
        NaiveBayesOVRClassifier<NaiveBayesClassifierFeatureMarginals> model = null;

        // TODO: Figure out if there's a better way to do it
        if (classifier instanceof NaiveBayesOVRClassifier) {
            NaiveBayesOVRClassifier<NaiveBayesClassifierFeatureMarginals> cls = (NaiveBayesOVRClassifier<NaiveBayesClassifierFeatureMarginals>)classifier;
            model = new NaiveBayesOVRClassifier<>(classifier.getLabels(), NaiveBayesClassifierFeatureMarginals.class, cls.getOvrLearners());

            for (int key : cls.getOvrLearners().keySet()) {
                NaiveBayesClassifierFeatureMarginals from = cls.getOvrLearners().get(key);
                NaiveBayesClassifierFeatureMarginals to = model.getOvrLearners().get(key);

                super.copyLabelMultipliers(from, to);
                super.copyFeatureAlphas(from, to);

                to.empiricalLabelPriors(from.empiricalLabelPriors());
            }

            model.train(labelledData, unlabelledData);
        }
        return model;
    }
}