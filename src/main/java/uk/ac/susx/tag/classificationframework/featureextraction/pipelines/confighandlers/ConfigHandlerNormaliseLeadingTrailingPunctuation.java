package uk.ac.susx.tag.classificationframework.featureextraction.pipelines.confighandlers;

import com.google.gson.Gson;
import uk.ac.susx.tag.classificationframework.featureextraction.normalisation.TokenNormaliserLeadingTrailingPunctuation;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.PipelineBuilder;

import java.util.List;

/**
 * Created by Andrew D. Robertson on 06/12/16.
 */
public class ConfigHandlerNormaliseLeadingTrailingPunctuation extends ConfigHandler{
    @Override
    public void handle(FeatureExtractionPipeline pipeline, String jsonOptionValue, List<PipelineBuilder.Option> other) {
        if(new Gson().fromJson(jsonOptionValue, Boolean.class)){
            pipeline.add(new TokenNormaliserLeadingTrailingPunctuation(), "normalise_leading_trailing_punctuation");
        }
    }

    @Override
    public String getKey() {
        return "normalise_leading_trailing_punctuation";
    }
}
