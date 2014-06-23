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
 * Created with IntelliJ IDEA.
 * User: Andrew D. Robertson
 * Date: 17/02/2014
 * Time: 18:54
 */
public class ConfigHandlerDependencyParser extends ConfigHandler {
    @Override
    public void handle(FeatureExtractionPipeline pipeline, String jsonOptionValue, List<PipelineBuilder.Option> other) {
        if(new Gson().fromJson(jsonOptionValue, Boolean.class)) {
            pipeline.add(new TweetTagConverter(), "tag_converter");
            try {
                pipeline.add(new ArcEagerDependencyParser(), getKey());
            } catch (IOException e) {
                throw new ConfigurationException(e);
            }
        }
    }

    @Override
    public String getKey() {
        return "dependency_parser";
    }
}
