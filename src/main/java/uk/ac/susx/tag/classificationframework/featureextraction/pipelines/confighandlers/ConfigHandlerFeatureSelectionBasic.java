package uk.ac.susx.tag.classificationframework.featureextraction.pipelines.confighandlers;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import uk.ac.susx.tag.classificationframework.exceptions.ConfigurationException;
import uk.ac.susx.tag.classificationframework.featureextraction.inference.featureselection.FeatureSelectorMI;
import uk.ac.susx.tag.classificationframework.featureextraction.inference.featureselection.FeatureSelectorWFO;
import uk.ac.susx.tag.classificationframework.featureextraction.inference.featureselection.FeatureSelectorWithDocumentFrequencyCutoff;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.PipelineBuilder;

import java.util.*;

/**
 * Created by Andrew D. Robertson on 25/04/2015.
 */
public class ConfigHandlerFeatureSelectionBasic extends ConfigHandler {
    @Override
    public void handle(FeatureExtractionPipeline pipeline, String jsonOptionValue, List<PipelineBuilder.Option> other) {
        Map<String, String> mine = new HashMap<>();
        Map<String, String> options = new Gson().fromJson(jsonOptionValue, new TypeToken<Map<String, Object>>(){}.getType());
        mine.putAll(options);

        boolean selectorPerFeatureType = ConfigHandler.getAndRemove("selector_per_feature_type", mine, false);

        String type = ConfigHandler.getAndRemove("feature_selection_type", mine, "wllr").toLowerCase();  // {wllr, df, mi, custom}
        double lambda = ConfigHandler.getAndRemove("lambda", mine, 0.5);  // ignored unless type == custom
        int n = ConfigHandler.getAndRemove("feature_selection_limit", mine, 1000);  // The number of features to select
        int featureFrequencyCutoff = ConfigHandler.getAndRemove("feature_count_cutoff", mine, 3); // The frequency above which the count of a feature must be in order to even be considered for selection
        Set<String> featureTypes = Sets.newHashSet(ConfigHandler.getAndRemove("feature_types", mine, "bigram,unigram").split("\\s*,\\s*")); // Comma separated list of feature types that this feature selector is interested in (leaving blank defaults to ALL features)
        String customSetup = ConfigHandler.getAndRemove("custom_setup", mine, "");

        if (mine.size() > 0) {
            throw new ConfigurationException("Unrecognised options[s]: " + ConfigHandler.getUnrecognisedOptionsString(mine));
        }

        if (selectorPerFeatureType)
            handleMulti(pipeline, type, lambda, n, featureFrequencyCutoff, featureTypes);
        else
            handleSingle(pipeline, type, lambda, n, featureFrequencyCutoff, featureTypes);
    }

    public void handleSingle(FeatureExtractionPipeline pipeline,
                             String type,
                             double lambda,
                             int n,
                             int featureFrequencyCutoff,
                             Set<String> featureTypes){

        FeatureSelectorWithDocumentFrequencyCutoff featureSelector;
        switch (type) {
            case "wllr": featureSelector = FeatureSelectorWFO.WLLR(n, featureTypes); break;
//            case "mi"  : featureSelector = FeatureSelectorWFO.MI(n, featureTypes); break;
            case "df"  : featureSelector = FeatureSelectorWFO.DF(n, featureTypes); break;
            case "mi"  : featureSelector = new FeatureSelectorMI(n, featureTypes); break;
            default    : featureSelector = new FeatureSelectorWFO(lambda, n, featureTypes); break;
        }
        featureSelector.setDocumentFrequencyCutoff(featureFrequencyCutoff);
        pipeline.add(featureSelector, "selector_basic_"+ type + ":"+ Joiner.on(",").join(featureTypes));

    }

    public void handleMulti(FeatureExtractionPipeline pipeline,
                            String type,
                            double lambda,
                            int n,
                            int featureFrequencyCutoff,
                            Set<String> featureTypes){

        FeatureSelectorWithDocumentFrequencyCutoff featureSelector;
        for (String featureType : featureTypes) {
            Set<String> features = Sets.newHashSet(featureType);
            switch (type) {
                case "wllr": featureSelector = FeatureSelectorWFO.WLLR(n, features); break;
//            case "mi"  : featureSelector = FeatureSelectorWFO.MI(n, features); break;
                case "df"  : featureSelector = FeatureSelectorWFO.DF(n, features); break;
                case "mi"  : featureSelector = new FeatureSelectorMI(n, features); break;
                default    : featureSelector = new FeatureSelectorWFO(lambda, n, features); break;
            }
            featureSelector.setDocumentFrequencyCutoff(featureFrequencyCutoff);
            pipeline.add(featureSelector, "selector_basic_"+ type + ":"+ Joiner.on(",").join(features));
        }
    }

    @Override
    public String getKey() {
        return "feature_selection_basic";
    }
}




