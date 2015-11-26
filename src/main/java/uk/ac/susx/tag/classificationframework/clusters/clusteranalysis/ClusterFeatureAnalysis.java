package uk.ac.susx.tag.classificationframework.clusters.clusteranalysis;

import com.google.common.collect.Ordering;
import uk.ac.susx.tag.classificationframework.clusters.ClusteredProcessedInstance;
import uk.ac.susx.tag.classificationframework.datastructures.Instance;
import uk.ac.susx.tag.classificationframework.datastructures.ProcessedInstance;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * WARNING: CREATING AN INSTANCE OF THIS CLASS WITH BACKGROUND DOCUMENTS WILL MAKE YOUR PIPELINE USE A NON-FIXED VOCABULARY
 *
 *
 * Analyse the features in clusters of documents.
 *
 * Some classes to know about:
 *
 *
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

    public enum FEATURE_TYPE {
        WORD, HASH_TAG, ACCOUNT_TAG
    }

    private FeatureClusterJointCounter counts;
    private int numOfClusters;

    public enum OrderingMethod {
        LIKELIHOOD_IN_CLUSTER_OVER_PRIOR,  // Essentially PMI: P(feature|cluster) / P(feature)
//        LIKELIHOOD_IN_CLUSTER_OVER_LIKELIHOOD_OUT // P(feature|cluster) / P(feature|!cluster)
    }

    public ClusterFeatureAnalysis(Collection<ClusteredProcessedInstance> documents, FeatureExtractionPipeline pipeline){
        this(documents, pipeline,
             new FeatureClusterJointCounter.FeatureBasedCounts(),
             new FeatureClusterJointCounter.HighestProbabilityOnly(),
             5);
    }

    public ClusterFeatureAnalysis(Collection<ClusteredProcessedInstance> documents,
                                  Iterable<Instance> backgroundDocuments,
                                  FeatureExtractionPipeline pipeline) {
        this(documents, backgroundDocuments, pipeline,
             new FeatureClusterJointCounter.FeatureBasedCounts(),
             new FeatureClusterJointCounter.HighestProbabilityOnly(),
             10,
             10);
    }

    public ClusterFeatureAnalysis(Collection<ClusteredProcessedInstance> documents,
                                  FeatureExtractionPipeline pipeline,
                                  FeatureClusterJointCounter c,
                                  FeatureClusterJointCounter.ClusterMembershipTest t,
                                  int minimumFeatureCount) {
        counts = c;
        numOfClusters = documents.iterator().next().getClusterVector().length;
        c.count(documents, t, pipeline);
        c.pruneFeaturesWithCountLessThan(minimumFeatureCount);
    }

    public ClusterFeatureAnalysis(Collection<ClusteredProcessedInstance> documents,
                                  Iterable<Instance> backgroundDocuments,
                                  FeatureExtractionPipeline pipeline,
                                  FeatureClusterJointCounter c,
                                  FeatureClusterJointCounter.ClusterMembershipTest t,
                                  int minimumBackgroundFeatureCount,
                                  int minimumClusterFeatureCount){
        counts = c;
        numOfClusters = documents.iterator().next().getClusterVector().length;

        pipeline.setFixedVocabulary(false);

        c.count(documents, backgroundDocuments, t, pipeline);
        if (minimumBackgroundFeatureCount > 1) {
            c.pruneOnlyBackgroundFeaturesWithCountLessThan(minimumBackgroundFeatureCount);
        }
        if (minimumClusterFeatureCount > 1) {
            c.pruneOnlyClusterFeaturesWithCountLessThan(minimumClusterFeatureCount);
        }
    }

    public FeatureClusterJointCounter getCounts() {
        return counts;
    }

    public int getNumOfClusters() {
        return numOfClusters;
    }

    public List<Integer> getFrequentFeatures(int clusterIndex, int K, FEATURE_TYPE t){
        return new JointLikelihoodOrdering(clusterIndex, t)
                    .greatestOf(counts.getFeaturesInCluster(clusterIndex, t), K);
    }
    public List<String> getFrequentFeatures(int clusterIndex, int K, FEATURE_TYPE t, FeatureExtractionPipeline pipeline){
        return getFrequentFeatures(clusterIndex, K, t).stream()
                .map(pipeline::featureString)
                .collect(Collectors.toList());
    }

    public List<String> getTopFeatures(int clusterIndex, int K, FeatureExtractionPipeline pipeline, FEATURE_TYPE t){
        return getTopFeatures(clusterIndex, K, OrderingMethod.LIKELIHOOD_IN_CLUSTER_OVER_PRIOR, pipeline, t);
    }

    public List<String> getTopFeatures(int clusterIndex, int K, OrderingMethod m, FeatureExtractionPipeline pipeline, FEATURE_TYPE t){
        return getTopFeatures(clusterIndex, K, m, t).stream()
                .map(pipeline::featureString)
                .collect(Collectors.toList());
    }

    public List<Integer> getTopFeatures(int clusterIndex, int K, FEATURE_TYPE t){
        return getTopFeatures(clusterIndex, K, OrderingMethod.LIKELIHOOD_IN_CLUSTER_OVER_PRIOR, t);
    }

    /**
     * Get a list of the K most interesting features for a given cluster and ordering method.
     */
    public List<Integer> getTopFeatures(int clusterIndex, int K, OrderingMethod m, FEATURE_TYPE t) {
        Ordering<Integer> ordering;
        switch(m) {
//            case LIKELIHOOD_IN_CLUSTER_OVER_LIKELIHOOD_OUT:
//                ordering = new LikelihoodsInAndOutOfClusterRatioOrdering(clusterIndex); break;
            case LIKELIHOOD_IN_CLUSTER_OVER_PRIOR:
                ordering = new LikelihoodPriorRatioOrdering(clusterIndex, t); break;
            default:
                throw new RuntimeException("OrderingMethod not recognised.");

        }

        return ordering.greatestOf(counts.getFeaturesInCluster(clusterIndex, t), K);
    }


    public class LikelihoodPriorRatioOrdering extends Ordering<Integer> {

        int clusterIndex = 0;
        FEATURE_TYPE t;

        public LikelihoodPriorRatioOrdering(int clusterIndex, FEATURE_TYPE t) {
            this.clusterIndex = clusterIndex;
            this.t = t;
        }

        @Override
        public int compare(Integer feature1, Integer feature2) {

            double leftRatio = Math.log(counts.likelihoodFeatureGivenCluster(feature1, clusterIndex, t)) - Math.log(counts.featurePrior(feature1, t));
            double rightRatio = Math.log(counts.likelihoodFeatureGivenCluster(feature2, clusterIndex, t)) - Math.log(counts.featurePrior(feature2, t));
            return Double.compare(leftRatio, rightRatio);
        }
    }


    public class JointLikelihoodOrdering extends Ordering<Integer> {

        int clusterIndex;
        FEATURE_TYPE t;

        public JointLikelihoodOrdering(int clusterIndex, FEATURE_TYPE t){
            this.clusterIndex = clusterIndex;
            this.t = t;
        }

        @Override
        public int compare(Integer left, Integer right) {
            return Double.compare(counts.likelihoodFeatureGivenCluster(left, clusterIndex, t), counts.likelihoodFeatureGivenCluster(right, clusterIndex, t));
        }
    }

//    /**
//     * Order by:
//     *
//     *    P(feature|cluster) / P(feature|NOT-cluster)
//     */
//    public class LikelihoodsInAndOutOfClusterRatioOrdering extends Ordering<Integer> {
//
//        int clusterIndex = 0;
//        public LikelihoodsInAndOutOfClusterRatioOrdering(int clusterIndex) {
//            this.clusterIndex = clusterIndex;
//        }
//
//        @Override
//        public int compare(Integer feature1, Integer feature2) {
//
//            double leftRatio = Math.log(counts.likelihoodFeatureGivenCluster(feature1, clusterIndex)) - Math.log(counts.likelihoodFeatureGivenNotCluster(feature1, clusterIndex));
//            double rightRatio = Math.log(counts.likelihoodFeatureGivenCluster(feature2, clusterIndex)) - Math.log(counts.likelihoodFeatureGivenNotCluster(feature2, clusterIndex));
//
//            return Double.compare(leftRatio, rightRatio);
//        }
//
//    }

}
