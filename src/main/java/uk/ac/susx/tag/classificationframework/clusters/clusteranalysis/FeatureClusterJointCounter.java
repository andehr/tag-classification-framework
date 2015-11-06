package uk.ac.susx.tag.classificationframework.clusters.clusteranalysis;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import uk.ac.susx.tag.classificationframework.clusters.ClusteredProcessedInstance;

import java.util.Collection;
import java.util.Iterator;

/**
 * Class for gathering statistics about features in clustered documents.
 *
 * User: Andrew D. Robertson
 * Date: 19/10/2015
 * Time: 12:11
 */
public abstract class FeatureClusterJointCounter {

    public abstract void count(Collection<ClusteredProcessedInstance> documents, ClusterMembershipTest t);

    public abstract double featurePrior(int feature);

    public abstract double likelihoodFeatureGivenCluster(int feature, int cluster);

    public abstract double likelihoodFeatureGivenNotCluster(int feature, int cluster);

    public abstract IntSet getFeatures();

    public abstract IntSet getFeaturesInCluster(int clusterIndex);

    public abstract int getFeatureCount(int feature);

    public abstract int getJoinCount(int feature, int cluster);

    public abstract void pruneFeaturesWithCountLessThan(int n);

    /**
     * A count of 1 for a feature means that the feature occurred at least once in exactly 1 document.
     *
     * A joint count of 1 for a feature in a cluster means that the feature occurred at least once
     * in exactly 1 document in the given cluster.
     */
    public static class DocumentBasedCounts extends FeatureClusterJointCounter {

        public int numDocuments;
        public int[] numDocumentsPerCluster;
        public Int2IntOpenHashMap featureCounts;
        public Int2IntOpenHashMap[] jointCounts;

        @Override
        public void count(Collection<ClusteredProcessedInstance> documents, ClusterMembershipTest t) {
            // Initialise the counting data structures
            int numClusters = documents.iterator().next().getClusterVector().length;

            numDocumentsPerCluster = new int[numClusters];
            featureCounts = new Int2IntOpenHashMap();
            jointCounts = new Int2IntOpenHashMap[numClusters];
            for (int i = 0; i < jointCounts.length; i++)
                jointCounts[i] = new Int2IntOpenHashMap();

            // Obtain feature counts, and joint counts of features per cluster
            numDocuments = documents.size();

            for (ClusteredProcessedInstance instance : documents) {

                t.setup(instance);

                IntSet features = new IntOpenHashSet(instance.getDocument().features);

                for (int feature : features)
                    featureCounts.addTo(feature, 1);

                for (int clusterIndex=0; clusterIndex < numClusters; clusterIndex++){
                    if (t.isDocumentInCluster(instance, clusterIndex)){
                        numDocumentsPerCluster[clusterIndex]++;
                        for (int feature : features) {
                            jointCounts[clusterIndex].addTo(feature, 1);
                        }
                    }
                }
            }
        }

        @Override
        public double featurePrior(int feature) {
            return featureCounts.get(feature) / (double)numDocuments;
        }

        @Override
        public double likelihoodFeatureGivenCluster(int feature, int cluster) {
            return jointCounts[cluster].get(feature) / (double)numDocumentsPerCluster[cluster];
        }

        @Override
        public double likelihoodFeatureGivenNotCluster(int feature, int cluster) {
            int countInOtherClusters = featureCounts.get(feature) - jointCounts[cluster].get(feature);
            int totalDocsInOtherClusters = numDocuments - numDocumentsPerCluster[cluster];
            return countInOtherClusters / (double)totalDocsInOtherClusters;
        }

        @Override
        public IntSet getFeatures() {
            return featureCounts.keySet();
        }

        @Override
        public IntSet getFeaturesInCluster(int clusterIndex) {
            return jointCounts[clusterIndex].keySet();
        }

        @Override
        public int getFeatureCount(int feature) {
            return featureCounts.get(feature);
        }

        @Override
        public int getJoinCount(int feature, int cluster) {
            return jointCounts[cluster].get(feature);
        }

