package uk.ac.susx.tag.classificationframework.featureextraction.filtering;

/*
 * #%L
 * TokenFilter.java - classificationframework - CASM Consulting - 2,013
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
 * A TokenFilter decides whether a particular token should be
 * filtered by returning true if the token should be filtered.
 * The FeatureExtractionPipeline will then use this to set the
 * filtered property on the token in question.
 *
 * FeatureInferrers are the only PipelineComponents which produce
 * actual features (rather than just process the document in some
 * way). It is the duty of the FeatureInferrers to decide whether
 * or not to ignore the filtered property of a token.
 *
 * User: Andrew D. Robertson
 * Date: 27/07/2013
 * Time: 13:23
 */
public abstract class TokenFilter extends PipelineComponent {

    private static final long serialVersionUID = 0L;

    /**
     * Return true if token at tokens[index] should be filtered
     */
    public abstract boolean filter(int index, Document tokens);

    public void filterBatch(List<Document> documents){
        throw new UnsupportedOperationException();
    }
}
