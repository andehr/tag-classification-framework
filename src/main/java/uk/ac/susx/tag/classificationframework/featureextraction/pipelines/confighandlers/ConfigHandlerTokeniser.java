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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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
import uk.ac.susx.tag.classificationframework.featureextraction.tokenisation.TokeniserIllinoisAndNER;
import uk.ac.susx.tag.classificationframework.featureextraction.tokenisation.TokeniserTwitterBasic;
import uk.ac.susx.tag.classificationframework.featureextraction.tokenisation.TokeniserChineseStanford;
import uk.ac.susx.tag.classificationframework.featureextraction.tokenisation.TokeniserArabicStanford;


import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configure a tokeniser and related options for a pipeline.
 *
 * The optionValue associated with this handler is a map of Strings to Objects (where the expected actual
 * types of the Objects are usually either Strings or Booleans (but Strings representing booleans are tolerated).
 *
 * Each expected option has a default, so an empty or incomplete option map is fine.
 *
 * Options are details below (key : value):
 *
 *  type               : the type of the tokeniser ["basic", "cmu", "cmuTokeniseOnly"] (Type: String, Default: "basic")
 *  filter_punctuation : whether to filter punctuation           (Type: boolean, Default: true)
 *  lower_case         : whether to lower case                   (Type: boolean, Default: true)
 *  normalise_urls     : whether to normalise URLs               (Type: boolean, Default: false)
 *  normalise_twitter_usernames : whether to normalise usernames (Type: boolean, Default: false)
 *
 * Notes:
 *
 *  - the "basic" tokeniser always strips all punctuation except ! and ?
 *  - the "cmu" tokeniser includes PoS tagging. The pipeline will use the tags to achieve the punctuation filtering and
 *    URL and username normalisation if these are set. Otherwise regex is used.
 *
 * User: Andrew D. Robertson
 * Date: 17/02/2014
 * Time: 14:14
 */
public class ConfigHandlerTokeniser extends ConfigHandler {

    @Override
    public void handle(FeatureExtractionPipeline pipeline, String jsonOptionValue, List<PipelineBuilder.Option> config) {

        Map<String, String> mine = new HashMap<>();
        Map<String, String> options = new Gson().fromJson(jsonOptionValue, new TypeToken<Map<String, String>>(){}.getType());
        mine.putAll(options);

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
                if(filterPunctuation) pipeline.add(new TokenFilterPunctuation(true), "filter_punctuation");
                if(normaliseURLs)     pipeline.add(new TokenNormaliserByFormRegexMatch("(?:https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*", "HTTPLINK"), "normalise_urls");
                if(normaliseTwitterUsernames) pipeline.add(new TokenNormaliserTwitterUsername(), "normalise_twitter_usernames");
                break;

            case "basic":

                pipeline.setTokeniser(new TokeniserTwitterBasic(filterPunctuation? null : "[!?\"#$%&'()*+,-./:;<=>@\\[\\]^_`{|}~]+", lowerCase, normaliseURLs));
                if(normaliseTwitterUsernames) pipeline.add(new TokenNormaliserTwitterUsername(), "normalise_twitter_usernames");
                break;

            case "chinesestanford":
                try {
                    pipeline.setTokeniser(new TokeniserChineseStanford());
                } catch (IOException e) {throw new ConfigurationException(e);}
                if(lowerCase) pipeline.add(new TokenNormaliserToLowercase(), "lower_case");
                if(filterPunctuation) pipeline.add(new TokenFilterPunctuation(true), "filter_punctuation");
                if(normaliseURLs)     pipeline.add(new TokenNormaliserByFormRegexMatch("(?:https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*", "HTTPLINK"), "normalise_urls");
                break;

            case "arabicstanford":
                try {
                    pipeline.setTokeniser(new TokeniserArabicStanford());
                } catch (IOException e) {throw new ConfigurationException(e);}
                if(lowerCase) pipeline.add(new TokenNormaliserToLowercase(), "lower_case");
                if(filterPunctuation) pipeline.add(new TokenFilterPunctuation(true), "filter_punctuation");
                if(normaliseURLs)     pipeline.add(new TokenNormaliserByFormRegexMatch("(?:https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*", "HTTPLINK"), "normalise_urls");
                break;

            case "illinois":
                if (ConfigHandler.isPresent("edu.illinois.cs.cogcomp.annotation.handler.IllinoisNerExtHandler")){
                    pipeline.setTokeniser(new TokeniserIllinoisAndNER());
                    if(lowerCase) pipeline.add(new TokenNormaliserToLowercase(), "lower_case");
                    if(filterPunctuation) pipeline.add(new TokenFilterPunctuation(true), "filter_punctuation");
                    if(normaliseURLs)     pipeline.add(new TokenNormaliserByFormRegexMatch("(?:https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*", "HTTPLINK"), "normalise_urls");
                    if(normaliseTwitterUsernames) pipeline.add(new TokenNormaliserTwitterUsername(), "normalise_twitter_usernames");
                } else throw new ConfigurationException("The Illinois NER option requires the IllinoisNER dependencies, which must be explicitly included in your POM for licensing reasons. See POM.");
                break;

            default: throw new ConfigurationException("Unsupported tokeniser " + type);
        }
    }

    @Override
    public String getKey() {
        return "tokeniser";
    }
}
