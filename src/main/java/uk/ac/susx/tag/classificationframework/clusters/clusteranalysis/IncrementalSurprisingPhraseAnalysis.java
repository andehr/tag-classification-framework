package uk.ac.susx.tag.classificationframework.clusters.clusteranalysis;

import com.google.common.collect.Ordering;
import uk.ac.susx.tag.classificationframework.datastructures.Instance;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline.PipelineChanges;

/**
 *
 *
 * Created by Andrew D. Robertson on 20/10/16.
 */
public class IncrementalSurprisingPhraseAnalysis {

    private FeatureExtractionPipeline pipeline;
    private PipelineChanges prePhraseExtractionChanges;
    private int minimumBackgroundFeatureCount;
    private int minimumTargetFeatureCount;
    private IncrementalFeatureCounter counter;

    public enum OrderingMethod {
        LIKELIHOOD_IN_CLUSTER_OVER_PRIOR,  // Essentially PMI: P(feature|cluster) / P(feature)
    }

    public IncrementalSurprisingPhraseAnalysis(FeatureExtractionPipeline pipeline,
                                               PipelineChanges prePhraseExtractionChanges,
                                               int minimumBackgroundFeatureCount,
                                               int minimumTargetFeatureCount) {

        this.pipeline = pipeline;
        this.prePhraseExtractionChanges = prePhraseExtractionChanges;
        this.minimumBackgroundFeatureCount = minimumBackgroundFeatureCount;
        this.minimumTargetFeatureCount = minimumTargetFeatureCount;

        counter = new IncrementalFeatureCounter(0.1);
    }

    public Map<String, List<String>> getTopPhrases(){

        return null; //TODO
    }


    public void incrementBackgroundCounts(List<Instance> backgroundDocuments){
        counter.incrementCounts(backgroundDocuments, pipeline);
    }

    private List<List<Integer>> getTopFeatures(Collection<Instance> documents, OrderingMethod method, FeatureType featureType) {

        return null;
    }

    private Map<Integer, List<List<Integer>>> getTopPhrases(List<Integer> topFeatures,
                                                            Collection<Instance> documents,
                                                            int numPhrasesPerFeature,
                                                            double leafPruningThreshold,
                                                            int minPhraseSize,
                                                            int maxPhraseSize){
        return null;
    }

    public static class LikelihoodPriorRatioOrdering extends Ordering<Integer> {

        int clusterIndex = 0;
        FeatureType t;
        FeatureClusterJointCounter theCounts;

        public LikelihoodPriorRatioOrdering(int clusterIndex, FeatureType t, FeatureClusterJointCounter counts) {
            this.clusterIndex = clusterIndex;
            this.t = t;
            this.theCounts = counts;
        }

        @Override
        public int compare(Integer feature1, Integer feature2) {

            double leftRatio = Math.log(theCounts.likelihoodFeatureGivenCluster(feature1, clusterIndex, t)) - Math.log(theCounts.featurePrior(feature1, t));
            double rightRatio = Math.log(theCounts.likelihoodFeatureGivenCluster(feature2, clusterIndex, t)) - Math.log(theCounts.featurePrior(feature2, t));
            return Double.compare(leftRatio, rightRatio);
        }
    }
}
