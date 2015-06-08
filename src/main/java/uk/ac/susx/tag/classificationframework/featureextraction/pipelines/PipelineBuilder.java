package uk.ac.susx.tag.classificationframework.featureextraction.pipelines;

/*
 * #%L
 * PipelineBuilder.java - classificationframework - CASM Consulting - 2,013
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
import org.reflections.Reflections;
import uk.ac.susx.tag.classificationframework.datastructures.Instance;
import uk.ac.susx.tag.classificationframework.exceptions.ConfigurationException;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.confighandlers.ConfigHandler;

import java.util.*;

/**
 * Use this class to build a FeatureExtractionPipeline from a list of config options.
 *
 * An Option instance is a [key, value] pair, where the key corresponds to the string given by the getKey() method of
 * a class that implements ConfigHandler (see confighandlers package). And the value corresponds to the configuration
 * options that that particular ConfigHandler expects. Each ConfigHandler could expect a different data structure for
 * its configuration options; it will take an Object as its configuration, then attempt to cast to the needed dataformat.
 *
 * Pass a list of such options to the build() method in order to obtain a configured pipeline. Check out the OptionList
 * class for a convenient way of doing this.
 *
 * ConfigurationExceptions are thrown when:
 *
 *  - There is a problem instantiating a ConfigHandler (this should only happen if a handler is implemented incorrectly)
 *  - There is no handler matching the key you specify.
 *  - The option value type does not match the type required by the relevant handler.
 *  - A tokeniser is not specified (or incorrectly specified).
 *
 * Individual handlers may choose to throw an exception if they encounter unexpected additional options.
 *
 * User: Andrew D. Robertson
 * Date: 17/02/2014
 * Time: 14:54
 */
public class PipelineBuilder {

    private Map<String, ConfigHandler> handlers = new HashMap<>();

    /**
     * When constructed, the builder uses reflection on the confighandlers package to determine which config options
     * can be handled. This allows other projects to define classes in this package that handle new options for
     * configuring a pipeline. For example, if you wished to add the option for including a new FeatureInferrer,
     * you'd first define the inferrer, then define a ConfigHandler that presents the option to configure that
     * inferrer, and knows how to add that inferrer to the pipeline. Then this class will see that new config option
     * and allow its use in pipeline building.
     */
    public PipelineBuilder() {
        Reflections reflections = new Reflections("uk.ac.susx.tag.classificationframework.featureextraction.pipelines.confighandlers");

        // Find the set of all classes in the confighandlers package that subclass ConfigHandler
        Set<Class<? extends ConfigHandler>> foundHandlers = reflections.getSubTypesOf(ConfigHandler.class);

        // Get an instance of each available handler
        for(Class<? extends ConfigHandler> klass : foundHandlers) {
            try {
                // Create the instance
                ConfigHandler handler = klass.newInstance();

                // Each handler is associated which a key, and if a coder has introduced a key that a handler already has, then an exception is thrown.
                if (handlers.containsKey(handler.getKey())) throw new ConfigurationException("A handler has been defined with a duplicate key: " + handler.getKey());

                handlers.put(handler.getKey(), handler);

            } catch (IllegalAccessException | InstantiationException e) {
                throw new ConfigurationException(e);
            }
        }
    }

    /**
     * An option passed to the build() method in order to configure a pipeline.
     *
     * The key corresponds to the value obtained from a call to getKey() on a class implementing ConfigHandler.
     * The value corresponds to the configuration options associated with that particular ConfigHandler.
     *
     *
     * NOTE: It may look a little risky just JSON-ifying anything that isn't immediately
     *       obviously a String, or how the ConfigHandlers may receive booleans as "true" or "\"true\"",
     *       or generally strings being in quotes or not, but the Gson decode
     *       process is tolerant of this.
     */
    public static class Option {

        public Option(String key, Object value) {
            this.key = key;
            this.value = new Gson().toJson(value);
            this.data = null;
        }

        public Option(String key, String jsonString) {
            this.key = key;
            this.value = jsonString;
            this.data = null;
        }

        public Option(String key, Object value, List<Instance> data){
            this.key = key;
            this.value = new Gson().toJson(value);
            this.data = data;
        }

        public Option(String key, String jsonString, List<Instance> data){
            this.key = key;
            this.value = jsonString;
            this.data = data;
        }

        public String key;
        public String value;
        public List<Instance> data;

        public boolean isDataPresent(){
            return data != null;
        }
    }

    /**
     * Convenience class. Instead of manually building a list of Option instances.
     * You can create an OptionList, and repeatedly call the "add()" method
     * with the arguments to instantiate an Option instance for you:
     *
     * OptionList config = new OptionList()
     *      .add("tokeniser", ImmutableMap.of("type", "basic", "normalise_urls","true","lower_case", "true"))
     *      .add("remove_stopwords", true)
     *      .add("unigrams", true);
     * FeatureExtractionPipeline pipe = new PipelineBuilder().build(config)
     *
     */
    public static class OptionList extends ArrayList<Option>{

        public OptionList() {
            super();
        }

        public OptionList(String key, Object value) {
            super();
            add(key, value);
        }

        public OptionList add(String key, Object value) {
            this.add(new Option(key, value));
            return this;
        }

        public OptionList add(String key, String jsonString) {
            this.add(new Option(key, jsonString));
            return this;
        }

        public OptionList add(String key, String jsonString, List<Instance> data){
            this.add(new Option(key, jsonString, data));
            return this;
        }

        public OptionList add(String key, Object value, List<Instance> data){
            this.add(new Option(key, value, data));
            return this;
        }
    }

    /**
     * Create a pipeline from a list of configuration options. See class description.
     */
    public FeatureExtractionPipeline build(List<Option> config){

        // Instantiate an empty pipeline (no tokeniser)
        FeatureExtractionPipeline pipeline = new FeatureExtractionPipeline();

        // Attempt to handle each config option in order
        for(Option opt : config) {
            try {
                // If a handler is present than can deal with this config option, then call its handle() method
                if (handlers.containsKey(opt.key)) {
                    if (opt.isDataPresent())
                        handlers.get(opt.key).setData(opt.data);
                    handlers.get(opt.key).handle(pipeline, opt.value, config);
                }
                else throw new ConfigurationException("Unrecognised option: " + opt.key);

            } catch (ClassCastException e) {
                // If any option casting went wrong, throw an exception.
                throw new ConfigurationException("Option value (" + opt.value + ") is incorrect type for option key (" + opt.key + ")");
            }
        }
        // If none of the options specified a tokeniser, thrown an exception.
        if(!pipeline.tokeniserAssigned()) throw new ConfigurationException("No tokeniser assigned to pipeline.");
        return pipeline;
    }

/**
 * Validation helper methods.
 */

    public Set<String> getAvailableHandlerKeys() {
        return handlers.keySet();
    }

    /**
     * Check to see if a particular option handler is available using its key.
     */
    public boolean isHandlerAvailable(String handlerKey){
        return handlers.containsKey(handlerKey);
    }

    /**
     * Given a list of options return set of the keys of any handlers that are not
     * present to deal with said options.
     */
    public Set<String> getUnavailableHandlerKeys(List<Option> config){
        Set<String> unavailableHandlerKeys = new HashSet<>();

        for (Option opt : config) {
            if (!handlers.containsKey(opt.key))
                unavailableHandlerKeys.add(opt.key);
        }
        return unavailableHandlerKeys;
    }

    /**
     * Return true only if there are handlers available for all the options in a config list.
     */
    public boolean areAllHandlersAvailable(List<Option> config) {
        return getUnavailableHandlerKeys(config).isEmpty();
    }
}
