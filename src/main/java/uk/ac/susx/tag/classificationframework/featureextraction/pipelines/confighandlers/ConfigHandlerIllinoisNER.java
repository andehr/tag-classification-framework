package uk.ac.susx.tag.classificationframework.featureextraction.pipelines.confighandlers;

import com.google.gson.Gson;
import uk.ac.susx.tag.classificationframework.exceptions.ConfigurationException;
import uk.ac.susx.tag.classificationframework.featureextraction.documentprocessing.IllinoisNER;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.PipelineBuilder;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Andrew D. Robertson
 * Date: 06/02/2015
 * Time: 11:57
 */
public class ConfigHandlerIllinoisNER extends ConfigHandler {
    @Override
    public void handle(FeatureExtractionPipeline pipeline, String jsonOptionValue, List<PipelineBuilder.Option> other) {
        if(new Gson().fromJson(jsonOptionValue, Boolean.class)) {
            if (ConfigHandler.isPresent("edu.illinois.cs.cogcomp.annotation.handler.IllinoisNerExtHandler")){
                pipeline.add(new IllinoisNER());

            } else throw new ConfigurationException("The Illinois NER option requires the IllinoisNER dependencies, which must be explicitly included in your POM for licensing reasons. See POM.");
        }
    }

    @Override
    public String getKey() {
        return "illinois_ner";
    }
}
