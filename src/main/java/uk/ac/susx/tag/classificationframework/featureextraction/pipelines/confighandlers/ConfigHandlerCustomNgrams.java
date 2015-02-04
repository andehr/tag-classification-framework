package uk.ac.susx.tag.classificationframework.featureextraction.pipelines.confighandlers;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import uk.ac.susx.tag.classificationframework.exceptions.ConfigurationException;
import uk.ac.susx.tag.classificationframework.featureextraction.inference.FeatureInferrerCustomNgrams;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.PipelineBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Andrew D. Robertson
 * Date: 04/02/2015
 * Time: 15:46
 */
public class ConfigHandlerCustomNgrams extends ConfigHandler{
    @Override
    public void handle(FeatureExtractionPipeline pipeline, String jsonOptionValue, List<PipelineBuilder.Option> other) {
        Map<String, Object> mine = new HashMap<>();
        Map<String, Object> options = new Gson().fromJson(jsonOptionValue, new TypeToken<Map<String, Object>>(){}.getType());
        mine.putAll(options);

        List<String> customNgrams = ConfigHandler.getAndRemove("ngrams", mine, new ArrayList<String>());
        boolean includeFilteredTokens = ConfigHandler.getAndRemove("include_filtered_tokens", mine, false);

        if (mine.size() > 0) {
            throw new ConfigurationException("Unrecognised options[s]: " + ConfigHandler.getUnrecognisedOptionsString(mine));
        }

        if (customNgrams.size() > 0) {
            pipeline.add(new FeatureInferrerCustomNgrams(customNgrams, includeFilteredTokens));
        }
    }

    @Override
    public String getKey() {
        return "custom_ngrams";
    }
}
