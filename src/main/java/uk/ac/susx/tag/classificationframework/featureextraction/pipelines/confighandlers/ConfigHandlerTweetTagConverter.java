package uk.ac.susx.tag.classificationframework.featureextraction.pipelines.confighandlers;

import com.google.gson.Gson;
import uk.ac.susx.tag.classificationframework.exceptions.ConfigurationException;
import uk.ac.susx.tag.classificationframework.featureextraction.documentprocessing.ArcEagerDependencyParser;
import uk.ac.susx.tag.classificationframework.featureextraction.documentprocessing.TweetTagConverter;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.PipelineBuilder;

import java.io.IOException;
import java.util.List;

/**
 * Created by adr27 on 01/07/2016.
 */
public class ConfigHandlerTweetTagConverter extends ConfigHandler {
    @Override
    public void handle(FeatureExtractionPipeline pipeline, String jsonOptionValue, List<PipelineBuilder.Option> other) {
        if(new Gson().fromJson(jsonOptionValue, Boolean.class)) {
            pipeline.add(new TweetTagConverter(), getKey());
        }
    }

    @Override
    public String getKey() {
        return "tag_converter";
    }
}
