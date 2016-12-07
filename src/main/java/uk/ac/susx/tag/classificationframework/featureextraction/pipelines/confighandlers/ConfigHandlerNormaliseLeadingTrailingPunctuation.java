package uk.ac.susx.tag.classificationframework.featureextraction.pipelines.confighandlers;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import uk.ac.susx.tag.classificationframework.featureextraction.documentprocessing.Service;
import uk.ac.susx.tag.classificationframework.featureextraction.normalisation.TokenNormaliserLeadingTrailingPunctuation;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.PipelineBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Andrew D. Robertson on 06/12/16.
 */
public class ConfigHandlerNormaliseLeadingTrailingPunctuation extends ConfigHandler{
    @Override
    public void handle(FeatureExtractionPipeline pipeline, String jsonOptionValue, List<PipelineBuilder.Option> other) {
        Map<String, String> mine = new HashMap<>();
        Map<String, String> options = new Gson().fromJson(jsonOptionValue, new TypeToken<Map<String, String>>(){}.getType());
        mine.putAll(options);

        boolean excludeTwitterTags = ConfigHandler.getAndRemove("exclude_twitter_tags", mine, false);

        pipeline.add(new TokenNormaliserLeadingTrailingPunctuation(excludeTwitterTags), "normalise_leading_trailing_punctuation");
    }

    @Override
    public String getKey() {
        return "normalise_leading_trailing_punctuation";
    }
}
