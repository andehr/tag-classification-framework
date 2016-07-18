package uk.ac.susx.tag.classificationframework.featureextraction.normalisation;

/*
 * #%L
 * TokenNormaliserTwitterUsername.java - classificationframework - CASM Consulting - 2,013
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
 * Replace any token that is longer than 1 character and starts with "@",
 * with "USERNAME".
 *
 * If you've used the CMU PoS tagger, you'd be better off using a TokenNormaliserByPoS,
 * and filtering the PoS by the "@" tag.
 *
 * User: Andrew D. Robertson
 * Date: 27/01/2014
 * Time: 11:10
 */
public class TokenNormaliserTwitterUsername extends TokenNormaliser{

    private static final long serialVersionUID = 0L;

    public TokenNormaliserTwitterUsername() { }

    @Override
    public boolean normalise(int index, Document tokens) {
        AnnotatedToken token = tokens.get(index);
        String form = token.get("form");
        if (form.startsWith("@") && form.length() > 1)
            token.put("form", "USERNAME");
        return true;
    }

    @Override
    public boolean isThreadSafe() {
        return true;
    }
}
