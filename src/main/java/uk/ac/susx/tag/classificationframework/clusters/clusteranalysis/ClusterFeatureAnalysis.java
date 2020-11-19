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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
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
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;



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
//                Here i changed a static behaviour of getStopwords
                0.3, 4, 5, 7, 15, TokenFilterRelevanceStopwords.getStopwords_dict("en"), 1, 6);
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
        System.out.println("Start processing background ");
        // Only pass in the background documents. Make a fake clustered document, so that the function can extract a number of clusters, though this will be overwritten later
        counter.count(Lists.newArrayList(new ClusteredProcessedInstance(new ProcessedInstance(0, new int[0], null), new double[numOfClusters])), backgroundDocuments, new HighestProbabilityOnly(), pipeline);
        // Prune low frequency features
        counter.pruneOnlyBackgroundFeaturesWithCountLessThan(minimumBackgroundFeatureCount);
        System.out.println("Writing background ");
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

    public static List<Instance> readCsv(String inpath) {
        List<Instance> list = new ArrayList<Instance>();
        try {
            Reader reader = Files.newBufferedReader(Paths.get(inpath) , StandardCharsets.UTF_16);
            CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
//                    change based on the the number of columns in the file, my file had only text column which contains the articles
                    .withHeader("text")
                    .withIgnoreHeaderCase()
                    .withTrim());

            for (CSVRecord csvRecord: csvParser) {
                String text = csvRecord.get("text").replaceAll("[^\u0000-\u200f]", "");
//                This was added by Qiwiei to check if there is some errors in the file i guess for missing values i kept it but change the size to the number of columns your file have in my case only one column
                if (csvRecord.size()!=1) {
                    System.out.println("!!!!!!! corrupted instance !!!!!");
                    System.out.println(text);
                    System.out.println("!!!!!!! corrupted instance !!!!!");
                }
                else {
                    Instance text_i = new Instance("", text, "");
                    list.add(text_i);

                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return list;

    }

//    To save the articles
    public static List<Instance> savecorpus(File outputFile, String inpath) throws IOException{
        List<Instance> bg_list = readCsv(inpath);

        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(outputFile))){
            out.writeObject(bg_list);
        }

        return bg_list;
    }
// To save the pipeline
    public static void savepipeline(File outputFile, FeatureExtractionPipeline pipeline) throws IOException{
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(outputFile))){
            out.writeObject(pipeline);
        }
    }
//    load the saved file for testing
    public static Iterable<Instance> load(File inputFile) throws IOException, ClassNotFoundException{

        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(inputFile))){
            return (Iterable<Instance>)in.readObject();
        }

    }
//      to count the size of the ser file that saved the articles
    public static int count_size(Iterable<Instance> corpus) {
        if (corpus instanceof Collection)
            return ((Collection<?>)corpus).size();
        int i = 0;
        for (Object obj : corpus) i++;
        return i;

    }

//    To print the articles saved in the ser
    public static void print_instance(Iterable<Instance> corpus) {
        for (Instance i: corpus){
            System.out.println(i.text);
            System.out.println("~~~~~~~~~~~~~~~~");
        }
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {

//        // Deserialised it to see if it contains the lang key
//        ObjectInputStream ios = new ObjectInputStream(new FileInputStream("/Users/ay227/Desktop/CASM/Arabic_background/ar-wiki-pipeline.ser"));
//        FeatureExtractionPipeline temp;
//        try {
//            while ((temp = (FeatureExtractionPipeline) ios.readObject()) != null) {
//                System.out.println(temp);
//            }
//        } catch (EOFException e) {
//        } finally {
//            ios.close();
//        }

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

//        build the pipeline
        FeatureExtractionPipeline arabic_pipeline = new PipelineBuilder().build(new PipelineBuilder.OptionList() // Instantiate the pipeline.
                .add("tokeniser", ImmutableMap.of(
                        "type", "arabicstanford",
                        "filter_punctuation", true,
                        "normalise_urls", true
                        )
                )
                .add("remove_stopwords", ImmutableMap.of(
                        "use", "true",
                        "lang", "ar"))
                .add("filter_regex", "[\\-（()）【\\[\\]】]")
                .add("unigrams", true)
        );

//      Read the csv file that contains the article and save it in articles.ser
        List<Instance> bl = savecorpus(new File("/Users/ay227/Desktop/CASM/Arabic_background/ar-wiki-articles.ser"), "/Users/ay227/Desktop/CASM/source_background/ar_background_10.csv");
//        to test the correctness of serializing articles load the generated file
        Iterable<Instance> bl_test = load(new File("/Users/ay227/Desktop/CASM/Arabic_background/ar-wiki-articles.ser"));
//        load one of the files previously generated by other language
        Iterable<Instance> bl_en_test = load(new File("/Users/ay227/Desktop/CASM/git/wikisample-withzh/sample/zh-wiki-articles.ser"));

        System.out.println("Done loading articles");
//        to print the count of the serialized articles for example for the output should be
//        done loading articles
//        36205 the size of the generated Arabic file (approximately)
//        15000 the size of english
        System.out.println(count_size(bl_test));
        System.out.println(count_size(bl_en_test));
//        to print the serialized articles
        print_instance(bl_test);

        savepipeline(new File("/Users/ay227/Desktop/CASM/Arabic_background/ar-wiki-pipeline.ser"), arabic_pipeline);
        System.out.println("Done saving pipeline.ser");



        FeatureBasedCounts counter1 = saveNewBackgroundCounter(new File("/Users/ay227/Desktop/CASM/Arabic_background/ar-wiki-count.ser"), 1, bl, arabic_pipeline, 3);
        System.out.println("Done testing FeatureBasedCounts");

        IncrementalFeatureCounter cNew = new IncrementalFeatureCounter(0.1);
        cNew.incrementCounts(bl, arabic_pipeline, 10);
        cNew.pruneFeaturesWithCountLessThanN(3);
        System.out.println("Done testing IncrementalFeatureCounter");

        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("/Users/ay227/Desktop/CASM/Arabic_background/ar-wiki-inc-feat-counts.ser"))){
            out.writeObject(cNew);
        }
        System.out.println("Done saving ar-wiki-inc-feat-counts.ser");







    }
}
