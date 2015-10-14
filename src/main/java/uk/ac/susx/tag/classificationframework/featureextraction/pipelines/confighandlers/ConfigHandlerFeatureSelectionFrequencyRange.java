package uk.ac.susx.tag.classificationframework.featureextraction.pipelines.confighandlers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import uk.ac.susx.tag.classificationframework.exceptions.ConfigurationException;
import uk.ac.susx.tag.classificationframework.featureextraction.inference.featureselection.FeatureSelectorDocumentFrequencyRange;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.PipelineBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * Options map:
 * {
 *     lower : <int>
 *     upper : <int>
 * }
 * User: Andrew D. Robertson
 * Date: 14/10/2015
 * Time: 13:42
 */
public class ConfigHandlerFeatureSelectionFrequencyRange extends ConfigHandler {
    @Override
    public void handle(FeatureExtractionPipeline pipeline, String jsonOptionValue, List<PipelineBuilder.Option> other) {
        Map<String, String> mine = new HashMap<>();
        Map<String, String> options = new Gson().fromJson(jsonOptionValue, new TypeToken<Map<String, Object>>(){}.getType());
        mine.putAll(options);

        int lower = ConfigHandler.getAndRemove("lower", mine, 3);
        int upper = ConfigHandler.getAndRemove("upper", mine, -1);

        if (mine.size() > 0) {
            throw new ConfigurationException("Unrecognised options[s]: " + ConfigHandler.getUnrecognisedOptionsString(mine));
        }

        pipeline.add(new FeatureSelectorDocumentFrequencyRange(lower, upper), "selector_freqrange_from_" + lower + "_to_" + upper);
    }

    @Override
    public String getKey() {
        return "feature_selection_frequency_range";
    }
}
