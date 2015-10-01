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

import uk.ac.susx.tag.classificationframework.datastructures.Instance;
import uk.ac.susx.tag.classificationframework.exceptions.ConfigurationException;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.PipelineBuilder;

import java.util.List;
import java.util.Map;

/**
 * ConfigHandlers handle the process of taking a configuration option for the PipelineBuilder and performing the
 * corresponding action in the construction of a FeatureExtractionPipeline.
 *
 * The reason for this abstraction, is to allow the pipeline building process to be extensible externally. I.e. if a
 * user in their own project mimics the package hierarchy of the confighandlers package, then adds their own ConfigHandlers,
 * the PipelineBuilder will also use those ConfigHandlers (via reflection).
 *
 * See the handle() method for a worked example of subclassing a ConfigHandler.
 *
 * Sub-classing guidelines:
 *
 *      - The ConfigHandler will be instantiated with no args.
 *      - The ConfigHandler should be stateless.
 *      - Be as fuzzy with accepting option vales as possible. I.e. if you expect a single boolean as your configuration
 *        option, then also accept the Strings "true" and "false". This class provides a number of convenience methods
 *        for such things.
 *
 * User: Andrew D. Robertson
 * Date: 17/02/2014
 * Time: 14:09
 */

public abstract class ConfigHandler {

//    private List<Instance> data = null;
//
//    public List<Instance> getDataOrNull() {
//        return data;
//    }
//
//    public List<Instance> getDataOrThrow(){
//        if (data == null)
//            throw new ConfigurationException("No data present for handler that requires data.");
//        else return data;
//    }
//
//    public void setData(List<Instance> data) {
//        this.data = data;
//    }

    /**
     * This method is what is called to handle the configuration option that an implementation of this class proposes to handle.
     * Let's work an example:
     *   - You want to create a new type of FeatureInferrer for the FeatureExtractionPipeline.
     *   - It's going to extract trigrams as features.
     *   - First you create a FeatureInferrer (see the relevant class in the featureextraction.inference package)
     *      - The inferrer looks at a document and extracts all trigrams
     *      - It has the option to ignore punctuation trigrams
     *   - You can now create a pipeline manually, and use the pipeline.add() method to add your new inferrer, but
     *   - You want the user to be able to pass a configuration to the PipelineBuilder, and have it automatically
     *     construct a pipeline with trigram capability properly configured.
     *   - So, you need a ConfigHandler for trigrams.
     *   - Subclass this class, following the guidelines in the class description.
     *   - have the getKey() function return a string that explains the new set of options you're adding to the builder
     *     e.g. "trigrams"
     *   - Now, whenever the pipeline builder sees the option with a key "trigrams", it's pass the option's value to
     *     the new trigram ConfigHandler, along with a reference to the pipeline building build, and all the other options
     *     under consideration.
     *   - It is the ConfigHandler's duty to add the new FeatureInferrer to the pipeline and interpret the option value.
     *   - Here we might expect a map:
     *      {
     *        on : true/false
     *        filter_punctuation: true/false
     *      }
     *   - It is down to the person building the options to make sure they provide this map. But try to be flexible.
     *   - So here, cast the optionValue Object to a Map<String, Object>, then allow for the values to be booleans
     *     or strings representing booleans.
     *   - You're done!
     */
    public abstract void handle(FeatureExtractionPipeline pipeline, String jsonOptionValue, List<PipelineBuilder.Option> other);

    /**
     * A key that must be unique among all ConfigHandlers in the package.
     * The key represents the option that this handler will handle.
     * E.g. if the handler handles the inclusion of a dependency parser
     *      then the key might be "dependency_parsing".
     */
    public abstract String getKey();

    /**
     * Convenience method. Given a map from Strings to Objects, particular key we're interested in,
     * and a default value for the key's corresponding value: extract the value if present and cast
     * it to the type of the default value (the strings "true" and "false" are acceptable alternatives to the booleans;
     * they'll be converted to boolean). If the map does not contain the key, then return the default value.
     */
    protected static <T> T getAndRemove(String key, Map<String, ?> options, T defaultValue) {
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
            // in the case of expecting an int but finding a double
            if (defaultValue instanceof Integer && options.get(key) instanceof Double){
                val = (T)new Integer(((Double)options.get(key)).intValue());
            } else {
                // in case of "true" or "false" booleans or string numerics
                try {
                    String str = (String)options.get(key);
                    if (defaultValue instanceof Double)
                        val =  (T)new Double(str);
                    else if (defaultValue instanceof Integer)
                        val =  (T)new Integer(str);
                    else if(str.equalsIgnoreCase("true") || str.equalsIgnoreCase("false")) {
                        val = (T) Boolean.valueOf(str);
                    } else {
                        throw new ClassCastException();
                    }
                } catch (ClassCastException e1) {
                    throw new ConfigurationException("Error parsing value: " + key + " -> " + options.get(key));
                }
            }
        }

        options.remove(key);

        return val;
    }

    /**
     * Convenience method for subclasses.
     * Given a map of options that haven't been recognised, create a printable structure.
     */
    protected static String getUnrecognisedOptionsString(Map<String, ?> options) {
        StringBuilder sb = new StringBuilder();
        for(Map.Entry<String, ?> entry: options.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append(" ");
        }
        return sb.toString();
    }

    /**
     * Convenience method for subclasses.
     * Cast an object to boolean.
     * Add a little fuzziness by also interpreting the strings "true" and "false" with any capitalisation pattern.
     */
    protected static boolean cast2Boolean(Object o){
        try {
            return (boolean)o;
        } catch (ClassCastException e) {
            try {
                String str = (String)o;
                if (str.equalsIgnoreCase("true") || str.equalsIgnoreCase("false"))
                    return Boolean.valueOf(str);
                else if (str.equalsIgnoreCase("yes")) return true;
                else if (str.equalsIgnoreCase("no")) return false;
                else throw new ConfigurationException("Boolean expected, but not found.");
            } catch (ClassCastException e1) {
                throw new ConfigurationException("Boolean expected, but not found");
            }
        }
    }

    /**
     * Check if an optional dependency is present.
     */
    public static boolean isPresent(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (Throwable ex) {
            return false;
        }
    }
}
