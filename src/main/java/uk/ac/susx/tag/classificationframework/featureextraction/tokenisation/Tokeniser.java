package uk.ac.susx.tag.classificationframework.featureextraction.tokenisation;

/*
 * #%L
 * Tokeniser.java - classificationframework - CASM Consulting - 2,013
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

import uk.ac.susx.tag.classificationframework.datastructures.Document;
import uk.ac.susx.tag.classificationframework.datastructures.Instance;

import java.io.Serializable;

/**
 * A tokeniser takes a string and produces a list of token objects
 * (as a Document object) which can take further annotations.
 *
 * Ideally, any processing other than that of splitting the string
 * into tokens should be elsewhere. But often it is efficient to
 * lowercase the text before tokenisation, and sometimes it is
 * easier to include other processing when relying on a 3rd
 * party library (see TokeniserCMUTokenAndTag).
 *
 * User: Andrew D. Robertson
 * Date: 27/07/2013
 * Time: 14:40
 */
public interface Tokeniser extends Serializable {

    Document tokenise(Instance document);

    /**
     * Return a string representing the configuration of this
     * Tokeniser's parameters. Given two tokenisers of
     * the same type, they should only return the same
     * configuration if they perform the exact same
     * processing on a Instance as each other.
     *
     * NOTE: make it human readable
     */
    String configuration();

}
