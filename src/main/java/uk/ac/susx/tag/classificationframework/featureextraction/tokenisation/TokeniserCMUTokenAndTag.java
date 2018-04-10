package uk.ac.susx.tag.classificationframework.featureextraction.tokenisation;

/*
 * #%L
 * TokeniserCMUTokenAndTag.java - classificationframework - CASM Consulting - 2,013
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

import cmu.arktweetnlp.Tagger;
import uk.ac.susx.tag.classificationframework.Util;
import uk.ac.susx.tag.classificationframework.datastructures.AnnotatedToken;
import uk.ac.susx.tag.classificationframework.datastructures.Document;
import uk.ac.susx.tag.classificationframework.datastructures.Instance;

import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * See Tokeniser class for the function of Tokenisers.
 *
 * Tokeniser which also assigns PoS tags, using the CMU tokeniser and tagger.
 * The tokenisation type and tags are quite tightly coupled theoretically speaking
 * (e.g. "don't" tagged as a single token so it can be tagged with "L"). And
 * they are convenient to process together because of the Tagger class in the
 * CMU. So the processing happens in the Tokeniser. Otherwise, it would be
 * nicer to put the tagging functionality in a DocProcessor class.
 *
 * User: Andrew D. Robertson
 * Date: 27/07/2013
 * Time: 14:17
 */
public class TokeniserCMUTokenAndTag implements Tokeniser {

    private static final long serialVersionUID = 0L;

    public transient Tagger tagger;

    public TokeniserCMUTokenAndTag() throws IOException {
        loadTagger();
    }

    private void loadTagger() throws IOException {
        tagger = new Tagger();
        tagger.loadModel("/cmu/arktweetnlp/model.20120919");
    }

    public Document tokenise (Instance document) {
        Document processed = new Document(document);
        if (!Util.isNullOrEmptyText(document)){
            for (Tagger.TaggedToken taggedToken : tagger.tokenizeAndTag(document.text)) {
                processed.add(new AnnotatedToken(taggedToken));
            }
        }
        return processed;
    }

    @Override
    public String configuration() {
        return "";
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        loadTagger();
    }
}
