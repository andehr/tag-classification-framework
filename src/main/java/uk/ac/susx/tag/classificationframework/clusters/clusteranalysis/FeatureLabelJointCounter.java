package uk.ac.susx.tag.classificationframework.clusters.clusteranalysis;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import uk.ac.susx.tag.classificationframework.clusters.ClusteredProcessedInstance;

import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: Andrew D. Robertson
 * Date: 19/10/2015
 * Time: 12:11
 */
public abstract class FeatureLabelJointCounter {

    public abstract double featurePrior(int feature);

    public abstract double likelihoodFeatureGivenLabel(int feature, int label);

    public static FeatureLabelJointCounter createDocumentBased(Collection<ClusteredProcessedInstance> documents, ClusterMembershipTest t){
        return new DocumentBasedCounts(documents, t);
    }

    public static FeatureLabelJointCounter createFeatureBased(Collection<ClusteredProcessedInstance> documents, ClusterMembershipTest t) {
        return new FeatureBasedCounts(documents, t);
    }

    public static class DocumentBasedCounts extends FeatureLabelJointCounter{

        public int numDocuments;
        public Int2IntOpenHashMap featureCounts;
        public Int2IntOpenHashMap jointCounts;

        public DocumentBasedCounts (Collection<ClusteredProcessedInstance> documents, ClusterMembershipTest t){

        }

        @Override
        public double featurePrior(int feature) {
            return 0;
        }

        @Override
        public double likelihoodFeatureGivenLabel(int feature, int label) {
            return 0;
        }
    }

    public static class FeatureBasedCounts extends FeatureLabelJointCounter{

        public int totalFeatureCount;
        public Int2IntOpenHashMap featureCounts;
        public Int2IntOpenHashMap[] jointCounts;
        private int[] totalFeatureCountPerCluster;

        public FeatureBasedCounts (Collection<ClusteredProcessedInstance> documents, ClusterMembershipTest t){

        }

        @Override
        public double featurePrior(int feature) {
            return 0;
        }

        @Override
        public double likelihoodFeatureGivenLabel(int feature, int label) {
            return 0;
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
