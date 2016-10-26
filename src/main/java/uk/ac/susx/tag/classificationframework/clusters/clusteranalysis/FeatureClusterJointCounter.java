package uk.ac.susx.tag.classificationframework.clusters.clusteranalysis;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntSet;
import uk.ac.susx.tag.classificationframework.clusters.ClusteredProcessedInstance;
import uk.ac.susx.tag.classificationframework.datastructures.Instance;
import uk.ac.susx.tag.classificationframework.featureextraction.inference.FeatureInferrer;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class for gathering statistics about features in clustered documents.
 *
 * User: Andrew D. Robertson
 * Date: 19/10/2015
 * Time: 12:11
 */
public abstract class FeatureClusterJointCounter implements Serializable {

    private static final long serialVersionUID = 0L;

    protected double featureSmoothingAlpha = 0.1;

    public void count(Collection<ClusteredProcessedInstance> documents, ClusterMembershipTest t, FeatureExtractionPipeline pipeline){
        count(documents, t, pipeline, true);
    }
    public void count(Collection<ClusteredProcessedInstance> documents, Iterable<Instance> backgroundDocuments, ClusterMembershipTest t, FeatureExtractionPipeline pipeline){
        count(documents, backgroundDocuments, t, pipeline, true);
    }

    public abstract void count(Collection<ClusteredProcessedInstance> documents, ClusterMembershipTest t, FeatureExtractionPipeline pipeline, boolean reInitialise);
    public abstract void count(Collection<ClusteredProcessedInstance> documents, Iterable<Instance> backgroundDocuments, ClusterMembershipTest t, FeatureExtractionPipeline pipeline, boolean reInitialise);

    // P(feature)
    public double featurePrior(int feature) { return featurePrior(feature, FeatureType.WORD); }
    public abstract double featurePrior(int feature, FeatureType t);

    // P(feature | cluster)
    public double likelihoodFeatureGivenCluster(int feature, int cluster) { return likelihoodFeatureGivenCluster(feature, cluster, FeatureType.WORD); }
    public abstract double likelihoodFeatureGivenCluster(int feature, int cluster, FeatureType t);

    // P(feature | !cluster)
    public double likelihoodFeatureGivenNotCluster(int feature, int cluster) { return likelihoodFeatureGivenNotCluster(feature, cluster, FeatureType.WORD); }
    public abstract double likelihoodFeatureGivenNotCluster(int feature, int cluster, FeatureType t);


    public IntSet getFeatures(){ return getFeatures(FeatureType.WORD); }
    public abstract IntSet getFeatures(FeatureType t);
    public IntSet getFeaturesInCluster(int clusterIndex){ return getFeaturesInCluster(clusterIndex, FeatureType.WORD); }
    public abstract IntSet getFeaturesInCluster(int clusterIndex, FeatureType t);

    public int getFeatureCount(int feature) { return getFeatureCount(feature, FeatureType.WORD); }
    public abstract int getFeatureCount(int feature, FeatureType t);
    public int getJointCount(int feature, int cluster) { return getJointCount(feature, cluster, FeatureType.WORD);}
    public abstract int getJointCount(int feature, int cluster, FeatureType t);

    public abstract void pruneFeaturesWithCountLessThan(int n);
    public abstract void pruneOnlyBackgroundFeaturesWithCountLessThan(int n);
    public abstract void pruneOnlyClusterFeaturesWithCountLessThan(int n);

    public double getFeatureSmoothingAlpha() {
        return featureSmoothingAlpha;
    }

