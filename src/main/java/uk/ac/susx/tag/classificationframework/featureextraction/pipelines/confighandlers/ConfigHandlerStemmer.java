package uk.ac.susx.tag.classificationframework.featureextraction.pipelines.confighandlers;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import uk.ac.susx.tag.classificationframework.featureextraction.normalisation.TokenNormaliserStemmer;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.PipelineBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: Simon Wibberley <sw206@sussex.ac.uk>
 * Date: 07/10/2015
 * Time: 13:54
 */
public class ConfigHandlerStemmer extends ConfigHandler{
    @Override
    public void handle(FeatureExtractionPipeline pipeline, String jsonOptionValue, List<PipelineBuilder.Option> other) {
        Map<String, String> mine = new HashMap<>();
        Map<String, String> options = new Gson().fromJson(jsonOptionValue, new TypeToken<Map<String, Object>>(){}.getType());
        mine.putAll(options);

        String lang = ConfigHandler.getAndRemove("lang", mine, "en");

        pipeline.add(new TokenNormaliserStemmer(lang), "stemmer");
    }

    @Override
    public String getKey() {
        return "stemmer";
    }
}