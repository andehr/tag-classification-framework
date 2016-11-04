package uk.ac.susx.tag.classificationframework.clusters.clusteranalysis;

import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.susx.tag.classificationframework.datastructures.Instance;
import uk.ac.susx.tag.classificationframework.datastructures.ProcessedInstance;
import uk.ac.susx.tag.classificationframework.datastructures.RootedNgramCounter;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline.PipelineChanges;

/**
 *
 *
 * Created by Andrew D. Robertson on 20/10/16.
 */
public class IncrementalSurprisingPhraseAnalysis {

    private static final Logger LOG = LoggerFactory.getLogger(IncrementalSurprisingPhraseAnalysis.class);

    private static final double featureSmoothing = 0.1;

    private PipelineChanges prePhraseExtractionChanges;
    private int minimumBackgroundFeatureCount;
    private int minimumTargetFeatureCount;

    private int batchSize;

    private IncrementalFeatureCounter backgroundCounter;
    private FeatureExtractionPipeline pipeline;

    private Map<FeatureType, Boolean> backgroundPruned;

    public enum OrderingMethod {
        LIKELIHOOD_IN_TARGET_OVER_BACKGROUND,  // Essentially PMI: P(feature|target) / P(feature|background)
    }

    public IncrementalSurprisingPhraseAnalysis(FeatureExtractionPipeline pipeline,
                                               PipelineChanges prePhraseExtractionChanges,
                                               int minimumBackgroundFeatureCount,
                                               int minimumTargetFeatureCount,
                                               int batchSize) {

        this.pipeline = pipeline;
        this.prePhraseExtractionChanges = prePhraseExtractionChanges;
        this.minimumBackgroundFeatureCount = minimumBackgroundFeatureCount;
        this.minimumTargetFeatureCount = minimumTargetFeatureCount;
        this.batchSize = batchSize;

        backgroundCounter = new IncrementalFeatureCounter(featureSmoothing);
        backgroundPruned = new HashMap<>();
        for (FeatureType t : FeatureType.values())
            backgroundPruned.put(t, false);
    }


/************************************
 * Background count incrememnting
 ************************************/

    /**
     * Count features in documents, add counts to background knowledge.
     * See alternative incrementBackgroundCounts methods for large iterators of data.
     */
    public void incrementBackgroundCounts(List<Instance> backgroundDocuments){
        backgroundCounter.incrementCounts(backgroundDocuments, pipeline, batchSize);
    }

    /**
     * If you can't afford to hold all of your background data in memory, use this function.
     */
    public void incrementBackgroundCounts(Iterator<Instance> backgroundDocuments){
        backgroundCounter.incrementCounts(backgroundDocuments, pipeline, batchSize);
    }
/*************************************/



/************************************
 * Background count pruning.
 ************************************/

    public boolean areBackgroundCountsPruned(FeatureType t){ return backgroundPruned.get(t); }
    public void pruneBackgroundCounts(FeatureType t){
        if (!backgroundPruned.get(t) && minimumBackgroundFeatureCount > 1){
            backgroundCounter.pruneFeaturesWithCountLessThanN(minimumBackgroundFeatureCount, t);
        }
        backgroundPruned.put(t, true);
    }
    public void pruneBackgroundCounts(){
        for (FeatureType t : FeatureType.values())
            pruneBackgroundCounts(t);
    }
/*************************************/



/************************************
 * Get features and phrases
 ************************************/
    public static List<Integer> getTopIndexedFeatures(
            int numOfFeatures,
            List<Instance> targetDocuments,
            FeatureType featureType,
            OrderingMethod method,
            IncrementalFeatureCounter targetCounter,
            IncrementalFeatureCounter backgroundCounter,
            FeatureExtractionPipeline pipeline,
            int minimumTargetFeatureCount,
            int batchSize) {

        targetCounter.incrementCounts(targetDocuments, pipeline, batchSize);

        if (minimumTargetFeatureCount > 1){
            LOG.info("Pruning target document features...");
            targetCounter.pruneFeaturesWithCountLessThanN(minimumTargetFeatureCount, featureType);
        }

        Ordering<Integer> ordering;
        switch (method){
            case LIKELIHOOD_IN_TARGET_OVER_BACKGROUND:
                ordering = new TargetBackgroundRatioOrdering(featureType, backgroundCounter, targetCounter); break;
            default: throw new RuntimeException("OrderingMethod not recognised.");
        }

        LOG.info("Finding top N features...");
        return ordering.greatestOf(targetCounter.getFeatures(featureType), numOfFeatures);
    }

    public List<Integer> getTopIndexedFeatures(
            int numOfFeatures,
            List<Instance> targetDocuments,
            FeatureType featureType,
            OrderingMethod method,
            IncrementalFeatureCounter targetCounter){

        return getTopIndexedFeatures(
                numOfFeatures, targetDocuments, featureType, method, targetCounter,
                backgroundCounter, pipeline, minimumTargetFeatureCount, batchSize);
    }

    public List<Integer> getTopIndexedFeatures(
            int numOfFeatures,
            List<Instance> targetDocuments,
            FeatureType featureType,
            OrderingMethod method){

        return getTopIndexedFeatures(
                numOfFeatures, targetDocuments, featureType, method, new IncrementalFeatureCounter(featureSmoothing),
                backgroundCounter, pipeline, minimumTargetFeatureCount, batchSize);
    }