        @Override
        public void pruneFeaturesWithCountLessThan(int n) {
            Iterator<Int2IntMap.Entry> iter = featureCounts.int2IntEntrySet().fastIterator();
            while(iter.hasNext()) {
                Int2IntMap.Entry e = iter.next();
                int feature  = e.getIntKey();
                int count = e.getIntValue();
                if (count < n) {
                    iter.remove();
                    for (Int2IntMap jointCount : jointCounts){
                        jointCount.remove(feature);
                    }
                }
            }
        }

    }

    /**
     * A count of N for a feature means that the feature occurred exactly N times in the corpus,
     * the occurrences were in 1 or more documents.
     */
    public static class FeatureBasedCounts extends FeatureClusterJointCounter {

        public int totalFeatureCount;
        public Int2IntOpenHashMap featureCounts;
        public Int2IntOpenHashMap[] jointCounts;
        private int[] totalFeatureCountPerCluster;

        @Override
        public void count(Collection<ClusteredProcessedInstance> documents, ClusterMembershipTest t) {
            // Initialise the counting data structures
            int numClusters = documents.iterator().next().getClusterVector().length;

            totalFeatureCount = 0;
            totalFeatureCountPerCluster = new int[numClusters];
            featureCounts = new Int2IntOpenHashMap();

            jointCounts = new Int2IntOpenHashMap[numClusters];
            for (int i = 0; i < jointCounts.length; i++)
                jointCounts[i] = new Int2IntOpenHashMap();


            // Obtain feature counts, and joint counts of features per cluster
            for (ClusteredProcessedInstance instance : documents) {

                t.setup(instance);

                int[] features = instance.getDocument().features;

                totalFeatureCount += features.length;

                for (int feature : features)
                    featureCounts.addTo(feature, 1);

                for (int clusterIndex=0; clusterIndex < numClusters; clusterIndex++){
                    if (t.isDocumentInCluster(instance, clusterIndex)){
                        totalFeatureCountPerCluster[clusterIndex] += features.length;
                        for (int feature : features) {
                            jointCounts[clusterIndex].addTo(feature, 1);
                        }
                    }
                }
            }
        }

        @Override
        public double featurePrior(int feature) {
            return featureCounts.get(feature) / (double)totalFeatureCount;
        }

        @Override
        public double likelihoodFeatureGivenCluster(int feature, int cluster) {
            return jointCounts[cluster].get(feature) / (double)totalFeatureCountPerCluster[cluster];
        }

        @Override
        public double likelihoodFeatureGivenNotCluster(int feature, int cluster) {
            int countInOtherClusters = featureCounts.get(feature) - jointCounts[cluster].get(feature);
            int totalFeaturesInOtherClusters = totalFeatureCount - totalFeatureCountPerCluster[cluster];
            return countInOtherClusters / (double)totalFeaturesInOtherClusters;
        }

        @Override
        public IntSet getFeatures() {
            return featureCounts.keySet();
        }

        @Override
        public IntSet getFeaturesInCluster(int clusterIndex) {
            return jointCounts[clusterIndex].keySet();
        }

        @Override
        public int getFeatureCount(int feature) {
            return featureCounts.get(feature);
        }

        @Override
        public int getJoinCount(int feature, int cluster) {
            return jointCounts[cluster].get(feature);
        }

        @Override
        public void pruneFeaturesWithCountLessThan(int n) {
            Iterator<Int2IntMap.Entry> iter = featureCounts.int2IntEntrySet().fastIterator();
            while (iter.hasNext()){
                Int2IntMap.Entry e = iter.next();
                int feature = e.getIntKey();
                int count = e.getIntValue();
                if (count < n) {
                    iter.remove();
                    totalFeatureCount -= count;
                    for (int i = 0; i < jointCounts.length; i++) {
                        totalFeatureCountPerCluster[i] -= jointCounts[i].get(feature);
                        jointCounts[i].remove(feature);
                    }
                }
            }
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
