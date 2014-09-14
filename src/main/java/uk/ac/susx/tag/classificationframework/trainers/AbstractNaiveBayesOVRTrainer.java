package uk.ac.susx.tag.classificationframework.trainers;

import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import uk.ac.susx.tag.classificationframework.classifiers.NaiveBayesClassifier;
import uk.ac.susx.tag.classificationframework.classifiers.OVRClassifier;

import java.util.Map;

/**
 * Created by thomas on 9/14/14.
 */
public abstract class AbstractNaiveBayesOVRTrainer implements NaiveBayesOVRTrainer {

    protected void copyFeatureAlphas(NaiveBayesClassifier from, OVRClassifier<? extends NaiveBayesClassifier> to) {

        for (NaiveBayesClassifier cls : to.getOvrLearners()) {
            for (Int2ObjectMap.Entry<Int2DoubleOpenHashMap> entry : from.getLabelledFeatures().int2ObjectEntrySet()) {
                int labelIdx = entry.getIntKey();
                for (Int2DoubleOpenHashMap.Entry featureAlphas : entry.getValue().int2DoubleEntrySet()) {
                    cls.setFeatureAlpha(featureAlphas.getIntKey(), labelIdx, featureAlphas.getDoubleValue());
                }
            }
        }
    }

    protected void copyLabelMultipliers(NaiveBayesClassifier from, OVRClassifier<? extends NaiveBayesClassifier> to) {
        for (NaiveBayesClassifier cls : to.getOvrLearners()) {
            for (Map.Entry<Integer, Double> entry : from.getLabelMultipliers().entrySet()) {
                cls.setLabelMultiplier(entry.getKey(), entry.getValue());
            }
        }
    }

    protected void setEmpiricalLabelPriors(NaiveBayesClassifier from, OVRClassifier<? extends NaiveBayesClassifier> to)
    {
        for (NaiveBayesClassifier cls : to.getOvrLearners()) {
            cls.empiricalLabelPriors(from.empiricalLabelPriors());
        }
    }
}
