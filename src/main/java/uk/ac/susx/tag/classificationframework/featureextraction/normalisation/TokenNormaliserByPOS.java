package uk.ac.susx.tag.classificationframework.featureextraction.normalisation;

/*
 * #%L
 * TokenNormaliserByPOS.java - classificationframework - CASM Consulting - 2,013
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
 * See TokenNormaliser class for the function of TokenNormalisers.
 *
 * This normaliser will normalise all tokens of a particular PoS.
 *
 * User: Andrew D. Robertson
 * Date: 27/07/2013
 * Time: 12:33
 */
public class TokenNormaliserByPOS extends TokenNormaliser {

    private static final long serialVersionUID = 0L;

    private Set<String> pos;
    private String normalisedForm;

    public TokenNormaliserByPOS(String normalisablePos, String normalisedForm){
        this.pos = new HashSet<String>();
        this.pos.add(normalisablePos);
        this.normalisedForm = normalisedForm;
    }

    public TokenNormaliserByPOS(Set<String> pos, String normalisedForm){
        this.pos = pos;
        this.normalisedForm = normalisedForm;
    }

    @Override
    public boolean normalise(int index, Document tokens) {
        AnnotatedToken token = tokens.get(index);
        if (pos.contains(token.get("pos"))) {
            token.put("form", normalisedForm);
            return false;
        } else {
            return true;
        }
    }


    @Override
    public boolean isThreadSafe() {
        return true;
    }
}
