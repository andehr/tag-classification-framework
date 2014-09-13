package uk.ac.susx.tag.classificationframework.trainers;

import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import uk.ac.susx.tag.classificationframework.classifiers.NaiveBayesClassifier;

import java.util.Map;

/**
 * Created by simon on 22/04/14.
 */
public abstract class AbstractNaiveBayesTrainer implements NaiveBayesTrainer {

    protected void copyFeatureAlphas(NaiveBayesClassifier from, NaiveBayesClassifier to) {
        for(Int2ObjectMap.Entry<Int2DoubleOpenHashMap> entry : from.getLabelledFeatures().int2ObjectEntrySet()) {

            int labelIdx = entry.getIntKey();

            for(Int2DoubleOpenHashMap.Entry featureAlphas : entry.getValue().int2DoubleEntrySet() ) {

                to.setFeatureAlpha(featureAlphas.getIntKey(), labelIdx, featureAlphas.getDoubleValue());
            }
        }
    }

    protected void copyLabelMultipliers(NaiveBayesClassifier from, NaiveBayesClassifier to) {
        for(Map.Entry<Integer,Double> entry : from.getLabelMultipliers().entrySet()) {
            to.setLabelMultiplier(entry.getKey(), entry.getValue());
        }
    }
}
