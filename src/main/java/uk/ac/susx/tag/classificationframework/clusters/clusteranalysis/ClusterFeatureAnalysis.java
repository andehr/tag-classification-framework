package uk.ac.susx.tag.classificationframework.clusters.clusteranalysis;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;
import uk.ac.susx.tag.classificationframework.clusters.ClusteredProcessedInstance;
import uk.ac.susx.tag.classificationframework.datastructures.Instance;
import uk.ac.susx.tag.classificationframework.datastructures.ProcessedInstance;
import uk.ac.susx.tag.classificationframework.datastructures.RootedNgramCounter;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static uk.ac.susx.tag.classificationframework.clusters.clusteranalysis.FeatureClusterJointCounter.ClusterMembershipTest;

/**
 *
 * Analyse the features in clusters of documents.
 *
 * WARNINGS:
 *   - CREATING AN INSTANCE OF THIS CLASS WITH BACKGROUND DOCUMENTS WILL MAKE YOUR PIPELINE USE A NON-FIXED VOCABULARY
 *   - This class maintains references to your clustered documents (not background), and maintains counts of all features
 *     it has seen.
 *   - Currently, only FeatureBasedCounts with Background documents is implemented fully.
 *
 * Workflow info:
 *
 *    - Words, hashtags, and account tags are considered different feature types. When selecting topFeatures() or
 *      topPhrases() you'll probably specify which kind you are interested in.
 *    - Top hashtags and account tags are selected using frequency.
 *    - Top words primarily use PMI.
 *    - Top phrases use the PMI words and frequency counts of surrounding ngrams.
 *    - use getTopFeatures() for lists of just top features.
 *    - use getTopPhrases() for mapping from top features to their surrounding frequent ngrams.
 *
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

    // Allows specificatino of the type of feature we're interested in when asking for top features.
    public enum FEATURE_TYPE {
        WORD, HASH_TAG, ACCOUNT_TAG
    }

    private FeatureClusterJointCounter counts;
    private Collection<ClusteredProcessedInstance> documents;
    private ClusterMembershipTest t;
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
             5,
             5);
    }

    public ClusterFeatureAnalysis(Collection<ClusteredProcessedInstance> documents,
                                  FeatureExtractionPipeline pipeline,
                                  FeatureClusterJointCounter c,
                                  ClusterMembershipTest t,
                                  int minimumFeatureCount) {
        counts = c;
        numOfClusters = documents.iterator().next().getClusterVector().length;
        this.documents = documents;
        this.t = t;
        c.count(documents, t, pipeline);
        c.pruneFeaturesWithCountLessThan(minimumFeatureCount);
    }

    public ClusterFeatureAnalysis(Collection<ClusteredProcessedInstance> documents,
                                  Iterable<Instance> backgroundDocuments,
                                  FeatureExtractionPipeline pipeline,
                                  FeatureClusterJointCounter c,
                                  ClusterMembershipTest t,
                                  int minimumBackgroundFeatureCount,
                                  int minimumClusterFeatureCount){
        counts = c;
        numOfClusters = documents.iterator().next().getClusterVector().length;
        this.documents = documents;
        this.t = t;

        pipeline.setFixedVocabulary(false);

        c.count(documents, backgroundDocuments, t, pipeline);
        if (minimumBackgroundFeatureCount > 1) {
            c.pruneOnlyBackgroundFeaturesWithCountLessThan(minimumBackgroundFeatureCount);
        }
        if (minimumClusterFeatureCount > 1) {
            c.pruneOnlyClusterFeaturesWithCountLessThan(minimumClusterFeatureCount);
        }
    }

    public static List<List<Integer>> getTopFeatures(
            int K,
            OrderingMethod method,
            FEATURE_TYPE featureType,
            Collection<ClusteredProcessedInstance> documents,
            Iterable<Instance> backgroundDocuments,
            FeatureExtractionPipeline pipeline,
            FeatureClusterJointCounter counts,
            ClusterMembershipTest t,
            int minimumBackgroundFeatureCount,
            int minimumClusterFeatureCount){

        int numOfClusters = documents.iterator().next().getClusterVector().length;
        pipeline.setFixedVocabulary(false);

        // Do feature counting
        counts.count(documents, backgroundDocuments, t, pipeline);

        // Do feature pruning by frequency
        if (minimumBackgroundFeatureCount > 1){
            counts.pruneOnlyBackgroundFeaturesWithCountLessThan(minimumBackgroundFeatureCount);
        }
        if (minimumClusterFeatureCount > 1) {
            counts.pruneOnlyClusterFeaturesWithCountLessThan(minimumClusterFeatureCount);
        }

        List<List<Integer>> topFeaturesPerCluster = new ArrayList<>();

        // Get top features for each cluster
        for (int clusterIndex = 0; clusterIndex < numOfClusters; clusterIndex++){
            // Establish the feature ranking method
            Ordering<Integer> ordering;
            switch(method){
                case LIKELIHOOD_IN_CLUSTER_OVER_PRIOR:
                    ordering = new LikelihoodPriorRatioOrdering(clusterIndex, featureType, counts); break;
                default: throw new RuntimeException("OrderingMethod not recognised");
            }

            topFeaturesPerCluster.add(ordering.greatestOf(counts.getFeaturesInCluster(clusterIndex, featureType), K));
        }

        return topFeaturesPerCluster;
    }

    public static Map<String, List<String>> getTopPhrases(int clusterIndex,
                                                                List<Integer> topFeatures,
                                                                List<ClusteredProcessedInstance> documents,
                                                                FeatureExtractionPipeline pipeline,
                                                                ClusterMembershipTest t,
                                                                int numPhrasesPerFeature,
                                                                double leafPruningThreshold,
                                                                int minPhraseSize,
                                                                int maxPhraseSize){

        Map<Integer, List<List<Integer>>> indexedTopPhrases = getTopPhrases(
                clusterIndex, topFeatures, documents, t, numPhrasesPerFeature, leafPruningThreshold, minPhraseSize, maxPhraseSize
        );

        Map<String, List<String>> topPhrasesPerFeature = new LinkedHashMap<>();

        for (Map.Entry<Integer, List<List<Integer>>> entry : indexedTopPhrases.entrySet()){
//            List<String> phrases = new ArrayList<>();
//            for (List<Integer> ngram : entry.getValue()){
//                phrases.add(Joiner.on(" ").join(ngram.stream().map(pipeline::featureString).collect(Collectors.toList())));
//            }
            List<String> phrases = entry.getValue().stream()
                    .map(ngram -> (ngram.stream().map(pipeline::featureString).collect(Collectors.joining(" "))))
                    .collect(Collectors.toList());
            topPhrasesPerFeature.put(pipeline.featureString(entry.getKey()), phrases);
        }
        return topPhrasesPerFeature;
    }

    public static Map<Integer, List<List<Integer>>> getTopPhrases(int clusterIndex,
                                                                  List<Integer> topFeatures,
                                                                  List<ClusteredProcessedInstance> documents,
                                                                  ClusterMembershipTest t,
                                                                  int numPhrasesPerFeature,
                                                                  double leafPruningThreshold,
                                                                  int minPhraseSize,
                                                                  int maxPhraseSize){

        List<RootedNgramCounter<Integer>> counters = topFeatures.stream()
                                                        .map(f -> new RootedNgramCounter<>(f))
                                                        .collect(Collectors.toList());
        // For each document that is in the relevant cluster, count occurrences of surrounding words of each word of interest
        for (ClusteredProcessedInstance document : documents) {
            t.setup(document);
            if (t.isDocumentInCluster(document, clusterIndex)){
                for(RootedNgramCounter<Integer> counter : counters){
                    counter.countNgramsInContext(Ints.asList(document.getDocument().features), 1, minPhraseSize, maxPhraseSize);
                }
            }
        }

        // For each word of interest, pick the longest most frequent phrases
        Map<Integer, List<List<Integer>>> topPhrasesPerFeature = new LinkedHashMap<>();
        for (RootedNgramCounter<Integer> counter : counters){
            topPhrasesPerFeature.put(counter.getRootToken(), counter.topNgrams(numPhrasesPerFeature, leafPruningThreshold));
        }

        return topPhrasesPerFeature;
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
                ordering = new LikelihoodPriorRatioOrdering(clusterIndex, t, counts); break;
            default:
                throw new RuntimeException("OrderingMethod not recognised.");

        }

        return ordering.greatestOf(counts.getFeaturesInCluster(clusterIndex, t), K);
    }

    public Map<String, List<String>> getTopPhrases(int clusterIndex, int numFeatures, int numPhrasesPerFeature, FeatureExtractionPipeline pipeline){
        return getTopPhrases(clusterIndex,
                numFeatures, numPhrasesPerFeature, pipeline,
                OrderingMethod.LIKELIHOOD_IN_CLUSTER_OVER_PRIOR, FEATURE_TYPE.WORD,
                0.3, 2, 6);
    }

    /**
     * Obtain a mapping from the top features (as produced by getTopFeatures()), to the top phrases that involve said
     * feature.
     * @param clusterIndex The cluster of interest
     * @param numFeatures  The max number of top features to consider
     * @param numPhrasesPerFeature The max number of top phrases to consider
     * @param pipeline used for de-indexing features
     * @param m The method of selecting the top features
     * @param featureType The type of feature of interest. Probably you want FEATURE_TYPE.WORD
     * @param leafPruningThreshold Used for deciding how long a phrase should be allowed to be. E.g. if set to 0.3, then
     *                             a longer ngram must account for >=30% of the occurrences of its shorter variant to be permissible.
     * @param minPhraseSize The minimum size ngram to be considered
     * @param maxPhraseSize The maximum size ngram to be considered.
     * @return A mapping from top features to the most common phrases they occur in.
     */
    public Map<String, List<String>> getTopPhrases(int clusterIndex,
                                                   int numFeatures,
                                                   int numPhrasesPerFeature,
                                                   FeatureExtractionPipeline pipeline,
                                                   OrderingMethod m,
                                                   FEATURE_TYPE featureType,
                                                   double leafPruningThreshold,
                                                   int minPhraseSize,
                                                   int maxPhraseSize ){

        Map<Integer, List<List<Integer>>> indexedTopPhrases = getTopPhrases(clusterIndex, numFeatures, numPhrasesPerFeature, m, featureType, leafPruningThreshold, minPhraseSize, maxPhraseSize);

        Map<String, List<String>> topPhrasesPerFeature = new LinkedHashMap<>();

        for (Map.Entry<Integer, List<List<Integer>>> entry : indexedTopPhrases.entrySet()){
            List<String> phrases = new ArrayList<>();
            for (List<Integer> ngram : entry.getValue()){
                phrases.add(Joiner.on(" ").join(ngram.stream().map(pipeline::featureString).collect(Collectors.toList())));
            }
            topPhrasesPerFeature.put(pipeline.featureString(entry.getKey()), phrases);
        }
        return topPhrasesPerFeature;
    }

    public Map<Integer, List<List<Integer>>> getTopPhrases(int clusterIndex,
                                                     int numFeatures,
                                                     int numPhrasesPerFeature,
                                                     OrderingMethod m,
                                                     FEATURE_TYPE featureType,
                                                     double leafPruningThreshold,
                                                     int minPhraseSize,
                                                     int maxPhraseSize){


        List<Integer> features = getTopFeatures(clusterIndex, numFeatures, m, featureType);

        List<RootedNgramCounter<Integer>> counters = features.stream()
                                                        .map(f -> new RootedNgramCounter<>(f))
                                                        .collect(Collectors.toList());

        // For each document that is in the relevant cluster, count occurrences of surrounding words of each word of interest
        for (ClusteredProcessedInstance document : documents){
            t.setup(document);
            if (t.isDocumentInCluster(document, clusterIndex)){
                for (RootedNgramCounter<Integer> counter : counters){
                    counter.countNgramsInContext(Ints.asList(document.getDocument().features), 1, minPhraseSize, maxPhraseSize);
                }
            }
        }

        // For each word of interest, pick the longest most frequent phrases
        Map<Integer, List<List<Integer>>> topPhrases = new LinkedHashMap<>();
        for (RootedNgramCounter<Integer> counter : counters){
            topPhrases.put(counter.getRootToken(), counter.topNgrams(numPhrasesPerFeature, leafPruningThreshold));
        }

        return topPhrases;
    }


    public static class LikelihoodPriorRatioOrdering extends Ordering<Integer> {

        int clusterIndex = 0;
        FEATURE_TYPE t;
        FeatureClusterJointCounter theCounts;

        public LikelihoodPriorRatioOrdering(int clusterIndex, FEATURE_TYPE t, FeatureClusterJointCounter counts) {
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
