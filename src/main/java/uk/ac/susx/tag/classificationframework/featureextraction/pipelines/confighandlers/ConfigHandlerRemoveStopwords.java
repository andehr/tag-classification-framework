package uk.ac.susx.tag.classificationframework.featureextraction.pipelines.confighandlers;

/*
 * #%L
 * ConfigHandlerRemoveStopwords.java - classificationframework - CASM Consulting - 2,013
 * %%
 * Copyright (C) 2013 - 2014 CASM Consulting
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import uk.ac.susx.tag.classificationframework.featureextraction.filtering.TokenFilterRelevanceStopwords;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.PipelineBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * If optionValue is true, then remove stopwords (that are particularly suited
 * to the relevance classification problem).
 *
 * Option value type expected: boolean (will tolerate String "true" and "false" in any capitalisation pattern)
 *
 * User: Andrew D. Robertson
 * Date: 17/02/2014
 * Time: 18:22
 */
public class ConfigHandlerRemoveStopwords extends ConfigHandler {
    @Override
    public void handle(FeatureExtractionPipeline pipeline, String jsonOptionValue, List<PipelineBuilder.Option> other) {

        if(jsonOptionValue.contains("use") && jsonOptionValue.contains("lang")){
            Map<String, String> mine = new HashMap<>();
            Map<String, String> options = new Gson().fromJson(jsonOptionValue, new TypeToken<Map<String, String>>(){}.getType());
            mine.putAll(options);

            String type = ConfigHandler.getAndRemove("lang", mine, "en");
            boolean use = ConfigHandler.getAndRemove("use", mine, false);

            if(use){
                if(TokenFilterRelevanceStopwords.supportedLanguages(type)) {
                    pipeline.add(new TokenFilterRelevanceStopwords(type), getKey());
                } else {
                    throw new RuntimeException("Language not supported: " + type);
                }
            }
        }
        else {
            if(new Gson().fromJson(jsonOptionValue, Boolean.class))  // This is pretty tolerant of all the possible ways true and false could appear
                pipeline.add(new TokenFilterRelevanceStopwords("en"), getKey());

        }

//        if(new Gson().fromJson(jsonOptionValue, Boolean.class))  // This is pretty tolerant of all the possible ways true and false could appear
//            pipeline.add(new TokenFilterRelevanceStopwords("en"), getKey());
    }

    @Override
    public String getKey() {
        return "remove_stopwords";  //To change body of implemented methods use File | Settings | File Templates.
    }
}
