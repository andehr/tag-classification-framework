package uk.ac.susx.tag.classificationframework.featureextraction.normalisation;

/*
 * #%L
 * TokenNormaliser.java - classificationframework - CASM Consulting - 2,013
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
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.PipelineComponent;

import java.util.List;

/**
 * Normalisers perform some processing on the form of a token. It changes
 * the resulting unigrams, bigrams, etc. which will appear as features.
 *
 * A well-behaved normaliser will only perform some kind of normalisation
 * on the token specified by: tokens.get(index).
 *
 * A normaliser is basically a special case of a DocProcessor. It is convenient
 * because only processing on a single token must be coded, and the pipeline
 * will handle iteration over the tokens.
 *
 * It is also possible to stop subsequent normalisation to occur.
 *
 * User: Andrew D. Robertson
 * Date: 27/07/2013
 * Time: 12:34
 */
public abstract class TokenNormaliser extends PipelineComponent {

    private static final long serialVersionUID = 0L;

    /**
     * Perform the normalisation. Subsequent normalisers will only
     * be applied to this token if this method returns True.
     *
     * @return True if the token is allowed further normalisation.
     */
    public abstract boolean normalise(int index, Document tokens);

    public void normaliseBatch(List<Document> documents) {
        throw new UnsupportedOperationException();
    }
}
