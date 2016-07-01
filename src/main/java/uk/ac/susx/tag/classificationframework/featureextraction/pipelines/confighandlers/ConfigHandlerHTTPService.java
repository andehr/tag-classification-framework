package uk.ac.susx.tag.classificationframework.featureextraction.pipelines.confighandlers;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import uk.ac.susx.tag.classificationframework.featureextraction.documentprocessing.Service;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.PipelineBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The pipeline component added requires closing.
 *
 * Created by Andrew D. Robertson on 01/07/2016.
 */
public class ConfigHandlerHTTPService extends ConfigHandler {

    @Override
    public void handle(FeatureExtractionPipeline pipeline, String jsonOptionValue, List<PipelineBuilder.Option> other) {
        Map<String, String> mine = new HashMap<>();
        Map<String, String> options = new Gson().fromJson(jsonOptionValue, new TypeToken<Map<String, String>>(){}.getType());
        mine.putAll(options);

        String url = ConfigHandler.getAndRemove("url", mine, "");

        if (!url.equals("")) {
            pipeline.add(new Service(url), "service:" + url);
        }
    }

    @Override
    public String getKey() {
        return "http_service";
    }
}
