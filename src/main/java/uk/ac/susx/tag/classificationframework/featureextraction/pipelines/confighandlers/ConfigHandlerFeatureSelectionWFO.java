package uk.ac.susx.tag.classificationframework.featureextraction.pipelines.confighandlers;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import uk.ac.susx.tag.classificationframework.exceptions.ConfigurationException;
import uk.ac.susx.tag.classificationframework.featureextraction.inference.featureselection.FeatureSelectorWFO;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.PipelineBuilder;

import java.util.*;

/**
 * Created by Andrew D. Robertson on 25/04/2015.
 */
public class ConfigHandlerFeatureSelectionWFO extends ConfigHandler {
    @Override
    public void handle(FeatureExtractionPipeline pipeline, String jsonOptionValue, List<PipelineBuilder.Option> other) {
        Map<String, String> mine = new HashMap<>();
        Map<String, String> options = new Gson().fromJson(jsonOptionValue, new TypeToken<Map<String, String>>(){}.getType());
        mine.putAll(options);

        String type = ConfigHandler.getAndRemove("type", mine, "wllr").toLowerCase();  // {wllr, df, mi, custom}
        double lambda = ConfigHandler.getAndRemove("lambda", mine, 0.5);
        int n = ConfigHandler.getAndRemove("feature_number", mine, 500);
        Set<String> featureTypes = Sets.newHashSet(ConfigHandler.getAndRemove("feature_types", mine, "bigram,unigram").split("\\s*,\\s*"));

        if (mine.size() > 0) {
            throw new ConfigurationException("Unrecognised options[s]: " + ConfigHandler.getUnrecognisedOptionsString(mine));
        }

        pipeline.add(new FeatureSelectorWFO(lambda, n, featureTypes), getDataOrThrow());
    }

    @Override
    public String getKey() {
        return "feature_selection_wfo";
    }
}
