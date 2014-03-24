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

import org.reflections.Reflections;
import uk.ac.susx.tag.classificationframework.exceptions.ConfigurationException;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.confighandlers.ConfigHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Use this class to build a FeatureExtractionPipeline from a list of config options.
 *
 * An Option instance is a [key, value] pair, where the key corresponds to the string given by the getKey() method of
 * a class that implements ConfigHandler (see confighandlers package). And the value corresponds to the configuration
 * options that that particular ConfigHandler expects. Each ConfigHandler could expect a different data structure for
 * its configuration options; the data structure is specified by the parameterisation of the ConfigHandler class. E.g.
 * if a handler extends ConfigHandler<Boolean>, then the Option value required for that handler is of type Boolean.
 *
 * Pass a list of such options to the build() method in order to obtain a configured pipeline.
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
 *
 *
 * User: Andrew D. Robertson
 * Date: 17/02/2014
 * Time: 14:54
 */
public class PipelineBuilder {

    private Map<String, ConfigHandler> handlers = new HashMap<>();

    public PipelineBuilder() {
        Reflections reflections = new Reflections("uk.ac.susx.tag.classificationframework.featureextraction.pipelines.confighandlers");

        Set<Class<? extends ConfigHandler>> foundHandlers = reflections.getSubTypesOf(ConfigHandler.class);

        for(Class<? extends ConfigHandler> klass : foundHandlers) {
            try {
                ConfigHandler handler = klass.newInstance();
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
     */
    public static class Option {

        public Option(String key, Object value) {
            this.key = key; this.value = value;
        }

        public String key;
        public Object value;
    }

    public Set<String> getConfigKeys() {
        return handlers.keySet();
    }

    /**
     * Create a pipeline from a list of configuration options. See class description.
     */
    public FeatureExtractionPipeline build(List<Option> config){

        FeatureExtractionPipeline pipeline = new FeatureExtractionPipeline();

        for(Option opt : config) {

            try {
                if (handlers.containsKey(opt.key))
                    handlers.get(opt.key).handle(pipeline, opt.value, config);
                else throw new ConfigurationException("Unrecognised option: " + opt.key);

            } catch (ClassCastException e) {
                throw new ConfigurationException("Option value (" + opt.value + ") is incorrect type for option key (" + opt.key + ")");
            }
        }

        if(!pipeline.tokeniserAssigned()) throw new ConfigurationException("No tokeniser assigned to pipeline.");
        return pipeline;
    }
}
