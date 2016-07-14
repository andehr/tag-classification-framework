package uk.ac.susx.tag.classificationframework.featureextraction.normalisation;

/*
 * #%L
 * TokenNormaliserByForm.java - classificationframework - CASM Consulting - 2,013
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

import com.google.common.collect.Sets;
import uk.ac.susx.tag.classificationframework.datastructures.AnnotatedToken;
import uk.ac.susx.tag.classificationframework.datastructures.Document;

import java.util.Set;

/**
 * See TokenNormaliser class for the function of TokenNormalisers.
 *
 * This normaliser will replace any of a set of token forms by a
 * single normalised form.
 *
 * User: Andrew D. Robertson
 * Date: 27/07/2013
 * Time: 13:14
 */
public class TokenNormaliserByForm extends TokenNormaliser{

    private static final long serialVersionUID = 0L;

    private Set<String> forms;
    private String normalisedForm;
    private boolean lowerCaseChecking;

    /**
     * If lowerCaseChecking is true, then "toLowerCase()" will be called on all tokens before checking
     * for membership in normalisableForms.
     */
    public TokenNormaliserByForm(Set<String> normalisableForms, String normalisedForm, boolean lowerCaseChecking){
        this.forms = normalisableForms;
        this.normalisedForm = normalisedForm;
        this.lowerCaseChecking = lowerCaseChecking;
    }

    public TokenNormaliserByForm(Set<String> forms, String normalisedForm){
        this(forms, normalisedForm, true);
    }

    @Override
    public boolean normalise(int index, Document tokens) {
        AnnotatedToken token = tokens.get(index);
        if (forms.contains(lowerCaseChecking ? token.get("form").toLowerCase() : token.get("form"))) {
            token.put("form", normalisedForm);
            return false;
        } else {
            return true;
        }
    }

    public static TokenNormaliserByForm createAuthorNormaliser() {
        return createAuthorNormaliser("AUTHOR('s)");
    }
    public static TokenNormaliserByForm createAuthorNormaliser(String normalisedForm) {
        //TODO: Dealing with "yours truly"?
        return new TokenNormaliserByForm(Sets.newHashSet("i", "me", "my", "mine", "myself", "maself", "meself"), normalisedForm);
    }

    @Override
    public boolean isThreadSafe() {
        return true;
    }



}
