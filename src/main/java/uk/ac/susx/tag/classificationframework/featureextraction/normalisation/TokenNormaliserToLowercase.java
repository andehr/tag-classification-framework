package uk.ac.susx.tag.classificationframework.featureextraction.normalisation;

/*
 * #%L
 * TokenNormaliserToLowercase.java - classificationframework - CASM Consulting - 2,013
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

/**
 * See TokenNormaliser class for the function of TokenNormalisers.
 *
 * This normaliser will lowercase all tokens. Subsequent normalisation
 * is always allowed.
 *
 * Often, for efficiency, tokenisers will have the option to lowercase
 * before the document is split into tokens. But this is not always
 * sensible. For example, if the tokens are to be PoS-tagged, then
 * capitalisation patterns can be important. So this normaliser is useful
 * for this scenario when the document has already been tokenised but
 * lowercase unigrams are still desired.
 *
 * User: Andrew D. Robertson
 * Date: 05/08/2013
 * Time: 12:02
 */
public class TokenNormaliserToLowercase extends TokenNormaliser {

    private static final long serialVersionUID = 0L;

    @Override
    public boolean normalise(int index, Document tokens) {
        AnnotatedToken token = tokens.get(index);
        token.put("form", token.get("form").toLowerCase());
        return true;
    }
}
