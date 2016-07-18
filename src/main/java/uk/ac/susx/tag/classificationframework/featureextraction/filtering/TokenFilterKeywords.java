package uk.ac.susx.tag.classificationframework.featureextraction.filtering;

/*
 * #%L
 * TokenFilterKeywords.java - classificationframework - CASM Consulting - 2,013
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

import java.util.Collection;
import java.util.Set;

/**
 * This is a TokenFilter, see the TokenFilter abstract class for details of function and purpose of
 * token filters.
 *
 * This filter expects as input a set of strings, then whenever it sees a token that matches one of
 * those strings, it will mark that token to be filtered.
 *
 * User: Andrew D. Robertson
 * Date: 27/01/2014
 * Time: 10:49
 */
public class TokenFilterKeywords extends TokenFilter {

    private static final long serialVersionUID = 0L;

    private Set<String> keywords;

    /**
     * @param keywords The tokens that should be filtered.
     */
    public TokenFilterKeywords(Set<String> keywords) {
        this.keywords = keywords;
    }

    @Override
    public synchronized boolean filter(int index, Document tokens) {
        return keywords.contains(tokens.get(index).get("form").toLowerCase());
    }

    public synchronized void addKeyword(String keyword) {
        keywords.add(keyword);
    }

    public synchronized boolean removeKeyword(String keyword) {
        return keywords.remove(keyword);
    }

    public synchronized void setKeywords(Collection<String> kws) {
        keywords.clear();
        keywords.addAll(kws);
    }

    @Override
    public boolean isThreadSafe() {
        return true;
    }
}
