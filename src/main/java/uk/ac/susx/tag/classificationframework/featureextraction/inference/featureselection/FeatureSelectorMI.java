package uk.ac.susx.tag.classificationframework.featureextraction.inference.featureselection;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import uk.ac.susx.tag.classificationframework.datastructures.Instance;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;

import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: Andrew D. Robertson
 * Date: 24/06/2015
 * Time: 14:53
 */
public class FeatureSelectorMI extends FeatureSelectorWithDocumentFrequencyCutoff {

    private static final long serialVersionUID = 0L;
    private int N;
    private int documentFrequencyCutoff = 0; // see setDocumentFrequencyCutoff()

    /**
     * @param N The number of features to keep after ranking them
     */
    public FeatureSelectorMI(int N) {
        super();
        this.N = N;
    }

    public FeatureSelectorMI(int N, Set<String> featureTypes){
        super(featureTypes);
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
                    double score = mutualInformation(feature, classLabel, e);
                    if (score > maxScore) maxScore = score;
                }
                scores.put(feature, maxScore);
            }
        }
        for(Object2DoubleMap.Entry<String> entry : new FeatureScoreOrdering().greatestOf(scores.object2DoubleEntrySet(), N)){
            topFeatures.add(entry.getKey());
        }
    }

    public double mutualInformation(String feature, String classLabel, Evidence e){
        double cGivenF = (e.A(classLabel, feature) + 1) / (double)(e.A(classLabel,feature + e.B(classLabel, feature)) + e.classLabels().size());
        double cPrior = (e.N(classLabel) + 1)/ (double)(e.Nall() + e.classLabels().size());
        return Math.log(cGivenF/cPrior);
    }
}
