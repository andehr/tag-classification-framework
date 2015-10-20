package uk.ac.susx.tag.classificationframework.clusters.clusteranalysis;

import com.google.common.collect.Ordering;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import uk.ac.susx.tag.classificationframework.clusters.ClusteredProcessedInstance;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Analyse the features in clusters of documents.
 *
 * Some classes to know about:
 *
 * FeatureClusterJointCounter : these know a method of counting up feature/cluster occurrences, in
 *                              order to provide feature priors and joint cluster probabilities.
 *                              E.g. 1+ occurrences in a single document might count as a single
 *                              occurrence.
 *
 * FeatureClusterJointCounter
 *     .ClusterMembershipTest : when counting up features that occur in clusters, we need to determine
 *                              whether the containing document is considered "in" a cluster. Implementations
 *                              of this interface specify how this is done. E.g. only the cluster with the
 *                              highest membership probability.
 *
 * OrderingMethod             : When trying to get a list of most interesting features per cluster, there
 *                              are many methods of ranking. This enum allows the user to select between them.
 *
 * User: Andrew D. Robertson
 * Date: 16/10/2015
 * Time: 15:07
 */
public class ClusterFeatureAnalysis {

    private FeatureClusterJointCounter counts;

    public enum OrderingMethod {
        LIKELIHOOD_IN_CLUSTER_OVER_PRIOR,  // Essentially PMI: P(feature|cluster) / P(feature)
        LIKELIHOOD_IN_CLUSTER_OVER_LIKELIHOOD_OUT // P(feature|cluster) / P(feature|!cluster)
    }

    public ClusterFeatureAnalysis(Collection<ClusteredProcessedInstance> documents){
        this(documents, new FeatureClusterJointCounter.FeatureBasedCounts(), new FeatureClusterJointCounter.HighestProbabilityOnly());
    }

    public ClusterFeatureAnalysis(Collection<ClusteredProcessedInstance> documents, FeatureClusterJointCounter c, FeatureClusterJointCounter.ClusterMembershipTest t) {
        counts = c;
        c.count(documents, t);
    }


    public List<String> getTopFeatures(int clusterIndex, int K, FeatureExtractionPipeline pipeline){
        return getTopFeatures(clusterIndex, K, OrderingMethod.LIKELIHOOD_IN_CLUSTER_OVER_PRIOR, pipeline);
    }


    public List<String> getTopFeatures(int clusterIndex, int K, OrderingMethod m, FeatureExtractionPipeline pipeline){
        return getTopFeatures(clusterIndex, K, m).stream()
                .map(pipeline::labelString)
                .collect(Collectors.toList());
    }

    public List<Integer> getTopFeatures(int clusterIndex, int K){
        return getTopFeatures(clusterIndex, K, OrderingMethod.LIKELIHOOD_IN_CLUSTER_OVER_PRIOR);
    }

    /**
     * Get a list of the K most interesting features for a given cluster and ordering method.
     */
    public List<Integer> getTopFeatures(int clusterIndex, int K, OrderingMethod m) {
        Ordering<Integer> ordering;
        switch(m) {
            case LIKELIHOOD_IN_CLUSTER_OVER_LIKELIHOOD_OUT:
                ordering = new LikelihoodsInAndOutOfClusterRatioOrdering(clusterIndex); break;
            case LIKELIHOOD_IN_CLUSTER_OVER_PRIOR:
                ordering = new LikelihoodPriorRatioOrdering(clusterIndex); break;
            default:
                throw new RuntimeException("OrderingMethod not recognised.");
        }

        return ordering.greatestOf(counts.getFeaturesInCluster(clusterIndex), K);
    }

    /**
     * Order by:
     *
     *    P(feature|cluster) / P(feature)
     *
     */
    public class LikelihoodPriorRatioOrdering extends Ordering<Integer> {

        int clusterIndex = 0;
        public LikelihoodPriorRatioOrdering(int clusterIndex) {
            this.clusterIndex = clusterIndex;
        }

        @Override
        public int compare(Integer feature1, Integer feature2) {

            double leftRatio = Math.log(counts.likelihoodFeatureGivenCluster(feature1, clusterIndex)) - Math.log(counts.featurePrior(feature1));
            double rightRatio = Math.log(counts.likelihoodFeatureGivenCluster(feature2, clusterIndex)) - Math.log(counts.featurePrior(feature2));

            return Double.compare(leftRatio, rightRatio);
        }
    }

    /**
     * Order by:
     *
     *    P(feature|cluster) / P(feature|NOT-cluster)
     */
    public class LikelihoodsInAndOutOfClusterRatioOrdering extends Ordering<Integer> {

        int clusterIndex = 0;
        public LikelihoodsInAndOutOfClusterRatioOrdering(int clusterIndex) {
            this.clusterIndex = clusterIndex;
        }

        @Override
        public int compare(Integer feature1, Integer feature2) {

            double leftRatio = Math.log(counts.likelihoodFeatureGivenCluster(feature1, clusterIndex)) - Math.log(counts.likelihoodFeatureGivenNotCluster(feature1, clusterIndex));
            double rightRatio = Math.log(counts.likelihoodFeatureGivenCluster(feature2, clusterIndex)) - Math.log(counts.likelihoodFeatureGivenNotCluster(feature2, clusterIndex));

            return Double.compare(leftRatio, rightRatio);
        }
    }

}
