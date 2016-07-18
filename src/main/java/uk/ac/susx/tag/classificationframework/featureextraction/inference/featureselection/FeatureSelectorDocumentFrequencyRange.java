package uk.ac.susx.tag.classificationframework.featureextraction.inference.featureselection;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;

import java.util.Iterator;

/**
 * Created with IntelliJ IDEA.
 * User: Andrew D. Robertson
 * Date: 14/10/2015
 * Time: 13:06
 */
public class FeatureSelectorDocumentFrequencyRange extends FeatureSelector {

    private static final long serialVersionUID = 0L;

    private int lower;
    private int upper;

    public FeatureSelectorDocumentFrequencyRange(){
        this(0, -1);
    }

    public FeatureSelectorDocumentFrequencyRange(int lower){
        this(lower, -1);
    }

    public FeatureSelectorDocumentFrequencyRange(int lower, int upper){
        this.lower = lower;
        this.upper = upper;
    }

    @Override
    public void update(FeatureExtractionPipeline.Data data) {
        Object2IntOpenHashMap<String> featureCounts = selectedFeatureTypes.isEmpty()?
                featureCountsAnyFeatureType(data) : featureCountsSpecificFeatureType(data);

        ObjectIterator<Object2IntMap.Entry<String>> entries = featureCounts.object2IntEntrySet().fastIterator();
        while (entries.hasNext()){
            Object2IntMap.Entry<String> entry = entries.next();
            if (entry.getIntValue() >= lower && (upper<=lower || entry.getIntValue() < upper))
                topFeatures.add(entry.getKey());
        }
    }

    private Object2IntOpenHashMap<String> featureCountsAnyFeatureType(FeatureExtractionPipeline.Data data){
        Object2IntOpenHashMap<String> featureCounts = new Object2IntOpenHashMap<>();

        for (FeatureExtractionPipeline.Datum d : data.allData()){
            for (Feature f : d.features){
                featureCounts.addTo(f.value(), 1);
            }
        }

        return featureCounts;
    }

    private Object2IntOpenHashMap<String> featureCountsSpecificFeatureType(FeatureExtractionPipeline.Data data){
        Object2IntOpenHashMap<String> featureCounts = new Object2IntOpenHashMap<>();

        for (FeatureExtractionPipeline.Datum d : data.allData()){
            for (Feature f : d.features){
                if (selectedFeatureTypes.contains(f.type()))
                    featureCounts.addTo(f.value(), 1);
            }
        }

        return featureCounts;
    }

    @Override
    public boolean isThreadSafe() {
        return true;
    }
}
