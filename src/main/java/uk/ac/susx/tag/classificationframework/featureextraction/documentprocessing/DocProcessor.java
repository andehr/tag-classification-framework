package uk.ac.susx.tag.classificationframework.featureextraction.documentprocessing;

/*
 * #%L
 * DocProcessor.java - classificationframework - CASM Consulting - 2,013
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
 * A document processor is NOT designed to produce features, but to annotate or modify
 * the tokens of a document.
 *
 * Subsequent PipelineComponents will only be able to see a Document which has
 * already been passed through all DocProcessors. This fact can be used to ensure
 * that all feature inferrers are subject to certain properties.
 *
 * The "attributes" field of a Document source can be used to add annotations
 * which are not Token specific, or don't easily map to the particular tokenisation.
 *
 * E.g. add PoS tags, dependency relations, etc.
 *
 * User: Andrew D. Robertson
 * Date: 27/07/2013
 * Time: 14:11
 */
public abstract class DocProcessor extends PipelineComponent {

    private static final long serialVersionUID = 0L;

    /**
     * Perform processing and return the processed Document.
     */
    public abstract Document process (Document document);

    public List<Document> processBatch(List<Document> documents){
        throw new UnsupportedOperationException();
    }

    /**
     * Return a string representing the configuration of this
     * DocProcessor's parameters. Given two DocProcessors of
     * the same type, they should only return the same
     * configuration if they perform the exact same
     * processing on a Document as each other.
     *
     * This is used to compare the DocProcessor configurations
     * of a FeatureExtractionPipeline, so that the pipeline
     * can decide whether documents cached in a MongoDB
     * cache were processed in the same way as the current
     * pipeline.
     * 
     * NOTE: Make it human-readable.
     */
    public abstract String configuration();
}