    public void setFeatureSmoothingAlpha(double featureSmoothingAlpha) {
        this.featureSmoothingAlpha = featureSmoothingAlpha;
    }

//    /**
//     * A count of 1 for a feature means that the feature occurred at least once in exactly 1 document.
//     *
//     * A joint count of 1 for a feature in a cluster means that the feature occurred at least once
//     * in exactly 1 document in the given cluster.
//     */
//    public static class DocumentBasedCounts extends FeatureClusterJointCounter {
//
//        public int numDocuments;
//        public int[] numDocumentsPerCluster;
//        public Int2IntOpenHashMap featureCounts;
//        public Int2IntOpenHashMap[] jointCounts;
//
//        @Override
//        public void count(Collection<ClusteredProcessedInstance> documents, ClusterMembershipTest t, FeatureExtractionPipeline pipeline) {
//            // Initialise the counting data structures
//            int numClusters = documents.iterator().next().getClusterVector().length;
//
//            numDocumentsPerCluster = new int[numClusters];
//            featureCounts = new Int2IntOpenHashMap();
//            jointCounts = new Int2IntOpenHashMap[numClusters];
//            for (int i = 0; i < jointCounts.length; i++)
//                jointCounts[i] = new Int2IntOpenHashMap();
//
//            // Obtain feature counts, and joint counts of features per cluster
//            numDocuments = documents.size();
//
//            for (ClusteredProcessedInstance instance : documents) {
//
//                t.setup(instance);
//
//                IntSet features = new IntOpenHashSet(instance.getDocument().features);
//
//                for (int feature : features)
//                    featureCounts.addTo(feature, 1);
//
//                for (int clusterIndex=0; clusterIndex < numClusters; clusterIndex++){
//                    if (t.isDocumentInCluster(instance, clusterIndex)){
//                        numDocumentsPerCluster[clusterIndex]++;
//                        for (int feature : features) {
//                            jointCounts[clusterIndex].addTo(feature, 1);
//                        }
//                    }
//                }
//            }
//        }
//
//        @Override
//        public void count(Collection<ClusteredProcessedInstance> documents, Iterable<Instance> backgroundDocuments, ClusterMembershipTest t, FeatureExtractionPipeline pipeline) {
//            // Initialise the counting data structures
//            int numClusters = documents.iterator().next().getClusterVector().length;
//
//            numDocumentsPerCluster = new int[numClusters];
//            featureCounts = new Int2IntOpenHashMap();
//            jointCounts = new Int2IntOpenHashMap[numClusters];
//            for (int i = 0; i < jointCounts.length; i++)
//                jointCounts[i] = new Int2IntOpenHashMap();
//
//            // Obtain feature counts, and joint counts of features per cluster
//            numDocuments = documents.size();
//
//            for (ClusteredProcessedInstance instance : documents) {
//
//                t.setup(instance);
//
//                IntSet features = new IntOpenHashSet(instance.getDocument().features);
//
//                for (int clusterIndex=0; clusterIndex < numClusters; clusterIndex++){
//                    if (t.isDocumentInCluster(instance, clusterIndex)){
//                        numDocumentsPerCluster[clusterIndex]++;
//                        for (int feature : features) {
//                            jointCounts[clusterIndex].addTo(feature, 1);
//                        }
//                    }
//                }
//            }
//
//            for (ProcessedInstance instance : backgroundDocuments) {
//
//                IntSet features = new IntOpenHashSet(instance.features);
//
//                for (int feature : features)
//                    featureCounts.addTo(feature, 1);
//            }
//        }
//
//        @Override
//        public double featurePrior(int feature) {
//            return (featureCounts.get(feature)+1) / ((double)numDocuments + 1);
//        }
//
//        @Override
//        public double likelihoodFeatureGivenCluster(int feature, int cluster) {
//            return jointCounts[cluster].get(feature) / (double)numDocumentsPerCluster[cluster];
//        }
//
////        @Override
////        public double likelihoodFeatureGivenNotCluster(int feature, int cluster) {
////            int countInOtherClusters = featureCounts.get(feature) - jointCounts[cluster].get(feature);
////            int totalDocsInOtherClusters = numDocuments - numDocumentsPerCluster[cluster];
////            return countInOtherClusters / (double)totalDocsInOtherClusters;
////        }
//
//        @Override
//        public IntSet getFeatures() {
//            return featureCounts.keySet();
//        }
//
//        @Override
//        public IntSet getFeaturesInCluster(int clusterIndex, FEATURE_TYPE t) {
//            // TODO
//            return jointCounts[clusterIndex].keySet();
//        }
//
//        @Override
//        public int getFeatureCount(int feature) {
//            return featureCounts.get(feature);
//        }
//
//        @Override
//        public int getJointCount(int feature, int cluster) {
//            return jointCounts[cluster].get(feature);
//        }
//
//        @Override
//        public void pruneFeaturesWithCountLessThan(int n) {
//            Iterator<Int2IntMap.Entry> iter = featureCounts.int2IntEntrySet().fastIterator();
//            while(iter.hasNext()) {
//                Int2IntMap.Entry e = iter.next();
//                int feature  = e.getIntKey();
//                int count = e.getIntValue();
//                if (count < n) {
//                    iter.remove();
//                    for (Int2IntMap jointCount : jointCounts){
//                        jointCount.remove(feature);
//                    }
//                }
//            }
//        }
//
//        @Override
//        public void pruneOnlyBackgroundFeaturesWithCountLessThan(int n) {
//
//        }
//
//        @Override
//        public void pruneOnlyClusterFeaturesWithCountLessThan(int n) {
//
//        }
//
//    }

