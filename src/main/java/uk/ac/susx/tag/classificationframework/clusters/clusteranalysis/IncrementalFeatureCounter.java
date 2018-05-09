package uk.ac.susx.tag.classificationframework.clusters.clusteranalysis;

import com.google.common.collect.Ordering;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import uk.ac.susx.tag.classificationframework.datastructures.Instance;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static uk.ac.susx.tag.classificationframework.featureextraction.inference.FeatureInferrer.Feature;

/**
 * Keep track of the frequency of features. Track FeatureTypes of features separately. Provide simple smoothed
 * probability estimates of the features according to the counts.
 *
 * Repeated calls to increment methods is allowed. Pruning counts is possible and irreversible.
 *
 * Created by Andrew D. Robertson on 20/10/16.
 */
public class IncrementalFeatureCounter implements Serializable {

    private static final long serialVersionUID = 0L;

    protected double featureSmoothing;

    protected Object2IntOpenHashMap<FeatureType> totalFeatureCount;

    protected Map<FeatureType, Int2IntOpenHashMap> featureCounts;

    public IncrementalFeatureCounter(double featureSmoothing) {

        this.featureSmoothing = featureSmoothing;

        totalFeatureCount = new Object2IntOpenHashMap<>();
        featureCounts = new HashMap<>();

        for (FeatureType type : FeatureType.values()){

            totalFeatureCount.put(type, 0);
            featureCounts.put(type, new Int2IntOpenHashMap());
        }
    }

    public static IncrementalFeatureCounter build(Iterator<Instance> documents, FeatureExtractionPipeline pipeline, double featureSmoothing, int batchSize){
        IncrementalFeatureCounter counter = new IncrementalFeatureCounter(featureSmoothing);
        counter.incrementCounts(documents, pipeline, batchSize);
        return counter;
    }

    public void incrementCounts(List<Instance> documents, FeatureExtractionPipeline pipeline, int batchSize){
        pipeline.extractUnindexedFeaturesInBatchesToStream(documents, batchSize).forEach(
            featureList -> {
                for (Feature feature : featureList){
                    String featureValue = feature.value();
                    // Supporting hashtags, account tags, and words
                    if (featureValue.startsWith("#")){
                        totalFeatureCount.addTo(FeatureType.HASH_TAG, 1);
                        featureCounts.get(FeatureType.HASH_TAG).addTo(pipeline.featureIndex(featureValue), 1);
                    } else if (featureValue.startsWith("@")){
                        totalFeatureCount.addTo(FeatureType.ACCOUNT_TAG, 1);
                        featureCounts.get(FeatureType.ACCOUNT_TAG).addTo(pipeline.featureIndex(featureValue), 1);
                    } else {
                        totalFeatureCount.addTo(FeatureType.WORD, 1);
                        featureCounts.get(FeatureType.WORD).addTo(pipeline.featureIndex(featureValue), 1);
                    }
                }
            }
        );
    }

    public void incrementCounts(Iterator<Instance> documents, FeatureExtractionPipeline pipeline, int batchSize){
        pipeline.extractUnindexedFeaturesInBatchesToIterator(documents, batchSize).forEachRemaining(
            featureList -> {
                for (Feature feature : featureList){
                    String featureValue = feature.value();
                    // Supporting hashtags, account tags, and words
                    if (featureValue.startsWith("#")){
                        totalFeatureCount.addTo(FeatureType.HASH_TAG, 1);
                        featureCounts.get(FeatureType.HASH_TAG).addTo(pipeline.featureIndex(featureValue), 1);
                    } else if (featureValue.startsWith("@")){
                        totalFeatureCount.addTo(FeatureType.ACCOUNT_TAG, 1);
                        featureCounts.get(FeatureType.ACCOUNT_TAG).addTo(pipeline.featureIndex(featureValue), 1);
                    } else {
                        totalFeatureCount.addTo(FeatureType.WORD, 1);
                        featureCounts.get(FeatureType.WORD).addTo(pipeline.featureIndex(featureValue), 1);
                    }
                }
            }
        );
    }

    public int getCount(FeatureType type, int feature) {
        return featureCounts.get(type).get(feature);
    }

    public void add(IncrementalFeatureCounter other){
        for (FeatureType type : FeatureType.values()){
            totalFeatureCount.addTo(type, other.totalFeatureCount.get(type));

            Int2IntOpenHashMap counts = featureCounts.get(type);

            for(Int2IntMap.Entry entry : featureCounts.get(type).int2IntEntrySet()){
                int feature = entry.getIntKey();
                int count = entry.getIntValue();
                counts.addTo(feature, count);
            }
        }
    }

    public IntSet getWords(){
        return featureCounts.get(FeatureType.WORD).keySet();
    }

    public IntSet getFeatures(FeatureType type){
        return featureCounts.get(type).keySet();
    }

    public double wordProbability(int wordIndex){
        return featureProbability(wordIndex, FeatureType.WORD);
    }

    public double featureProbability(int featureIndex, FeatureType type){
        int totalCount = totalFeatureCount.get(type);
        int featureCount = featureCounts.get(type).get(featureIndex);
        int totalVocab = featureCounts.get(type).size();

        return (featureCount + featureSmoothing) / ((double)totalCount + (featureSmoothing * totalVocab));
    }

    public List<Integer> mostFrequentFeatures(FeatureType type, int maxFeatures, int minimumFrequency){

        List<Integer> topFeatures = new FeatureFrequencyOrdering(featureCounts.get(type)).greatestOf(featureCounts.get(type).keySet(), maxFeatures);

        return topFeatures.stream()
                .filter(f -> featureCounts.get(type).get(f) >= minimumFrequency)
                .collect(Collectors.toList());
    }

    public void pruneFeaturesWithCountLessThanN(int n, FeatureType type){
        if (n > 1) {
            Iterator<Int2IntMap.Entry> iter = featureCounts.get(type).int2IntEntrySet().fastIterator();

            while (iter.hasNext()) {
                Int2IntMap.Entry e = iter.next();
                int feature = e.getIntKey();
                int count = e.getIntValue();
                if (count < n) {
                    iter.remove();
                    totalFeatureCount.put(type, Math.max(0, totalFeatureCount.get(type) - count));
                }
            }
        }
    }

    public void pruneFeaturesWithCountLessThanN(int n){
        for (FeatureType t : FeatureType.values())
            pruneFeaturesWithCountLessThanN(n, t);
    }

    public static class FeatureFrequencyOrdering extends Ordering<Integer>{

        private Int2IntOpenHashMap counts;

        public FeatureFrequencyOrdering(Int2IntOpenHashMap featureCounts){
            counts = featureCounts;
        }

        @Override
        public int compare(Integer left, Integer right) {
            return counts.get(right) - counts.get(left);
        }
    }
}
