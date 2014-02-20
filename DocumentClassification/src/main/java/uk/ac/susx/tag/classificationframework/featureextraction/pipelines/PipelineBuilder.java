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
 * Created with IntelliJ IDEA.
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
                throw new RuntimeException(e);
            }
        }
    }

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
