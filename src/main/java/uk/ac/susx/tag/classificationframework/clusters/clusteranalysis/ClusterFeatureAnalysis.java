package uk.ac.susx.tag.classificationframework.clusters.clusteranalysis;

import com.google.common.collect.Ordering;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import uk.ac.susx.tag.classificationframework.clusters.ClusteredProcessedInstance;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Analyse the features within clusters.
 *
 * User: Andrew D. Robertson
 * Date: 16/10/2015
 * Time: 15:07
 */
public class ClusterFeatureAnalysis {

    private Int2IntOpenHashMap featureCounts;
    private int totalFeatureCount;

    private int numClusters;
    private Int2IntOpenHashMap[] jointCounts;
    private int[] totalFeatureCountPerCluster;

    public ClusterFeatureAnalysis(Collection<ClusteredProcessedInstance> documents){
        this(documents, new HighestProbabilityOnly());
    }

    public ClusterFeatureAnalysis(Collection<ClusteredProcessedInstance> documents, ClusterMembershipTest t) {
        numClusters = documents.iterator().next().getClusterVector().length;
        totalFeatureCount = 0;
        totalFeatureCountPerCluster = new int[numClusters];

        // Initialise the counting data structures
        jointCounts = new Int2IntOpenHashMap[numClusters];
        for (int i = 0; i < jointCounts.length; i++)
            jointCounts[i] = new Int2IntOpenHashMap();

        featureCounts = new Int2IntOpenHashMap();

        // Obtain counts
        for (ClusteredProcessedInstance instance : documents) {

            // Setup the membership testing for a new document
            t.setup(instance);

            int[] features = instance.getDocument().features;

            // Increment total feature count across all clusters
            totalFeatureCount += features.length;

            // Increment overall count of each feature seen
            for (int feature : features){
                featureCounts.addTo(feature, 1);
            }

            for (int clusterIndex=0; clusterIndex < numClusters; clusterIndex++){
                if (t.isDocumentInCluster(instance, clusterIndex)) {
                    totalFeatureCountPerCluster[clusterIndex] += features.length;

                    for (int feature : features){
                        jointCounts[clusterIndex].addTo(feature, 1);
                    }
                }
            }
        }
    }

    public List<String> getTopFeatures(int clusterIndex, int K, FeatureExtractionPipeline pipeline){
        return getTopFeatures(clusterIndex, K).stream()
                .map(pipeline::labelString)
                .collect(Collectors.toList());
    }

    public List<Integer> getTopFeatures(int clusterIndex, int K) {
        return new LikelihoodPriorRatioOrdering(clusterIndex).greatestOf(jointCounts[clusterIndex].keySet(), K);
    }

    private double featurePrior(int feature){
        return featureCounts.get(feature) / totalFeatureCount;
    }

    private double likelihoodFeatureGivenCluster(int feature, int cluster){
        return jointCounts[cluster].get(feature) / totalFeatureCountPerCluster[cluster];
    }

    public class LikelihoodPriorRatioOrdering extends Ordering<Integer> {

        int clusterIndex = 0;

        public LikelihoodPriorRatioOrdering(int clusterIndex) {
            this.clusterIndex = clusterIndex;
        }

        public void setClusterIndex(int clusterIndex) {
            this.clusterIndex = clusterIndex;
        }

        @Override
        public int compare(Integer feature1, Integer feature2) {

            double leftRatio = Math.log(featurePrior(feature1)) - Math.log(likelihoodFeatureGivenCluster(feature1, clusterIndex));
            double rightRatio = Math.log(featurePrior(feature2)) - Math.log(likelihoodFeatureGivenCluster(feature2, clusterIndex));

            return Double.compare(leftRatio, rightRatio);
        }
    }

    /**
     * Cluster membership testing.
     *
     * In order to produce the counts of how many times a feature occurs within a particular cluster,
     * we must be able to decide whether or not a document is in a cluster.
     *
     * A class implementing the ClusterMembershipTest gets to decide whether a document is considered
     * part of a cluster.
     *
     * For example, the HighestProbabilityOnly implementation allows the document to only be part of the
     * cluster with which it has the highest probability of membership (clusterVector being treated as
     * vector of membership probabilities).
     */
    public interface ClusterMembershipTest {

        /**
         * Called once per document. Perform any setup required before the cluster membership
         * testing happens for each cluster.
         */
        void setup(ClusteredProcessedInstance instance);

        /**
         * Called N times per document, where N is the number of clusters.
         * Must return true if the document is considered to belong to the specified cluster.
         */
        boolean isDocumentInCluster(ClusteredProcessedInstance instance, int clusterIndex);
    }

    /**
     * Allows the document to only be part of the cluster with which it has the highest
     * probability of membership (clusterVector being treated as vector of membership probabilities).
     */
    public static class HighestProbabilityOnly implements ClusterMembershipTest{
        private int highestClusterIndex = 0;

        public void setup(ClusteredProcessedInstance instance){
            double[] clusterVector = instance.getClusterVector();
            highestClusterIndex = 0;
            double currentMax = clusterVector[0];
            for (int i = 0; i < clusterVector.length; i++) {
                if (clusterVector[i] > currentMax) {
                    highestClusterIndex = i;
                    currentMax = clusterVector[i];
                }
            }
        }

        public boolean isDocumentInCluster(ClusteredProcessedInstance instance, int clusterIndex) {
            return clusterIndex == highestClusterIndex;
        }
    }

    /**
     * Allows the document to be part of any cluster for which the document's membership
     * probability is equal to or higher than some threshold.
     */
    public static class ProbabilityAboveThreshold implements ClusterMembershipTest{

        private final double threshold;

        public ProbabilityAboveThreshold(double threshold){
            this.threshold = threshold;
        }

        public void setup(ClusteredProcessedInstance instance){
            // None required
        }

        public boolean isDocumentInCluster(ClusteredProcessedInstance instance, int clusterIndex) {
            return instance.getClusterVector()[clusterIndex] >= threshold;
        }
    }

}