    /**
     * A count of N for a feature means that the feature occurred exactly N times in the corpus,
     * the occurrences were in 1 or more documents.
     */
    public static class FeatureBasedCounts extends FeatureClusterJointCounter {

        public int numClusters;

        public int totalFeatureCount;
        public int totalHashTagCount;
        public int totalAccountTagCount;

        public Int2IntOpenHashMap featureCounts;
        public Int2IntOpenHashMap hashTagCounts;
        public Int2IntOpenHashMap accountTagCounts;

        public Int2IntOpenHashMap[] jointCounts;
        public Int2IntOpenHashMap[] hashTagJointCounts;
        public Int2IntOpenHashMap[] accountTagJointCounts;

        private int[] totalFeatureCountPerCluster;
        private int[] totalHashTagCountPerCluster;
        private int[] totalAccountTagCountPerCluster;

        public FeatureBasedCounts() {
            this(1);
        }

        public FeatureBasedCounts(int numClusters){
            initialise(numClusters);
        }

        public void initialise(int numClusters){
            this.numClusters = numClusters;

            totalFeatureCount = 0;
            totalHashTagCount = 0;
            totalAccountTagCount = 0;

            totalFeatureCountPerCluster = new int[numClusters];
            totalHashTagCountPerCluster = new int[numClusters];
            totalAccountTagCountPerCluster = new int[numClusters];

            featureCounts = new Int2IntOpenHashMap();
            hashTagCounts = new Int2IntOpenHashMap();
            accountTagCounts = new Int2IntOpenHashMap();

            jointCounts = new Int2IntOpenHashMap[numClusters];
            hashTagJointCounts = new Int2IntOpenHashMap[numClusters];
            accountTagJointCounts = new Int2IntOpenHashMap[numClusters];
            for (int i = 0; i < jointCounts.length; i++) {
                jointCounts[i] = new Int2IntOpenHashMap();
                hashTagJointCounts[i] = new Int2IntOpenHashMap();
                accountTagJointCounts[i] = new Int2IntOpenHashMap();
            }
        }

