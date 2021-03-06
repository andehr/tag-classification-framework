package uk.ac.susx.tag.classificationframework.exceptions;

/*
 * #%L
 * ConfigurationException.java - classificationframework - CASM Consulting - 2,013
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

/**
 * Thrown in the event of problems with PipelineConfigurations.
 *
 * User: Simon Wibberley
 * Date: 23/09/2013
 * Time: 18:00
 */
public class ConfigurationException extends RuntimeException {

    public ConfigurationException(String msg) {
        super(msg);
    }

    public ConfigurationException(Throwable cause){
        super(cause);
    }

    public ConfigurationException(String msg, Throwable cause){
        super(msg, cause);
    }
}
