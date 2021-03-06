package uk.ac.susx.tag.classificationframework.featureextraction.inference.featureselection;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import uk.ac.susx.tag.classificationframework.datastructures.Instance;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;

import java.util.Set;

/**
 * Weighted Frequency and Odds feature selection.
 *
 * Can be made to model the following 3 popular feature selection techniques :
 *
 *  DF: document frequency
 *  MI: mutual information
 *  WLLR: weighted log-likelihood ratio
 *
 * Use the factory methods to achieve this.
 *
 * But note that a purpose built DF or MI would me more efficient.
 *
 * For details how to otherwise utilise the lambda parameter, see:
 *
 *      A Framework of Feature Selection Methods for Text Categorization
 *         - Li et al, 2009
 *
 * User: Andrew D. Robertson
 * Date: 27/01/2014
 * Time: 12:43
 */
public class FeatureSelectorWFO extends FeatureSelectorWithDocumentFrequencyCutoff {

    private static final long serialVersionUID = 0L;

    private double lambda;
    private int N;

    private int documentFrequencyCutoff = 0; // see setDocumentFrequencyCutoff()

    /**
     * @param lambda Between 0 and 1. It is the balance between document frequency measurement, and category ratio
     * @param N The number of features to keep after ranking them
     */
    public FeatureSelectorWFO(double lambda, int N) {
        super();
        this.lambda = lambda;
        this.N = N;
    }

    public FeatureSelectorWFO(double lambda, int N, Set<String> featureTypes){
        super(featureTypes);
        this.lambda = lambda;
        this.N = N;
    }

    /**
     * Set a hard cutoff on the document frequency of those features which can be considered to be selected.
     * E.g. with a cutoff of 3, only those features which occurred in MORE THAN 3 documents will be scored and ranked
     *      in order to determine whether they'll be in the final selected feature set.
     * This defaults to 0, which is basically no cutoff.
     */
    public void setDocumentFrequencyCutoff(int cutoff){
        documentFrequencyCutoff = cutoff;
    }

    /**
     * Create a weighted log-likelihood ratio feature selector
     */
    public static FeatureSelectorWFO WLLR(int N, Set<String> featureTypes) {
        return new FeatureSelectorWFO(0.5, N, featureTypes);
    }

    /**
     * Create a mutual information feature selector
     */
    public static FeatureSelectorWFO MI(int N, Set<String> featureTypes) {
        return new FeatureSelectorWFO(0, N, featureTypes);
    }

    /**
     * Create a document frequency feature selector
     */
    public static FeatureSelectorWFO DF(int N, Set<String> featureTypes) {
        return new FeatureSelectorWFO(1, N, featureTypes);
    }

    @Override
    public void update(FeatureExtractionPipeline.Data data) {
//        Evidence e = FeatureSelector.collectEvidence(documents, selectedFeatureTypes, pipeline);
        Evidence e = new FeatureSelector.EvidenceCollectorAllData().collectEvidence(data, selectedFeatureTypes);

        Object2DoubleMap<String> scores = new Object2DoubleOpenHashMap<>();
        for (String feature : e.vocab()){

            // If this feature has a document frequency greater than the cutoff, then we'll consider its score in the ranking, otherwise, we take it out of the running
            if (e.getFeatureCount(feature) >= documentFrequencyCutoff) {

                double maxScore = 0; // According to the paper, max score tends to work better than average score
                for (String classLabel : e.classLabels()) {
                    double score = Math.pow(frequency(classLabel, feature, e), lambda) *
                                   Math.pow(odds(classLabel, feature, e), 1 - lambda);
                    if (score > maxScore) maxScore = score;
                }
                scores.put(feature, maxScore);
            }
        }
        for(Object2DoubleMap.Entry<String> entry : new FeatureScoreOrdering().greatestOf(scores.object2DoubleEntrySet(), N)){
            topFeatures.add(entry.getKey());
        }
    }

    /**
     * Get the fraction of occurrence of a feature in documents with a particular class label
     */
    private double frequency(String classLabel, String feature, Evidence e) {
        // (number of documents with class label containing feature) / (number of documents with class label)
        return (e.A(classLabel, feature) + 1) / (double)(e.N(classLabel)+1);
    }

    private double odds(String classLabel, String feature, Evidence e){
        double fGivenC = (e.A(classLabel, feature) + 1) / (double)((e.N(classLabel) + 1));
        double fGivenNotC = (e.B(classLabel, feature) + 1) / (double)(e.Nall() - e.N(classLabel) + 1);
        double ratio = fGivenC / fGivenNotC;
        if (ratio > 1){
            return Math.log(ratio);
        } else return 0;
    }

    @Override
    public boolean isThreadSafe() {
        return true;
    }
}