    public static Map<Integer, List<List<Integer>>> getTopIndexedPhrases(List<Integer> topFeatures,
                                                                         List<Instance> documents,
                                                                         int numPhrasesPerFeature,
                                                                         double minLeafPruningThreshold,
                                                                         int minimumCount,
                                                                         int level1NgramCount,
                                                                         int level2NgramCount,
                                                                         int level3NgramCount,
                                                                         Set<Integer> stopwords,
                                                                         int minPhraseSize,
                                                                         int maxPhraseSize,
                                                                         FeatureExtractionPipeline pipeline,
                                                                         PipelineChanges prePhraseExtractionChanges,
                                                                         int batchSize){

        // Prepare the rooted ngram counter objects, one for each top feature
        List<RootedNgramCounter<Integer>> counters = topFeatures.stream()
             .map(f -> new RootedNgramCounter<>(f, minPhraseSize, maxPhraseSize, minLeafPruningThreshold, minimumCount,
                     level1NgramCount, level2NgramCount, level3NgramCount, stopwords))
             .collect(Collectors.toList());

        LOG.info("Processing target documents for feature context counting.");
        // Process the documents with any pipeline changes necessary, adding counts to the rooted ngram counters
        for (ProcessedInstance i : pipeline.surroundProcessingWithChanges(prePhraseExtractionChanges, p -> p.extractFeaturesInBatches(documents, batchSize))) {
            for (RootedNgramCounter<Integer> rootedNgramCounter : counters) {
                rootedNgramCounter.addContext(Ints.asList(i.features), 1);
            }
        }

        LOG.info("Taking top N phrases.");
        // For each word of interest, pick the longest most frequent phrases, using the counts found
        Map<Integer, List<List<Integer>>> topPhrasesPerFeature = new LinkedHashMap<>();
        for (RootedNgramCounter<Integer> counter : counters){
            topPhrasesPerFeature.put(counter.getRootToken(), counter.topNgrams(numPhrasesPerFeature));
        }
        return topPhrasesPerFeature;
    }

    /**
     * @param topFeatures Top features as obtained from getTopFeatures()
     * @param documents The same documents used for getTopFeatures(), ensure that indices match up if you did any reprocessing
     * @param pipeline The pipeline used during getTopFeatures()
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
    public static Map<String, List<String>> getTopPhrases( List<Integer> topFeatures,
                                                    List<Instance> documents,
                                                    int numPhrasesPerFeature,
                                                    double minLeafPruningThreshold, // E.g. 0.2
                                                    int minimumCount, // E.g. 4
                                                    int level1NgramCount, // E.g. 5
                                                    int level2NgramCount, // E.g. 7
                                                    int level3NgramCount, // E.g. 15
                                                    Set<String> stopwords, // E.g. TokenFilterRelevanceStopwords.getStopwords()
                                                    int minPhraseSize,    // E.g. 1
                                                    int maxPhraseSize,   // E.g. 6
                                                    FeatureExtractionPipeline pipeline,
                                                    PipelineChanges prePhraseExtractionChanges,
                                                    int batchSize){
        Map<Integer, List<List<Integer>>> indexedTopPhrases = getTopIndexedPhrases(
                topFeatures, documents, numPhrasesPerFeature, minLeafPruningThreshold, minimumCount, level1NgramCount, level2NgramCount, level3NgramCount,
                stopwords.stream().map(pipeline::featureIndex).collect(Collectors.toSet()), minPhraseSize, maxPhraseSize,
                pipeline, prePhraseExtractionChanges, batchSize
        );

        Map<String, List<String>> topPhrasesPerFeature = new LinkedHashMap<>();

        LOG.info("Unindexing the top phrases.");
        for (Map.Entry<Integer, List<List<Integer>>> entry : indexedTopPhrases.entrySet()){
            List<String> phrases = entry.getValue().stream()
                                    .map(ngram -> (ngram.stream().map(pipeline::featureString).collect(Collectors.joining(" "))))
                                    .collect(Collectors.toList());
            topPhrasesPerFeature.put(pipeline.featureString(entry.getKey()), phrases);
        }
        LOG.info("Returning top phrases.");
        return topPhrasesPerFeature;
    }

    public Map<String, List<String>> getTopPhrases( List<Integer> topFeatures,
                                                    List<Instance> documents,
                                                    int numPhrasesPerFeature,
                                                    double minLeafPruningThreshold, // E.g. 0.2
                                                    int minimumCount, // E.g. 4
                                                    int level1NgramCount, // E.g. 5
                                                    int level2NgramCount, // E.g. 7
                                                    int level3NgramCount, // E.g. 15
                                                    Set<String> stopwords, // E.g. TokenFilterRelevanceStopwords.getStopwords()
                                                    int minPhraseSize,    // E.g. 1
                                                    int maxPhraseSize   // E.g. 6
                                                    ){
        return getTopPhrases(topFeatures, documents, numPhrasesPerFeature, minLeafPruningThreshold,
                             minimumCount, level1NgramCount, level2NgramCount, level3NgramCount,
                             stopwords, minPhraseSize, maxPhraseSize, pipeline, prePhraseExtractionChanges, batchSize);
    }

    public static class TargetBackgroundRatioOrdering extends Ordering<Integer> {

        private final FeatureType t;
        private final IncrementalFeatureCounter backgroundCounter;
        private final IncrementalFeatureCounter targetCounter;

        public TargetBackgroundRatioOrdering(FeatureType t, IncrementalFeatureCounter backgroundCounter, IncrementalFeatureCounter targetCounter) {
            this.t = t;
            this.backgroundCounter = backgroundCounter;
            this.targetCounter = targetCounter;
        }

        @Override
        public int compare(Integer feature1, Integer feature2) {
            double leftRatio = Math.log(targetCounter.featureProbability(feature1, t)) - Math.log(backgroundCounter.featureProbability(feature1, t));
            double rightRatio = Math.log(targetCounter.featureProbability(feature2, t)) - Math.log(backgroundCounter.featureProbability(feature2, t));
            return Double.compare(leftRatio, rightRatio);
        }
    }
}
