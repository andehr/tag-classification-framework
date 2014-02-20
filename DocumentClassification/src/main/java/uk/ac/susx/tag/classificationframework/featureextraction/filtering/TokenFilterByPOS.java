package uk.ac.susx.tag.classificationframework.featureextraction.filtering;

/*
 * #%L
 * TokenFilterByPOS.java - classificationframework - CASM Consulting - 2,013
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

import uk.ac.susx.tag.classificationframework.datastructures.AnnotatedToken;
import uk.ac.susx.tag.classificationframework.datastructures.Document;

import java.util.HashSet;
import java.util.Set;

/**
 * This is a TokenFilter, see the TokenFilter abstract class for details of function and purpose of
 * token filters.
 *
 * Filter tokens based on their PoS tag. A list of exceptions can be provided to.
 *
 * For example:
 *
 *  filter = new TokenFilterByPOS(",", Sets.newHashSet("?","!"))
 *
 * This produces a filter which filters tokens with the "," tag (CMU punctuation tag) attached,
 * so long as they aren't a question mark, or an exclamation mark.
 *
 * User: Andrew D. Robertson
 * Date: 27/07/2013
 * Time: 13:40
 */
public class TokenFilterByPOS extends TokenFilter {

    private static final long serialVersionUID = 0L;

    private String pos;
    private Set<String> exceptions;

    public TokenFilterByPOS(String pos, Set<String> exceptions) {
        this.pos = pos;
        this.exceptions = exceptions;
    }

    public TokenFilterByPOS(String pos){
        this(pos, new HashSet<String>());
    }

    public boolean filter(int index, Document tokens) {
        AnnotatedToken token = tokens.get(index);
        return token.get("pos").equals(pos) && !exceptions.contains(token.get("form"));
    }
}
