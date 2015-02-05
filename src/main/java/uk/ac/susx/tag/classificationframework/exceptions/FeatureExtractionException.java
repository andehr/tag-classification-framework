package uk.ac.susx.tag.classificationframework.exceptions;

/*
 * #%L
 * FeatureExtractionException.java - classificationframework - CASM Consulting - 2,013
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
 * Instantiated when there is a problem with feature extraction, which does not easily
 * map to Java's standard exceptions (e.g. instantiating an AnnotatedToken object without
 * a "form" property). Usually implies that user has instantiated a pipeline in a non-sensible
 * way (like used a feature inferrer that requires PoS tags, but not used a docProcessor or
 * similar to actually add the PoS tags).
 *
 * User: Andrew D. Robertson
 * Date: 08/08/2013
 * Time: 10:38
 */
public class FeatureExtractionException extends RuntimeException {

    public FeatureExtractionException(String message) {
        super(message);
    }

    public FeatureExtractionException(Throwable cause){
        super(cause);
    }

    public FeatureExtractionException(String message, Throwable cause){
        super(message, cause);
    }
}