        @Override
        public void count(Collection<ClusteredProcessedInstance> documents, ClusterMembershipTest t, FeatureExtractionPipeline pipeline, boolean reInitialise) {

            if (reInitialise) {
                int numClusters = documents.iterator().next().getClusterVector().length;
                initialise(numClusters);
            }

            // Obtain feature counts, and joint counts of features per cluster
            for (ClusteredProcessedInstance instance : documents) {

                t.setup(instance);

                int[] features = instance.getDocument().features;

                IntList words = new IntArrayList();
                IntList hashTags = new IntArrayList();
                IntList accountTags = new IntArrayList();

                for (int feature : features) {
                    String f = pipeline.featureString(feature, "**UNKNOWN**");
                    if (f.startsWith("#")){
                        hashTags.add(feature);
                    } else if (f.startsWith("@")){
                        accountTags.add(feature);
                    } else {
                        words.add(feature);
                    }
                }

                totalFeatureCount += words.size();
                for (int feature : features)
                    featureCounts.addTo(feature, 1);
                totalHashTagCount += hashTags.size();
                for (int hashTag : hashTags)
                    hashTagCounts.addTo(hashTag, 1);
                totalAccountTagCount += accountTags.size();
                for (int accountTag : accountTags)
                    accountTagCounts.addTo(accountTag, 1);


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
        public void count(Collection<ClusteredProcessedInstance> documents, Iterable<Instance> backgroundDocuments, ClusterMembershipTest t, FeatureExtractionPipeline pipeline, boolean reInitialise) {
            //TODO: inspect the hashtag issue, why do we count separately? Why do all clustered document counts go togther ?

            // Initialise the counting data structures
            if (reInitialise) {
                int numClusters = documents.iterator().next().getClusterVector().length;
                initialise(numClusters);
            }

            //  joint counts of features per cluster
            for (ClusteredProcessedInstance instance : documents) {

                t.setup(instance);

                int[] features = instance.getDocument().features;

                IntList words = new IntArrayList();
                IntList hashTags = new IntArrayList();
                IntList accountTags = new IntArrayList();

                for (int feature : features) {
                    String f = pipeline.featureString(feature, "**UNKNOWN**");
                    if (f.startsWith("#")){
                        hashTags.add(feature);
                    } else if (f.startsWith("@")){
                        accountTags.add(feature);
                    } else {
                        words.add(feature);
                    }
                }

                for (int clusterIndex=0; clusterIndex < numClusters; clusterIndex++){
                    if (t.isDocumentInCluster(instance, clusterIndex)){

                        // Add joint counts and cluster totals for the words
                        totalFeatureCountPerCluster[clusterIndex] += words.size();
                        for (int word : words) {
                            jointCounts[clusterIndex].addTo(word, 1);
                        }

                        // Add joint counts, cluster totals, and background counts for hashtags/accounttags
                        totalAccountTagCountPerCluster[clusterIndex] += accountTags.size();
                        for(int accountTag : accountTags){
                            accountTagJointCounts[clusterIndex].addTo(accountTag, 1);
                        }

                        totalHashTagCountPerCluster[clusterIndex] += hashTags.size();
                        for (int hashTag : hashTags){
                            hashTagJointCounts[clusterIndex].addTo(hashTag, 1);
                        }
                    }
                }

                // Use all clusters as background data for hash and account tags
                totalHashTagCount += hashTags.size();
                for (int hashTag : hashTags){
                    hashTagCounts.addTo(hashTag, 1);
                }

                totalAccountTagCount += accountTags.size();
                for (int accountTag : accountTags){
                    accountTagCounts.addTo(accountTag, 1);
                }
            }

            for (Instance instance : backgroundDocuments) {

                List<String> words = pipeline.extractUnindexedFeatures(instance).stream()
                                        .map(FeatureInferrer.Feature::value)
                                        .filter(f -> !f.startsWith("#") && !f.startsWith("@"))
                                        .collect(Collectors.toList());

                totalFeatureCount += words.size();

                for (String word : words)
                    featureCounts.addTo(pipeline.featureIndex(word), 1);
            }
        }

        @Override
        public double featurePrior(int feature, FeatureType t){
            switch (t) {
                case WORD:
                    return (featureCounts.get(feature) + getFeatureSmoothingAlpha())
                            / ((double)totalFeatureCount + getFeatureSmoothingAlpha()*featureCounts.size());
                case HASH_TAG:
                    return hashTagCounts.get(feature) / (double) totalHashTagCount;
                case ACCOUNT_TAG:
                    return accountTagCounts.get(feature) / (double) totalAccountTagCount;
                default: throw new RuntimeException("Invalid feature type");
            }
        }

        public double likelihoodFeatureGivenNotCluster(int feature, int cluster, FeatureType t) {
            Int2IntOpenHashMap[] counts;
            switch(t){
                case WORD:
                    counts = jointCounts; break;
                case HASH_TAG:
                    counts = hashTagJointCounts; break;
                case ACCOUNT_TAG:
                    counts = accountTagJointCounts; break;
                default: throw new RuntimeException("Invalid feature type");
            }
            int count = 0;
            for (int otherCluster = 0; otherCluster < counts.length; otherCluster++){
                if (cluster != otherCluster){
                    count += counts[otherCluster].get(feature);
                }
            }
            return count;
        }

        @Override
        public double likelihoodFeatureGivenCluster(int feature, int cluster, FeatureType t) {
            switch (t){
                case WORD:
                    return jointCounts[cluster].get(feature) / (double)totalFeatureCountPerCluster[cluster];
                case HASH_TAG:
                    return hashTagJointCounts[cluster].get(feature) / (double)totalHashTagCountPerCluster[cluster];
                case ACCOUNT_TAG:
                    return accountTagJointCounts[cluster].get(feature) / (double)totalAccountTagCountPerCluster[cluster];
                default:
                    throw new RuntimeException("Invalid feature type.");
            }
        }

        @Override
        public IntSet getFeatures(FeatureType t) {
            switch (t){
                case WORD:
                    return featureCounts.keySet();
                case HASH_TAG:
                    return hashTagCounts.keySet();
                case ACCOUNT_TAG:
                    return accountTagCounts.keySet();
                default:
                    throw new RuntimeException("Invalid enum value for feature type.");
            }
        }

        @Override
        public IntSet getFeaturesInCluster(int clusterIndex, FeatureType t) {
            switch (t){
                case WORD:
                    return jointCounts[clusterIndex].keySet();
                case HASH_TAG:
                    return hashTagJointCounts[clusterIndex].keySet();
                case ACCOUNT_TAG:
                    return accountTagJointCounts[clusterIndex].keySet();
                default:
                    throw new RuntimeException("Invalid enum value for feature type.");
            }
        }

        @Override
        public int getFeatureCount(int feature, FeatureType t) {
            switch (t){
                case WORD:
                    return featureCounts.get(feature);
                case HASH_TAG:
                    return hashTagCounts.get(feature);
                case ACCOUNT_TAG:
                    return accountTagCounts.get(feature);
                default: throw new RuntimeException("Invalid feature type.");
            }

        }

        @Override
        public int getJointCount(int feature, int cluster, FeatureType t) {
            switch (t) {
                case WORD:
                    return jointCounts[cluster].get(feature);
                case HASH_TAG:
                    return hashTagJointCounts[cluster].get(feature);
                case ACCOUNT_TAG:
                    return accountTagJointCounts[cluster].get(feature);
                default:
                    throw new RuntimeException("Invalid feature type");
            }

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

        @Override
        public void pruneOnlyBackgroundFeaturesWithCountLessThan(int n) {
            Iterator<Int2IntMap.Entry> iter = featureCounts.int2IntEntrySet().fastIterator();
            while (iter.hasNext()){
                Int2IntMap.Entry e = iter.next();
//                int feature = e.getIntKey();
                int count = e.getIntValue();
                if (count < n) {
                    totalFeatureCount -= count;
                    iter.remove();
                }
            }
        }

        @Override
        public void pruneOnlyClusterFeaturesWithCountLessThan(int n) {
            for (int c = 0; c < jointCounts.length; c++){
                Iterator<Int2IntMap.Entry> iter = jointCounts[c].int2IntEntrySet().fastIterator();
                while(iter.hasNext()){
                    Int2IntMap.Entry e = iter.next();
                    int count = e.getIntValue();
                    if (count < n) {
                        totalFeatureCountPerCluster[c] -= count;
                        iter.remove();
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

    public static class ProbabilityAboveUniform extends ProbabilityAboveThreshold {
        public ProbabilityAboveUniform(int numClusters) {
            super(1 / numClusters);
        }
    }
}
