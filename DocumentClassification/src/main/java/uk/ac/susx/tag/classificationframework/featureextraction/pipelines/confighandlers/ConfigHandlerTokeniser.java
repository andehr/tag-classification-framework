package uk.ac.susx.tag.classificationframework.featureextraction.pipelines.confighandlers;

/*
 * #%L
 * ConfigHandlerTokeniser.java - classificationframework - CASM Consulting - 2,013
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

import uk.ac.susx.tag.classificationframework.exceptions.ConfigurationException;
import uk.ac.susx.tag.classificationframework.featureextraction.filtering.TokenFilterByPOS;
import uk.ac.susx.tag.classificationframework.featureextraction.filtering.TokenFilterPunctuation;
import uk.ac.susx.tag.classificationframework.featureextraction.normalisation.TokenNormaliserByFormRegexMatch;
import uk.ac.susx.tag.classificationframework.featureextraction.normalisation.TokenNormaliserByPOS;
import uk.ac.susx.tag.classificationframework.featureextraction.normalisation.TokenNormaliserToLowercase;
import uk.ac.susx.tag.classificationframework.featureextraction.normalisation.TokenNormaliserTwitterUsername;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.PipelineBuilder;
import uk.ac.susx.tag.classificationframework.featureextraction.tokenisation.TokeniserCMUTokenAndTag;
import uk.ac.susx.tag.classificationframework.featureextraction.tokenisation.TokeniserCMUTokenOnly;
import uk.ac.susx.tag.classificationframework.featureextraction.tokenisation.TokeniserTwitterBasic;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Andrew D. Robertson
 * Date: 17/02/2014
 * Time: 14:14
 */
public class ConfigHandlerTokeniser extends ConfigHandler<Map<String, Object>> {

    @Override
    public void handle(FeatureExtractionPipeline pipeline, Map<String, Object> optionMap, List<PipelineBuilder.Option> config) {

        Map<String, Object> mine = new HashMap<>();
        mine.putAll(optionMap);

        String type = ConfigHandler.getAndRemove("type", mine, "basic");
        boolean filterPunctuation = ConfigHandler.getAndRemove("filter_punctuation", mine, true);
        boolean lowerCase = ConfigHandler.getAndRemove("lower_case", mine, true);
        boolean normaliseURLs = ConfigHandler.getAndRemove("normalise_urls", mine, false);
        boolean normaliseTwitterUsernames = ConfigHandler.getAndRemove("normalise_twitter_usernames", mine, false);

        if (mine.size() > 0) {
            throw new ConfigurationException("Unrecognised options[s]: " + ConfigHandler.getUnrecognisedOptionsString(mine));
        }

        switch (type) {

            case "cmu":

                try {
                    pipeline.setTokeniser(new TokeniserCMUTokenAndTag());
                } catch (IOException e) { throw new ConfigurationException(e);}
                if(lowerCase) pipeline.add(new TokenNormaliserToLowercase(), "lower_case");
                if(filterPunctuation) pipeline.add(new TokenFilterByPOS(","), "filter_punctuation");
                if(normaliseURLs)     pipeline.add(new TokenNormaliserByPOS("U", "HTTPLINK"), "normalise_urls");
                if(normaliseTwitterUsernames) pipeline.add(new TokenNormaliserByPOS("@", "USERNAME"), "normalise_twitter_usernames");
                break;

            case "cmuTokeniseOnly":

                pipeline.setTokeniser(new TokeniserCMUTokenOnly());
                if(lowerCase) pipeline.add(new TokenNormaliserToLowercase(), "lower_case");
                if(filterPunctuation) pipeline.add(new TokenFilterPunctuation(), "filter_punctuation");
                if(normaliseURLs)     pipeline.add(new TokenNormaliserByFormRegexMatch("(?:https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*", "HTTPLINK"), "normalise_urls");
                if(normaliseTwitterUsernames) pipeline.add(new TokenNormaliserTwitterUsername(), "normalise_twitter_usernames");
                break;

            case "basic":

                pipeline.setTokeniser(new TokeniserTwitterBasic(lowerCase, normaliseURLs));
                if(normaliseTwitterUsernames) pipeline.add(new TokenNormaliserTwitterUsername(), "normalise_twitter_usernames");
                break;

            default: throw new ConfigurationException("Unsupported tokeniser " + type);
        }
    }

    @Override
    public String getKey() {
        return "tokeniser";  //To change body of implemented methods use File | Settings | File Templates.
    }
}
