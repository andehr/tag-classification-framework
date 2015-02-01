package uk.ac.susx.tag.classificationframework.featureextraction.pipelines.confighandlers;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import uk.ac.susx.tag.classificationframework.exceptions.ConfigurationException;
import uk.ac.susx.tag.classificationframework.featureextraction.inference.FeatureInferrerDependencyNGrams;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.PipelineBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Andrew D. Robertson
 * Date: 01/02/2015
 * Time: 13:48
 */

public class ConfigHandlerDependencyNgrams extends ConfigHandler{

    @Override
    public void handle(FeatureExtractionPipeline pipeline, String jsonOptionValue, List<PipelineBuilder.Option> other) {
        Map<String, String> mine = new HashMap<>();
        Map<String, String> options = new Gson().fromJson(jsonOptionValue, new TypeToken<Map<String, String>>(){}.getType());
        mine.putAll(options);

        boolean includeBigrams = ConfigHandler.getAndRemove("include_bigrams", mine, true);
        boolean includeTrigrams = ConfigHandler.getAndRemove("include_trigrams", mine, true);
        boolean collapsePrepositions = ConfigHandler.getAndRemove("collapse_prepositions", mine, true);
        boolean retainUncollapsedPrepositionalNgrams = ConfigHandler.getAndRemove("keep_uncollapsed_prepositional_ngrams", mine, false);

        boolean lowercase = ConfigHandler.getAndRemove("lower_case", mine, true);

        if (mine.size() > 0) {
            throw new ConfigurationException("Unrecognised options[s]: " + ConfigHandler.getUnrecognisedOptionsString(mine));
        }

        if (includeBigrams || includeTrigrams) {
            pipeline.add(new FeatureInferrerDependencyNGrams(true, includeBigrams, includeTrigrams,collapsePrepositions, retainUncollapsedPrepositionalNgrams, true, lowercase));
        }
    }

    @Override
    public String getKey() {
        return "dependency_ngrams";
    }
}
