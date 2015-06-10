package uk.ac.susx.tag.classificationframework.classifiers;

import it.unimi.dsi.fastutil.ints.IntSet;

/**
 * Created with IntelliJ IDEA.
 * User: Andrew D. Robertson
 * Date: 08/06/2015
 * Time: 15:56
 */
public interface LowFrequencyFeatureTrimmable {

    /**
     * Get all features with frequency less than *frequencyCutoff* including pseudo-counts.
     */
    public IntSet getInfrequentFeatures(double frequencyCutoff);

    /**
     * Get all features with frequency less than *frequencyCutoff* including pseudo-counts.
     * Also remove all evidence of these features from the classifier.
     */
    public IntSet trimInfrequentFeature(double frequencyCutoff);
}
