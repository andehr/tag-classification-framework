package uk.ac.susx.tag.classificationframework.featureextraction.pipelines.confighandlers;

/*
 * #%L
 * ConfigHandler.java - classificationframework - CASM Consulting - 2,013
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
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.PipelineBuilder;

import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Andrew D. Robertson
 * Date: 17/02/2014
 * Time: 14:09
 */

public abstract class ConfigHandler<V> {

    public abstract void handle(FeatureExtractionPipeline pipeline, V optionValue, List<PipelineBuilder.Option> other);

    public abstract String getKey();

    /**
     * Convenience method. Given a map from Strings to Objects, particular key we're interested in,
     * and a default value for the key's corresponding value: extract the value if present and cast
     * it to the type of the default value (the strings "true" and "false" are acceptable alternatives to the booleans;
     * they'll be converted to boolean). If the map does not contain the key, then return the default value.
     */
    protected static <T> T getAndRemove(String key, Map<String, Object> options, T defaultValue) {
        T val;

        try {

            if(options.containsKey(key)) {
                val = (T) defaultValue.getClass().cast(options.get(key));
                options.remove(key);
            } else {
                val = defaultValue;
            }
        } catch(NullPointerException e) {
            return defaultValue;

        } catch(ClassCastException e) {
            // in case of "true" or "false" booleans
            try {
                String str = (String)options.get(key);

                if(str.equalsIgnoreCase("true") || str.equalsIgnoreCase("false")) {
                    val = (T) Boolean.valueOf(str);
                } else {
                    throw new ClassCastException();
                }
            } catch (ClassCastException e1) {
                throw new ConfigurationException("Error parsing value: " + key + " -> " + options.get(key));
            }
        }

        options.remove(key);

        return val;
    }

    protected static String getUnrecognisedOptionsString(Map<String, Object> options) {
        StringBuilder sb = new StringBuilder();
        for(Map.Entry<String, Object> entry: options.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append(" ");
        }
        return sb.toString();
    }
}
