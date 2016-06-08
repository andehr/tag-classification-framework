package uk.ac.susx.tag.classificationframework.featureextraction.pipelines.confighandlers;

import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import uk.ac.susx.tag.classificationframework.exceptions.ConfigurationException;
import uk.ac.susx.tag.classificationframework.featureextraction.documentprocessing.PhraseMatcher;
import uk.ac.susx.tag.classificationframework.featureextraction.inference.FeatureInferrerPhraseNgrams;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.PipelineBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by Andrew D Robertson on 07/06/2016.
 */
public class ConfigHandlerPhraseNgrams extends ConfigHandler {

    @Override
    public void handle(FeatureExtractionPipeline pipeline, String jsonOptionValue, List<PipelineBuilder.Option> other) {
        Map<String, Object> mine = new HashMap<>();
        Map<String, Object> options = new Gson().fromJson(jsonOptionValue, new TypeToken<Map<String, Object>>(){}.getType());
        mine.putAll(options);

        List<String> phrases = ConfigHandler.getAndRemove("ngrams", mine, new ArrayList<String>());
        boolean allowOverlaps = ConfigHandler.getAndRemove("allow_overlaps", mine, false);
        boolean lowerCase = ConfigHandler.getAndRemove("lowercase", mine, true);
        boolean filterMatches = ConfigHandler.getAndRemove("filter_matches", mine, true);
        boolean ignoreFilteredTokens = ConfigHandler.getAndRemove("ignore_filtered_tokens", mine, true);


        if (mine.size() > 0) {
            throw new ConfigurationException("Unrecognised options[s]: " + ConfigHandler.getUnrecognisedOptionsString(mine));
        }

        if (phrases.size() > 0) {

            List<ImmutableList<String>> patterns = phrases.stream().map(p -> ImmutableList.copyOf(p.split(" "))).collect(Collectors.toList());

            pipeline.add(new PhraseMatcher(patterns,lowerCase, allowOverlaps, filterMatches));
            pipeline.add(new FeatureInferrerPhraseNgrams(ignoreFilteredTokens));
        }
    }

    @Override
    public String getKey() {
        return "phrase_ngrams";
    }
}
