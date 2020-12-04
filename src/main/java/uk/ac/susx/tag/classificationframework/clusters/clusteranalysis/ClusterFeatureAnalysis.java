package uk.ac.susx.tag.classificationframework.clusters.clusteranalysis;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math.stat.clustering.Cluster;
import uk.ac.susx.tag.classificationframework.clusters.ClusteredProcessedInstance;
import uk.ac.susx.tag.classificationframework.datastructures.Instance;
import uk.ac.susx.tag.classificationframework.datastructures.ProcessedInstance;
import uk.ac.susx.tag.classificationframework.datastructures.RootedNgramCounter;
import uk.ac.susx.tag.classificationframework.datastructures.RootedNgramCounter.TopNgram;
import uk.ac.susx.tag.classificationframework.featureextraction.filtering.TokenFilterByRegex;
import uk.ac.susx.tag.classificationframework.featureextraction.filtering.TokenFilterRelevanceStopwords;
import uk.ac.susx.tag.classificationframework.featureextraction.inference.FeatureInferrer;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.PipelineBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static uk.ac.susx.tag.classificationframework.clusters.clusteranalysis.FeatureClusterJointCounter.*;
import static uk.ac.susx.tag.classificationframework.clusters.clusteranalysis.IncrementalSurprisingPhraseAnalysis.OrderingMethod.LIKELIHOOD_IN_TARGET_OVER_BACKGROUND;
import static uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline.PipelineChanges;

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

    // Allows specification of the type of feature we're interested in when asking for top features.
    private FeatureClusterJointCounter counts;
    private Collection<ClusteredProcessedInstance> documents;
    private ClusterMembershipTest t;
    private int numOfClusters;

    public enum OrderingMethod {
        LIKELIHOOD_IN_CLUSTER_OVER_PRIOR,  // Essentially PMI: P(feature|cluster) / P(feature)
    }

    public ClusterFeatureAnalysis(Collection<ClusteredProcessedInstance> documents, FeatureExtractionPipeline pipeline){
        this(documents, pipeline,
             new FeatureBasedCounts(),
             new HighestProbabilityOnly(),
             5);
    }

    public ClusterFeatureAnalysis(Collection<ClusteredProcessedInstance> documents,
                                  Iterable<Instance> backgroundDocuments,
                                  FeatureExtractionPipeline pipeline) {
        this(documents, backgroundDocuments, pipeline,
             new FeatureBasedCounts(),
             new HighestProbabilityOnly(),
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

    public static interface Logger {
        void log(String msg);
    }

    public static List<List<Integer>> getTopFeatures(
            int K,
            OrderingMethod method,
            FeatureType featureType,
            Collection<ClusteredProcessedInstance> documents,
            Iterable<Instance> backgroundDocuments,
            FeatureExtractionPipeline pipeline,
            FeatureClusterJointCounter counts,
            boolean reInitialiseCounts,
            ClusterMembershipTest t,
            int minimumBackgroundFeatureCount,
            int minimumClusterFeatureCount){
        return getTopFeatures(K, method, featureType, documents, backgroundDocuments, pipeline, counts, reInitialiseCounts, t, minimumBackgroundFeatureCount, minimumClusterFeatureCount, null);
    }

    public static List<List<Integer>> getTopFeatures(
            int K,
            OrderingMethod method,
            FeatureType featureType,
            Collection<ClusteredProcessedInstance> documents,
            Iterable<Instance> backgroundDocuments,
            FeatureExtractionPipeline pipeline,
            FeatureClusterJointCounter counts,
            boolean reInitialiseCounts,
            ClusterMembershipTest t,
            int minimumBackgroundFeatureCount,
            int minimumClusterFeatureCount,
            Logger logger){

        int numOfClusters = documents.iterator().next().getClusterVector().length;
        pipeline.setFixedVocabulary(false);

        if ( logger != null) logger.log("Counting");

        // Do feature counting
        counts.count(documents, backgroundDocuments, t, pipeline, reInitialiseCounts);

        if ( logger != null) logger.log("Pruning");

        if (!reInitialiseCounts) {
            // Do feature pruning by frequency
            if (minimumBackgroundFeatureCount > 1) {
                counts.pruneOnlyBackgroundFeaturesWithCountLessThan(minimumBackgroundFeatureCount);
            }
        }
        if (minimumClusterFeatureCount > 1) {
            counts.pruneOnlyClusterFeaturesWithCountLessThan(minimumClusterFeatureCount);
        }

        List<List<Integer>> topFeaturesPerCluster = new ArrayList<>();

        if ( logger != null) logger.log("Sorting.");
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

        if ( logger != null) logger.log("Features found.");

        return topFeaturesPerCluster;
    }

    /**
     *
     * @param clusterIndex The index of the cluster of interest
     * @param topFeatures Top features as obtained from getTopFeatures()
     * @param documents The same documents used for getTopFeatures(), ensure that indices match up if you did any reprocessing
     * @param pipeline The pipeline used during getTopFeatures()
     * @param t A ClusterMembershipTest instance; an object which knows how to test whether a document is in a cluster
     * @param numPhrasesPerFeature The number of phrases to attempt to find per feature if possible
     * @param minLeafPruningThreshold Each n+1gram appeared a fraction of the time that its parent ngram occurred.
     *                                This is the minimum threshold on that fraction that the n+1gram must have
     *                                occurred to be considered a part of the phrase. The actual threshold could
     *                                be higher based on the other parameters.
     * @param minimumCount            An ngram must have occurred at least this many times to be considered at all.
     * @param level1NgramCount        When the occurrences of an n+1gram's parent ngram is less than this threshold
     *                                the n+1gram must have occurred 100% of these times to be considered.
     * @param level2NgramCount        When the occurrences of an n+1gram's parent ngram is less than this threshold
     *                                the n+1gram must have occurred more than 75% of these times to be considered
     * @param level3NgramCount        When the occurrences of an n+1gram's parent ngram is less than this threshold
     *                                the n+1gram must have occurred more than 50% of these times to be considered
     * @param stopwords               A set of stopwords; when frequency does not differentiate between ngrams, the
     *                                number of stopwords an ngram contains, or whether or not the ngram ends with a
     *                                stopword can be used to pick more interesting ngrams.
     * @param minPhraseSize           The minimum size ngrams that we'll be interested in.
     * @param maxPhraseSize           The maximum size ngrams that we'll be interested in.
     * @return A map from top feature to its top phrases
     */
    public static Map<String, List<String>> getTopPhrases(int clusterIndex,
                                                                List<Integer> topFeatures,
                                                                List<ClusteredProcessedInstance> documents,
                                                                FeatureExtractionPipeline pipeline,
                                                                ClusterMembershipTest t,
                                                                int numPhrasesPerFeature,
                                                                double minLeafPruningThreshold, // E.g. 0.2
                                                                int minimumCount, // E.g. 4
                                                                int level1NgramCount, // E.g. 5
                                                                int level2NgramCount, // E.g. 7
                                                                int level3NgramCount, // E.g. 15
                                                                Set<String> stopwords, // E.g. TokenFilterRelevanceStopwords.getStopwords()
                                                                int minPhraseSize,    // E.g. 1
                                                                int maxPhraseSize){  // E.g. 6

        Map<Integer, List<TopNgram<Integer>>> indexedTopPhrases = getTopPhrases(
                clusterIndex, topFeatures, documents, t, numPhrasesPerFeature,
                minLeafPruningThreshold, minimumCount,
                level1NgramCount, level2NgramCount, level3NgramCount, stopwords.stream().map(pipeline::featureIndex).collect(Collectors.toSet()),
                minPhraseSize, maxPhraseSize
        );

        Map<String, List<String>> topPhrasesPerFeature = new LinkedHashMap<>();

        for (Map.Entry<Integer, List<TopNgram<Integer>>> entry : indexedTopPhrases.entrySet()){
            List<String> phrases = entry.getValue().stream()
                    .map(topNgram -> (topNgram.ngram.stream().map(pipeline::featureString).collect(Collectors.joining(" "))))
                    .collect(Collectors.toList());
            topPhrasesPerFeature.put(pipeline.featureString(entry.getKey()), phrases);
        }
        return topPhrasesPerFeature;
    }

    public static Map<Integer, List<TopNgram<Integer>>> getTopPhrases(int clusterIndex,
                                                                  List<Integer> topFeatures,
                                                                  List<ClusteredProcessedInstance> documents,
                                                                  ClusterMembershipTest t,
                                                                  int numPhrasesPerFeature,
                                                                  double minleafPruningThreshold,
                                                                  int minimumCount,
                                                                  int level1NgramCount,
                                                                  int level2NgramCount,
                                                                  int level3NgramCount,
                                                                  Set<Integer> stopwords,
                                                                  int minPhraseSize,
                                                                  int maxPhraseSize){

        List<RootedNgramCounter<Integer>> counters = topFeatures.stream()
                                                        .map(f -> new RootedNgramCounter<>(f, minPhraseSize, maxPhraseSize, minleafPruningThreshold, minimumCount,level1NgramCount, level2NgramCount, level3NgramCount, stopwords))
                                                        .collect(Collectors.toList());
        // For each document that is in the relevant cluster, count occurrences of surrounding words of each word of interest
        for (ClusteredProcessedInstance document : documents) {
            t.setup(document);
            if (t.isDocumentInCluster(document, clusterIndex)){
                for(RootedNgramCounter<Integer> counter : counters){
                    counter.addContext(Ints.asList(document.getDocument().features), 1);
                }
            }
        }

        // For each word of interest, pick the longest most frequent phrases
        Map<Integer, List<TopNgram<Integer>>> topPhrasesPerFeature = new LinkedHashMap<>();
        for (RootedNgramCounter<Integer> counter : counters){
            topPhrasesPerFeature.put(counter.getRootToken(), counter.topNgrams(numPhrasesPerFeature));
        }

        return topPhrasesPerFeature;
    }

    public FeatureClusterJointCounter getCounts() {
        return counts;
    }

    public int getNumOfClusters() {
        return numOfClusters;
    }

    public List<Integer> getFrequentFeatures(int clusterIndex, int K, FeatureType t){
        return new JointLikelihoodOrdering(clusterIndex, t)
                    .greatestOf(counts.getFeaturesInCluster(clusterIndex, t), K);
    }
    public List<String> getFrequentFeatures(int clusterIndex, int K, FeatureType t, FeatureExtractionPipeline pipeline){
        return getFrequentFeatures(clusterIndex, K, t).stream()
                .map(pipeline::featureString)
                .collect(Collectors.toList());
    }

    public List<String> getTopFeatures(int clusterIndex, int K, FeatureExtractionPipeline pipeline, FeatureType t){
        return getTopFeatures(clusterIndex, K, OrderingMethod.LIKELIHOOD_IN_CLUSTER_OVER_PRIOR, pipeline, t);
    }

    public List<String> getTopFeatures(int clusterIndex, int K, OrderingMethod m, FeatureExtractionPipeline pipeline, FeatureType t){
        return getTopFeatures(clusterIndex, K, m, t).stream()
                .map(pipeline::featureString)
                .collect(Collectors.toList());
    }

    public List<Integer> getTopFeatures(int clusterIndex, int K, FeatureType t){
        return getTopFeatures(clusterIndex, K, OrderingMethod.LIKELIHOOD_IN_CLUSTER_OVER_PRIOR, t);
    }

    /**
     * Get a list of the K most interesting features for a given cluster and ordering method.
     */
    public List<Integer> getTopFeatures(int clusterIndex, int K, OrderingMethod m, FeatureType t) {
        Ordering<Integer> ordering;
        switch(m) {
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
                OrderingMethod.LIKELIHOOD_IN_CLUSTER_OVER_PRIOR, FeatureType.WORD,
                0.3, 4, 5, 7, 15, TokenFilterRelevanceStopwords.getStopwords(), 1, 6);
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
     * @param minLeafPruningThreshold Used for deciding how long a phrase should be allowed to be. E.g. if set to 0.3, then
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
                                                   FeatureType featureType,
                                                   double minLeafPruningThreshold,
                                                   int minimumCount,
                                                   int level1NgramCount,
                                                   int level2NgramCount,
                                                   int level3NgramCount,
                                                   Set<String> stopwords,
                                                   int minPhraseSize,
                                                   int maxPhraseSize ){

        Map<Integer, List<TopNgram<Integer>>> indexedTopPhrases = getTopPhrases(
                clusterIndex, numFeatures, numPhrasesPerFeature, m, featureType,
                minLeafPruningThreshold, minimumCount,
                level1NgramCount, level2NgramCount, level3NgramCount, stopwords.stream().map(pipeline::featureIndex).collect(Collectors.toSet()),
                minPhraseSize, maxPhraseSize
        );

        Map<String, List<String>> topPhrasesPerFeature = new LinkedHashMap<>();

        for (Map.Entry<Integer, List<TopNgram<Integer>>> entry : indexedTopPhrases.entrySet()){
            List<String> phrases = new ArrayList<>();
            for (TopNgram<Integer> topNgram : entry.getValue()){
                phrases.add(Joiner.on(" ").join(topNgram.ngram.stream().map(pipeline::featureString).collect(Collectors.toList())));
            }
            topPhrasesPerFeature.put(pipeline.featureString(entry.getKey()), phrases);
        }
        return topPhrasesPerFeature;
    }

    public Map<Integer, List<TopNgram<Integer>>> getTopPhrases(int clusterIndex,
                                                     int numFeatures,
                                                     int numPhrasesPerFeature,
                                                     OrderingMethod m,
                                                     FeatureType featureType,
                                                     double minLeafPruningThreshold,
                                                     int minimumCount,
                                                     int level1NgramCount,
                                                     int level2NgramCount,
                                                     int level3NgramCount,
                                                     Set<Integer> stopwords,
                                                     int minPhraseSize,
                                                     int maxPhraseSize){


        List<Integer> features = getTopFeatures(clusterIndex, numFeatures, m, featureType);

        List<RootedNgramCounter<Integer>> counters = features.stream()
                                                        .map(f -> new RootedNgramCounter<>(f, minPhraseSize, maxPhraseSize, minLeafPruningThreshold, minimumCount, level1NgramCount, level2NgramCount, level3NgramCount, stopwords))
                                                        .collect(Collectors.toList());

        // For each document that is in the relevant cluster, count occurrences of surrounding words of each word of interest
        for (ClusteredProcessedInstance document : documents){
            t.setup(document);
            if (t.isDocumentInCluster(document, clusterIndex)){
                for (RootedNgramCounter<Integer> counter : counters){
                    counter.addContext(Ints.asList(document.getDocument().features), 1);
                }
            }
        }

        // For each word of interest, pick the longest most frequent phrases
        Map<Integer, List<TopNgram<Integer>>> topPhrases = new LinkedHashMap<>();
        for (RootedNgramCounter<Integer> counter : counters){
            topPhrases.put(counter.getRootToken(), counter.topNgrams(numPhrasesPerFeature));
        }

        return topPhrases;
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


    public class JointLikelihoodOrdering extends Ordering<Integer> {

        int clusterIndex;
        FeatureType t;

        public JointLikelihoodOrdering(int clusterIndex, FeatureType t){
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

    public static FeatureBasedCounts saveNewBackgroundCounter(File outputFile, int numOfClusters, Iterable<Instance> backgroundDocuments, FeatureExtractionPipeline pipeline, int minimumBackgroundFeatureCount) throws IOException {
        FeatureBasedCounts counter = new FeatureBasedCounts();

        // Only pass in the background documents. Make a fake clustered document, so that the function can extract a number of clusters, though this will be overwritten later
        counter.count(Lists.newArrayList(new ClusteredProcessedInstance(new ProcessedInstance(0, new int[0], null), new double[numOfClusters])), backgroundDocuments, new HighestProbabilityOnly(), pipeline);
        // Prune low frequency features
        counter.pruneOnlyBackgroundFeaturesWithCountLessThan(minimumBackgroundFeatureCount);
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(outputFile))){
            out.writeObject(counter);
        }
        return counter;
    }

    public static FeatureBasedCounts loadBackgroundCounter(File inputFile) throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(inputFile))){
            return (FeatureBasedCounts)in.readObject();
        }
    }


    public static void main(String[] args) throws IOException, ClassNotFoundException {

        List<String> topFeatures = Lists.newArrayList(
                "visualisation",
                "workbench",
                "legasee",
                "findable",
                "voyant",
                "methodologies",
                "mir",
                "retrieval",
                "oral",
                "algorithms"
        );

        FeatureExtractionPipeline pipeline = new PipelineBuilder().build(new PipelineBuilder.OptionList() // Instantiate the pipeline.
                        .add("tokeniser", ImmutableMap.of(
                                        "type", "basic",
                                        "filter_punctuation", false,
                                        "normalise_urls", true,
                                        "lower_case", true
                                )
                        )
                        .add("filter_regex", "[\\-()\\[\\]]")
                        .add("unigrams", true)
        );

        // added to test chinese tokeniser and pipeline

        FeatureExtractionPipeline chinese_pipeline = new PipelineBuilder().build(new PipelineBuilder.OptionList() // Instantiate the pipeline.
        .add("tokeniser", ImmutableMap.of(
                "type", "chinesestanford",
                "filter_punctuation", true,
                "normalise_urls", true,
                "lower_case", true
                )
        )
        .add("remove_stopwords", ImmutableMap.of(
                "use", "true",
                "lang", "zh"))
        .add("filter_regex", "[\\-（()）【\\[\\]】]")
        .add("unigrams", true)
);

        // added to test chinese pipeline
        String aa = "没有什么可比性\uD83D\uDE3A, 哈哈我知道了, \uE310，每一个认真。\uD869\uDFDD你知道吗 \uD850\uDE26 ，喜来登酒店";
        String bb = "知识就是力量。Knowledge is power. 我";

        String cc = "张士贵，本名忽峍，字武安。虢州卢氏人(今河南省卢氏县)，唐代名将。臂力过人，善骑射。《册府元龟》载他「膂力过人，弯弓一百五十斤，左右骑射，矢不虚发。」\n" +
                "曾祖张俊任北魏银青光禄大夫、横野将军。祖父张和任北齐开府车骑将军。父张国仕隋朝历任陕县主簿，硖州录事参军，历阳县令，后以军功授大都督，定居虢州卢氏县。\n" +
                "隋朝大业末年，张士贵是农民起义军首领之一，自称大总管、怀义公，人称「忽峍贼」。李渊招降张士贵，授右光禄大夫，受相国府司马刘文静节度，大败伪熊州(今河南宜阳县)刺史郑仲达。义宁二年（618年），作为唐王世子左元帅李建成的副手，为第一军总管先锋，向东征讨。后召回京城，拜为通州刺史(今四川达县)。\n" +
                "武德元年(618年)五月，薛举入侵泾州，秦王李世民为西讨元帅，张士贵以先登之功，赏赐奴婢八十人，绢彩千余段，金一百三十挺，授上柱国。后负责运粮草至渑池时，大破王世充属下将领郭士衡的偷袭。\n" +
                "武德二年(619年)，奉命剿灭土匪苏径，进击陆浑，授马军总管，经略熊州(今河南宜阳县)，抵御王世充。赐爵为新野县开国公。\n" +
                "刘武周与突厥联军攻破榆次、介州，进围太原。齐王李元吉弃城而逃，关中震动。唐高祖诏秦王李世民督兵进讨，驻军柏壁。命张士贵攻打虞州(今山西运城东北安邑镇)的何小董。武德三年(620年)4月，在雀鼠谷之战和洺州之战中，唐军大破宋金刚、刘武周。\n" +
                "7月，李世民率军继续征讨洛阳的王世充，张士贵负责督运粮草。以功特派遣殷开山、杜如晦带金银四百余挺赏赐张士贵等诸将。平定洛阳后，累计战功，授虢州刺史。随后继续参与讨伐刘黑闼、徐圆朗，成为李世民秦王府的右库真、骠骑将军。\n" +
                "武德九年六月初四（626年）玄武门之变时张士贵也参与其中，李世民成为太子后，任太子内率。与刘师立募兵万余，拜为右骁卫将军。之后镇守玄武门，不久转为右屯卫将军。\n" +
                "贞观六年（632年）8月，除右武候将军，贞观七年（633年），拜龚州道行军总管，征讨桂州东西王洞獠民叛乱，破反獠而还，唐太宗听闻其冒矢石先登，慰劳张士贵道：“尝闻以忠报国者不顾身，于公见之。”授右屯卫大将军，改封虢国公，检校桂州都督。贞观十五年（641年）随唐太宗去洛阳宫。薛延陀入侵边境，张士贵奉命镇守庆州(甘肃庆阳市)，后任检校夏州都督。\n" +
                "贞观十八年（644年），唐太宗诏令调集粮草，招募军士，准备东征高句丽，龙门人薛仁贵投入张士贵麾下。十九年（645年）三月，张士贵跟随唐太宗征讨高句丽，隶属李世\uD869\uDFDD麾下为行军总管、洺州刺史，十月还师，以功拜冠军大将军、行左屯卫将军，并担任殿后，至并州时再次升为右屯卫大将军，授茂州都督。\n" +
                "唐太宗征讨高句丽时，下令剑南诸獠造船运兵粮，雅、邛、眉三州山獠因不堪其扰，相率叛乱，唐太宗下诏发陇右、峡兵二万，以茂州都督张士贵为雅州道行军总管，与右卫将军梁建方平之。事平，拜金紫光禄大夫、扬州都督府长史。\n" ;
//        String dd = "我们的收费非常不合理，在这里住的四五天令人非常不开心";
//        String ee = "没有什么可比性\uD83D\uDE3A";

        String ff = "四足形類 维基百科，自由的百科全书 跳到导航 跳到搜索 四足形上綱 化石时期：409–0  Ma PreЄ Є O S D C P T J K Pg N 泥盆纪 早期 → 现代 提塔利克鱼 ，生存于泥盆纪晚期的高等 肉鳍鱼 ，属于 希望螈类 ，是 四足动物 的旁系远亲。 科学分类 界： 动物界 Animalia 门： 脊索动物门 Chordata 高纲： 硬骨鱼高纲 Osteichthyes 总纲： 肉鳍鱼总纲 Sarcopterygii 纲： 肺鱼四足纲 Dipnotetrapodomorpha 亚纲： 四足形亚纲 Tetrapodomorpha Ahlberg , 1991 下级分类 † 肯氏鱼属 （英语： Kenichthys ） Kenichthys † 根齿鱼目 （英语： Rhizodontida ） Rhizodontida † 卡南德拉鱼科 （英语： Canowindridae ） Canowindridae † 骨鳞鱼目 （英语： Osteolepiformes ） Osteolepiformes （或 † 巨鱼目 （英语： Megalichthyiformes ） Megalichthyiformes） 始四足类 （英语： Eotetrapodiformes ） Eotetrapodiformes 四足形上綱（ 學名 ：Tetrapodomorpha，或 Choanata [1] ）通称 四足形类，是 肉鳍鱼总纲 的一个 演化支 ，包含 四足類 和一群介於魚類与四足類之間的史前過渡物種，這些史前物種顯示了海生肉鳍鱼发展为陆生四足类的演化历程。 肺魚 是四足形类现存最亲近的 旁系群 ，二者被一同归为 扇鳍类 （肺鱼四足形大纲）。 四足形类的 化石 在 4 亿年前的 泥盆纪 早期便已开始出现，包括 骨鳞鱼 （英语： Osteolepis ）（Osteolepis）、 潘氏鱼 （Panderichthys）、 肯氏鱼 （英语： Kenichthys ）（Kenichthys）和 东生鱼 （Tungsenia）等 [2] 。尽管肉鳍鱼总纲的鱼形动物至今已凋零殆尽， 生态位 被 辐鳍鱼 所取代，但四足形动物自 中生代 以来便成为地球上的优势动物，以极其丰富的种类占据多种生境，是肉鳍鱼现存的主要后代。 四足形类的一大特征是四肢的进化，即胸鳍和腹鳍演变为前足和后足。另一关键特征是鼻孔的移位——原始肉鳍鱼类的前后两对鼻孔长在头部两侧，分别用于进水和排水，而早期四足形动物（如肯氏鱼）的后鼻孔已下移至嘴边，较晚出现的四足形动物（如现代四足类）的后鼻孔则转移至口腔内部 [3] 。 系统发生[ 编辑 ] 根据 2017 年《 硬骨鱼支序分类法 》，四足形类和其他 現生 硬骨鱼 的演化关系如下： [4] [5] [6] 硬骨鱼高纲 Osteichthyes 辐鳍鱼总纲 Actinopterygii 辐鳍鱼纲 Actinopteri 新鳍亚纲 Neopterygii   真骨下纲 Teleostei     全骨下纲 Holostei       软质亚纲 Chondrostei       腕鳍鱼纲 Cladistia     肉鳍鱼总纲 Sarcopterygii 肺鱼四足纲 Dipnotetrapodomorpha   肺鱼亚纲 Dipnomorpha     四足形亚纲 Tetrapodomorpha       腔棘魚綱 Coelacanthimorpha         軟骨魚綱 Chondrichthyes （ 外类群 ）   下级分类[ 编辑 ] 泥盆紀 晚期相继出现的 肉鳍鱼 后代。 潘氏魚 Panderichthys：适合在淤泥浅滩中生活。 提塔利克魚 Tiktaalik：鱼鳍类似四足动物的脚，能使其走上陸地。 魚石螈 Ichthyostega：四足具备。 棘螈 Acanthostega：四足各有八趾。 四足形类是 肉鳍鱼 的主要 演化支 ，由现代 四足类动物 及其史前亲族构成。四足类的史前亲族包含原始的水生肉鳍鱼，和由它们演化而成的形似 蝾螈 的古代 两栖动物 ，以及处在此二者之间的各类过渡物种，这些肉鳍鱼类和现存四足动物的关系比和 肺魚 更为亲近（Amemiya 等人，2013 年）。 在 系统发生学 上，四足形类是四足动物的 总群 ，而现存四足动物（及其 最近共同祖先 的已灭绝后代）是四足形类的 冠群 ，除去这个冠群后则剩下 并系 的四足动物 幹群 ，囊括了从肉鳍鱼演化至四足动物的一系列史前过渡类群，其中 卡南德拉鱼科 （英语： Canowindridae ）、 骨鳞鱼目 （英语： Osteolepiformes ）（或 巨鱼目 （英语： Megalichthyiformes ））及 三列鳍鱼科 （英语： Tristichopteridae ）被统一归为 骨鳞鱼总目 （英语： Osteolepidida ）（Osteolepidida），但由于三列鳍鱼科为 始四足类 （英语： Eotetrapodiformes ）的演化支，而始四足类的其他分支未被骨鳞鱼总目涵盖，因此骨鳞鱼总目是不合理的并系群。 根据 2012 年 伯克利加州大学 学者 Swartz 对 46 个相关类群的 204 个特征进行的 系统发育 分析，四足形类的内部分化关系如下： [7] 四足形类    † 肯氏鱼属 Kenichthys       † 根齿鱼目 Rhizodontida       † 卡南德拉鱼科 Canowindridae   † Marsdenichthys       † 卡南德拉鱼属 Canowindra       † Koharalepis     † Beelarongia             † 骨鳞鱼目 Osteolepiformes   † 格格纳瑟斯鱼属 Gogonasus       † Gyroptychius       † 骨鳞鱼科 Osteolepidae       † Medoevia     † 巨鱼科 Megalichthyidae           始四足类 Eotetrapodiformes † 三列鳍鱼科 Tristichopteridae   † Spodichthys       † 三列鳍鱼属 Tristichopterus       † 真掌鳍鱼属 Eusthenopteron       † Jarvikina       † 石炭鱼属 Cabonnichthys       † Mandageria     † 真掌齿鱼属 Eusthenodon                   † 提尼拉鱼属 Tinirau       † 扁头鱼属 Platycephalichthys   希望螈类 Elpistostegalia   † 潘氏鱼属 Panderichthys   坚头类 Stegocephalia     † 提塔利克鱼属 Tiktaalik     † 希望螈属 Elpistostege         † 散步鱼属 Elginerpeton       † 孔螈属 Ventastega       † 棘螈属 Acanthostega       † 鱼石螈属 Ichthyostega       † 瓦切螈科 Whatcheeriidae       † 圆螈科 Colosteidae       † 厚蛙螈属 Crassigyrinus       † 斜眼螈总科 Baphetoidea     四足類 Tetrapoda（ 冠群 ）                                     参考文献[ 编辑 ] 维基共享资源 中相关的多媒体资源： 四足形類 维基物种 中的分类信息： 四足形類 ^ Zhu Min; Schultze, Hans-Peter.  Per Erik Ahlberg, 编. Major Events in Early Vertebrate Evolution . CRC Press. 11 September 2002: 296 [5 August 2015]. ISBN 978-0-203-46803-6 .   ^ Jing Lu, Min Zhu, John A. Long, Wenjin Zhao, Tim J. Senden, Liantao Jia and Tuo Qiao. The earliest known stem-tetrapod from the Lower Devonian of China. Nature Communications. 2012, 3: 1160. Bibcode:2012NatCo...3.1160L . PMID 23093197 . doi:10.1038/ncomms2170 .   ^ Clack, Jennifer A. Gaining Ground: The Origin and Evolution of Tetrapods . Indiana University Press. 2012: 74 [8 June 2015]. ISBN 978-0-253-35675-8 . （原始内容 存档 于2019-12-16）.   ^ Betancur-R, Ricardo; Wiley, Edward O.; Arratia, Gloria; Acero, Arturo; Bailly, Nicolas; Miya, Masaki; Lecointre, Guillaume; Ortí, Guillermo. Phylogenetic classification of bony fishes . BMC Evolutionary Biology. 2017-07-06, 17: 162 [2019-01-13]. ISSN 1471-2148 . doi:10.1186/s12862-017-0958-3 . （ 原始内容 存档于2019-03-22）.   ^ Betancur-R, R., E. Wiley, N. Bailly, M. Miya, G. Lecointre, and G. Ortí. 2014. Phylogenetic Classification of Bony Fishes --Version 3 ( 存档副本 . [2015-08-09]. （ 原始内容 存档于2015-08-14）.  ). ^ Betancur-R., R., R.E. Broughton, E.O. Wiley, K. Carpenter, J.A. Lopez, C. Li, N.I. Holcroft, D. Arcila, M. Sanciangco, J. Cureton, F. Zhang, T. Buser, M. Campbell, T. Rowley, J.A. Ballesteros, G. Lu, T. Grande, G. Arratia & G. Ortí. 2013. The tree of life and a new classification of bony fishes. PLoS Currents Tree of Life . 2013 Apr 18. ^ Swartz, B. A marine stem-tetrapod from the Devonian of Western North America . PLoS ONE. 2012, 7 (3): e33683. PMC 3308997 . PMID 22448265 . doi:10.1371/journal.pone.0033683 . （原始内容 存档 于2014-12-17）.   Mikko Haaramo. Tetrapodomorpha – Terrestrial vertebrate-like sarcopterygians . [6 April 2006]. （ 原始内容 存档于12 May 2006）.   P. E. Ahlberg & Z. Johanson. Osteolepiforms and the ancestry of tetrapods. Nature . 1998, 395 (6704): 792–794. Bibcode:1998Natur.395..792A . doi:10.1038/27421 .   Michel Laurin, Marc Girondot & Armand de Ricqlès. Early tetrapod evolution (PDF). TREE. 2000, 15 (3) [2020-09-05]. （ 原始内容 (PDF)存档于2009-09-22）.   查 论 编 魚類演化 （英语： Evolution_of_fish ） † 表示滅絕 脊索動物 頭索動物亞門 † 皮卡蟲 † 華夏鰻屬 文昌魚目 嗅球類 † 海口蟲屬 （英语： Haikouella ） 被囊動物亞門 † 昆明魚目 （英语： Myllokunmingiidae ）? † 中新魚屬 （英语： Zhongxiniscus ）? 無頜總綱 圓口綱 盲鰻 七鰓鰻亞綱 † 海口魚 七鰓鰻目 † 牙形石 † 原牙形石目 （英语： Protoconodont ）? † 副牙形石目 （英语： Paraconodontida ） † 鋸齒刺目 （英语： Prioniodontida ） † 應許牙石屬 （英语： Promissum ） † 甲冑魚 † 鰭甲魚綱 † 花鱗魚綱 （英语： Thelodonti ） † 缺甲魚綱 † 頭甲魚類 † 盔甲魚綱 （英语： Galeaspida ） † 茄甲魚綱 （英语： Pituriaspida ） † 骨甲魚綱 有頷下門 † 盾皮魚綱 † 胴甲魚目 † 節甲魚目 † 布林達貝拉魚目 （英语： Brindabellaspida ） † 瓣甲魚目 † 葉鱗魚目 † 褶齒魚目 （英语： Ptyctodontida ） † 硬鮫目 （英语： Rhenanida ） † 棘胸魚目 （英语： Acanthothoraci ） † 假瓣甲魚目 （英语： Pseudopetalichthyida ）? † 史天秀魚目 （英语： Stensioellida ）? † 棘魚綱 † 柵棘魚目 （英语： Climatiiformes ） † 銼棘魚目 （英语： Ischnacanthiformes ） 軟骨魚綱 板鰓亞綱 全頭亞綱 硬骨魚 肉鰭魚總綱 † 爪齒魚目 腔棘魚綱 腔棘魚目 肺魚形類 † 孔鱗魚目 肺魚總目 四足形類 輻鰭魚總綱 腕鰭魚綱 軟骨硬鱗亞綱 新鰭亞綱 † 半椎魚目 全骨下綱 真骨類 魚類列表 史前魚類列表 （英语： Lists of prehistoric fish ） 棘魚綱列表 （英语： List of acanthodians ） 盾皮魚列表 （英语： List of placoderm genera ） 史前軟骨魚列表 （英语： List of prehistoric cartilaginous fish genera ） 史前硬骨魚列表 （英语： List of prehistoric bony fish genera ） 肉鰭魚列表 （英语： List of sarcopterygian genera ） 過渡化石列表 （英语： List of transitional fossils ） 相關條目 Prehistoric life （英语： Prehistoric life ） 過渡化石 Vertebrate paleontology （英语： Vertebrate paleontology ） 查 论 编 現存的 脊索動物 類群 動物界 真後生動物亞界 两侧对称动物 後口動物總門 脊索動物門 頭索動物亞門 頭索綱 被囊動物亞門 海鞘纲 、 樽海鞘纲 、 尾海鞘綱 、 深水海鞘纲 （英语： Sorberacea ） 脊椎動物亞門 無頜總綱 圓口綱 有頜下門 軟骨魚綱 板鰓亞綱 、 全頭亞綱 硬骨魚高綱 輻鰭魚總綱 輻鰭魚綱 、 腕鰭魚綱 肉鰭魚總綱 腔棘魚綱 、 肺魚形類 四足形類 兩棲綱 → 离片椎目 → 滑體亞綱 羊膜類 合弓綱 真盤龍類 → 楔齒龍類 → 獸孔目 → 犬齒獸亞目 → 哺乳綱 蜥形綱 副爬行動物 、 真爬行動物 雙孔亞綱 鱗龍形下綱 喙頭目 、 有鳞目 （ 蜥蜴亚目 → 蛇亚目 ） 主龍形下綱 鱷目 、 龜鱉目 、 恐龍總目 → 鳥綱 物種識別信息 維基數據 : Q1209254 維基物種 : Tetrapodomorpha Fossilworks: 266402 取自“ https://zh.wikipedia.org/w/index.php?title=四足形類&oldid=62162249 ” 分类 ： 四足形類 1991年描述的分類群 隐藏分类： 物种微格式条目 含有拉丁語的條目 导航菜单 个人工具 没有登录 讨论 贡献 创建账户 登录 名字空间 条目 讨论 不转换 不转换 简体 繁體 大陆简体 香港繁體 澳門繁體 大马简体 新加坡简体 臺灣正體 视图 阅读 编辑 查看历史 更多 搜索 导航 首页 分类索引 特色内容 新闻动态 最近更改 随机条目 资助维基百科 帮助 帮助 维基社群 方针与指引 互助客栈 知识问答 字词转换 IRC即时聊天 联络我们 关于维基百科 工具 链入页面 相关更改 上传文件 特殊页面 固定链接 页面信息 引用本页 维基数据项 打印/导出 下载为PDF 打印页面 在其他项目中 维基共享资源 维基物种 其他语言 العربية Català English Español فارسی Suomi Français Bahasa Indonesia Italiano 한국어 Македонски Nederlands Polski Português Русский Simple English Українська Tiếng Việt 编辑链接 本页面最后修订于2020年10月4日 (星期日) 07:35。 本站的全部文字在 知识共享 署名-相同方式共享 3.0协议 之条款下提供，附加条款亦可能应用。（请参阅 使用条款 ） Wikipedia®和维基百科标志是 维基媒体基金会 的注册商标；维基™是维基媒体基金会的商标。 维基媒体基金会是按美国国內稅收法501(c)(3)登记的 非营利慈善机构 。 隐私政策 关于维基百科 免责声明 手机版视图 开发者 统计 Cookie声明 ";
        chinese_pipeline.extractUnindexedFeatures(new Instance("", ff, "")).forEach(System.out::println);

        int gg = 5;
        if (gg==5){
            return ;
        }
//
        List<Integer> topFeaturesIndexed = topFeatures.stream().map(pipeline::featureIndex).collect(Collectors.toList());
//
        String text = FileUtils.readFileToString(new File("/home/a/ad/adr27/Desktop/documentTest.txt"), "utf-8");
//        String text = FileUtils.readFileToString(new File("C:\\Users\\Andy\\Documents\\Work\\documentTest.txt"), "utf-8");
//
        List<String> features = pipeline.extractUnindexedFeatures(new Instance("", text, "")).stream().map(FeatureInferrer.Feature::value).collect(Collectors.toList());
//
        ProcessedInstance doc = pipeline.extractFeatures(new Instance("", text, ""));
        ClusteredProcessedInstance cDoc = new ClusteredProcessedInstance(doc, new double[]{1});

        Instance background = new Instance("", text, "");
        List<Instance> bl = Lists.newArrayList(background);
        FeatureBasedCounts counter1 = saveNewBackgroundCounter(new File("testsave.ser"), 1, bl, pipeline, 3);
        FeatureBasedCounts counter2 = loadBackgroundCounter(new File("testsave.ser"));
        counter2.count(Lists.newArrayList(cDoc), Lists.newArrayList(), new HighestProbabilityOnly(), pipeline, false);
        System.out.println();

        IncrementalFeatureCounter cNew = new IncrementalFeatureCounter(0.1);
        cNew.incrementCounts(bl, pipeline, 10);
        cNew.pruneFeaturesWithCountLessThanN(3);

//        List<Integer> featuresIndexed = IncrementalSurprisingPhraseAnalysis.getTopIndexedFeatures(
//                10, bl, FeatureType.WORD, LIKELIHOOD_IN_TARGET_OVER_BACKGROUND, new IncrementalFeatureCounter(0.1), new IncrementalFeatureCounter(0.1), pipeline, 3, 10);
//


        System.out.println();


        Map<String, List<String>> topPhrases = getTopPhrases(0, topFeaturesIndexed, Lists.newArrayList(cDoc), pipeline,
                new FeatureClusterJointCounter.HighestProbabilityOnly(), 3, 0.3, 4, 5, 7, 15, TokenFilterRelevanceStopwords.getStopwords(), 1, 10);


//        Map<String, List<String>> topPhrases2 = IncrementalSurprisingPhraseAnalysis.getTopPhrases 1(
//                topFeaturesIndexed, Lists.newArrayList(doc.source), 3, 0.3, 4, 5, 7, 15, TokenFilterRelevanceStopwords.getStopwords(), 1, 10, pipeline, new PipelineChanges() {
//                    public void apply(FeatureExtractionPipeline pipeline) { }
//                    public void undo(FeatureExtractionPipeline pipeline) { }
//                } , 10);


        System.out.println();


        Instance background1 = new Instance("","1 2 2 3 3 4 5", "");
        List<Instance> backgrounds = Lists.newArrayList(background1);
        Instance target1 = new Instance("", "0 1 2 2 3 3 3 3 4 4 4 5 5 5 5", "");
        List<Instance> targets = Lists.newArrayList(target1);
        ProcessedInstance doc1 = pipeline.extractFeatures(target1);
        ClusteredProcessedInstance cDoc1 = new ClusteredProcessedInstance(doc1, new double[]{1});
        List<ClusteredProcessedInstance> cDocs1 = Lists.newArrayList(cDoc1);

        List<List<Integer>> featuresOLD = getTopFeatures(10, OrderingMethod.LIKELIHOOD_IN_CLUSTER_OVER_PRIOR, FeatureType.WORD, cDocs1, backgrounds, pipeline, new FeatureBasedCounts(), true, new HighestProbabilityOnly(), 0, 0, null);
        IncrementalSurprisingPhraseAnalysis a = new IncrementalSurprisingPhraseAnalysis(pipeline, new PipelineChanges() {
            @Override
            public void apply(FeatureExtractionPipeline pipeline) {

            }

            @Override
            public void undo(FeatureExtractionPipeline pipeline) {

            }
        }, 0, 0, 10);
        a.incrementBackgroundCounts(backgrounds);
//        List<Integer> featuresNew = a.getTopIndexedFeatures(10, targets, FeatureType.WORD, LIKELIHOOD_IN_TARGET_OVER_BACKGROUND);
        System.out.println();
//
//        RootedNgramCounter<String> counter = new RootedNgramCounter<>("methodologies", 1, 6, 0.2, 4, 5,7,15, TokenFilterRelevanceStopwords.getStopwords());
//////
//        counter.addContext(features);
//////
//        counter.print();
//////
//        List<RootedNgramCounter<String>.Node> nodes = counter.getRoot().getNodeList(true);
//////
//        counter.topNgrams(10).forEach(
//                System.out::println
//        );

//        counter.print();
////
//        counter.print();
//
//        RootedNgramCounter<String> counter =  new RootedNgramCounter<>("methodologies", 1, 6, 0.3, 4, 5, 7, 15, TokenFilterRelevanceStopwords.getStopwords());
////
//        List<String> phrases = Lists.newArrayList(
//                "exploratory work applying specific tools and methodologies to large scale oral history collections",
//                "that allows MIR methodologies to be applied to audio files uploaded for analysis",
//                "It applies algorithm based Music Information Retrieval MIR methodologies created for digital music platforms and music research to these undigested audio oral history archives",
//                "phenomena obscured by traditional methodologies - from emotions to accent to meaningful pauses",
//                "Applied at scale to speech , these methodologies promise the ability to automatically identify",
//                "enabling exploratory research that applies MIR tools and methodologies to large scale oral history collections",
//                "this which allows MIR methodologies to be applied to audio files held locally",
//                "www.voyant-tools.org) allowing MIR methodologies to be applied to audio files uploaded for analysis and visualisation",
//                "Project months 1-4 : Scoping of MIR methodologies , and definition of initial technical specifications"
//        );
////
//        for (String phrase : phrases){
//            List<String> tokenList = Lists.newArrayList(Splitter.on(" ").split(phrase.toLowerCase()));
//            counter.addContext(tokenList, 1);
//        }
////
//        counter.print();
//
//        RootedNgramCounter.Node root = counter.copyTrie();
//
//        counter.topNgrams(10);
//
//        root.print(null);
//
//        counter.print();

//        System.out.println();
//
//        counter.topNgrams(2).stream().map(l -> Joiner.on(" ").join(l)).collect(Collectors.toList()).forEach(
//                System.out::println
//        );
//
//        counter.print();


    }
}
