package uk.ac.susx.tag.classificationframework.clusters.clusteranalysis;

import com.google.common.collect.Ordering;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import uk.ac.susx.tag.classificationframework.clusters.ClusteredProcessedInstance;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * User: Andrew D. Robertson
 * Date: 16/10/2015
 * Time: 15:07
 */
public class ClusterFeatureAnalysis {

    private FeatureClusterJointCounter counts;

    public ClusterFeatureAnalysis(Collection<ClusteredProcessedInstance> documents){
        this(documents, new FeatureClusterJointCounter.FeatureBasedCounts(), new FeatureClusterJointCounter.HighestProbabilityOnly());
    }

    public ClusterFeatureAnalysis(Collection<ClusteredProcessedInstance> documents, FeatureClusterJointCounter c, FeatureClusterJointCounter.ClusterMembershipTest t) {
        counts = c;
        c.count(documents, t);
    }

    public List<String> getTopFeatures(int clusterIndex, int K, FeatureExtractionPipeline pipeline){
        return getTopFeatures(clusterIndex, K).stream()
                .map(pipeline::labelString)
                .collect(Collectors.toList());
    }

    public List<Integer> getTopFeatures(int clusterIndex, int K) {
        return new LikelihoodPriorRatioOrdering(clusterIndex).greatestOf(counts.getFeaturesInCluster(clusterIndex), K);
    }

    public class LikelihoodPriorRatioOrdering extends Ordering<Integer> {

        int clusterIndex = 0;
        public LikelihoodPriorRatioOrdering(int clusterIndex) {
            this.clusterIndex = clusterIndex;
        }

        @Override
        public int compare(Integer feature1, Integer feature2) {

            double leftRatio = Math.log(counts.featurePrior(feature1)) - Math.log(counts.likelihoodFeatureGivenCluster(feature1, clusterIndex));
            double rightRatio = Math.log(counts.featurePrior(feature2)) - Math.log(counts.likelihoodFeatureGivenCluster(feature2, clusterIndex));

            return Double.compare(leftRatio, rightRatio);
        }
    }

}
