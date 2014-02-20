package uk.ac.susx.tag.classificationframework.featureextraction.normalisation;

/*
 * #%L
 * TokenNormaliserByFormRegexMatch.java - classificationframework - CASM Consulting - 2,013
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

import java.util.regex.Pattern;

/**
 * See TokenNormaliser class for the function of TokenNormalisers.
 *
 * If a token form matches the specified pattern, then it is replaced with
 * a particular string.
 *
 * If the patterned matched, then no further normalisation is allowed from
 * subsequent normalisers.
 *
 * User: Andrew D. Robertson
 * Date: 23/08/2013
 * Time: 10:51
 */
public class TokenNormaliserByFormRegexMatch extends TokenNormaliser {

    private static final long serialVersionUID = 0L;

    private Pattern pattern;
    private String replacement;

    public TokenNormaliserByFormRegexMatch(String pattern, String replacement){
        this.pattern = Pattern.compile(pattern);
        this.replacement = replacement;
    }

    @Override
    public boolean normalise(int index, Document tokens) {
        AnnotatedToken token = tokens.get(index);
        if (pattern.matcher(token.get("form")).matches()){
            token.put("form", replacement);
            return false;
        } else {
            return true;
        }
    }
}
