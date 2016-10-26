package uk.ac.susx.tag.classificationframework.clusters.clusteranalysis;

import com.google.common.collect.Ordering;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import uk.ac.susx.tag.classificationframework.datastructures.Instance;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uk.ac.susx.tag.classificationframework.featureextraction.inference.FeatureInferrer.Feature;

/**
 * Created by Andrew D. Robertson on 20/10/16.
 */
public class IncrementalFeatureCounter {

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

    public void incrementCounts(List<Instance> documents, FeatureExtractionPipeline pipeline){
        for (List<Feature> featureList : pipeline.extractUnindexedFeaturesFromBatch(documents)){

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

    public double wordPrior(int wordIndex){
        return featurePrior(wordIndex, FeatureType.WORD);
    }

    public double featurePrior(int feature, FeatureType type){
        int totalCount = totalFeatureCount.get(type);
        int featureCount = featureCounts.get(type).get(feature);
        int totalVocab = featureCounts.get(type).size();

        return (featureCount + featureSmoothing) / ((double)totalCount + (featureSmoothing * totalVocab));
    }

    public List<Integer> mostFrequentFeatures(FeatureType type, int maxFeatures, int minimumFrequency){

        List<Integer> topFeatures = new FeatureFrequencyOrdering(featureCounts.get(type)).greatestOf(featureCounts.get(type).keySet(), maxFeatures);

        return topFeatures.stream()
                .filter(f -> featureCounts.get(type).get(f) >= minimumFrequency)
                .collect(Collectors.toList());
    }

    public void pruneFeaturesWithCountLessThan(int n, FeatureType type){
        Iterator<Int2IntMap.Entry> iter = featureCounts.get(type).int2IntEntrySet().fastIterator();

        while(iter.hasNext()){
            Int2IntMap.Entry e = iter.next();
            int feature = e.getIntKey();
            int count = e.getIntValue();
            if (count < n){
                iter.remove();
                totalFeatureCount.put(type, Math.max(0, totalFeatureCount.get(type)-count));
            }
        }
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
