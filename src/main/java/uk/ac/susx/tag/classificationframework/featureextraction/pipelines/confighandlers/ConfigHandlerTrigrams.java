package uk.ac.susx.tag.classificationframework.featureextraction.pipelines.confighandlers;

import com.google.gson.Gson;
import uk.ac.susx.tag.classificationframework.featureextraction.inference.FeatureInferrerTrigrams;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.PipelineBuilder;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Andrew D. Robertson
 * Date: 24/06/2014
 * Time: 13:24
 */
public class ConfigHandlerTrigrams extends ConfigHandler {

    @Override
    public void handle(FeatureExtractionPipeline pipeline, String jsonOptionValue, List<PipelineBuilder.Option> other) {
        if (new Gson().fromJson(jsonOptionValue, Boolean.class))  // This is pretty tolerant of all the possible ways true and false could appear
            pipeline.add(new FeatureInferrerTrigrams(false, null), getKey());
    }

    @Override
    public String getKey() {
        return "trigrams";
    }
}
