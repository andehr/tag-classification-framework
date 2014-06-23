package uk.ac.susx.tag.classificationframework.featureextraction.tokenisation;

/*
 * #%L
 * TokeniserCMUTokenOnly.java - classificationframework - CASM Consulting - 2,013
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

import cmu.arktweetnlp.Twokenize;
import uk.ac.susx.tag.classificationframework.datastructures.AnnotatedToken;
import uk.ac.susx.tag.classificationframework.datastructures.Document;
import uk.ac.susx.tag.classificationframework.datastructures.Instance;

/**
 * See Tokeniser class for the function of Tokenisers.
 *
 * Tokeniser only uses the tokenisation part of the CMU tagger.
 * Optionally the string can also be lowercased. But make sure
 * that subsequent DocProcessors are not relying on capitalisation
 * patterns.
 *
 * User: Andrew D. Robertson
 * Date: 16/08/2013
 * Time: 11:21
 */
public class TokeniserCMUTokenOnly implements Tokeniser {

    private static final long serialVersionUID = 0L;

    public TokeniserCMUTokenOnly() {

    }

    @Override
    public Document tokenise(Instance document) {
        Document tokenised = new Document(document);
        if (!document.text.trim().isEmpty()){
            for (String token : Twokenize.tokenizeRawTweetText(document.text)) {
                tokenised.add(new AnnotatedToken(token));
            }
        }
        return tokenised;
    }

    @Override
    public String configuration() {
        return "";
    }
}
