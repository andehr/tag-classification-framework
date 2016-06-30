package uk.ac.susx.tag.classificationframework.featureextraction.pipelines;

/*
 * #%L
 * PipelineComponent.java - classificationframework - CASM Consulting - 2,013
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

import java.io.Serializable;

/**
 * Class representing some kind of feature extraction or pre-feature extraction
 * process. All components in a pipeline save the tokeniser implement this class.
 *
 * This class can be used to unify any functionality across all PipelineComponents.
 * Currently, this is only in the form of a "online" property. Every PipelineComponent
 * has online set to true or false. If it is set to false, then the FeatureExtractionPipeline
 * will ignore it when extracting features. This is useful when maintaining a pipeline and
 * an interactive interface; if a user wishes to turn of a certain feature, then the
 * corresponding component can be set offline, rather than having to re-instantiate a
 * whole new pipeline.
 *
 * When implementing a new PipelineComponent, follow these rules:
 *
 *  1. When creating a new PipelineComponent, the specific type of component
 *     should be sub-classed (e.g. TokenFilter, FeatureInferrer, DocProcessor,
 *     TokenNormaliser). Do not directly sub-class PipelineComponent. Directly sub-classing
 *     this class will require re-implementation of "FeatureExtractionPipeline" and supposes
 *     that the feature extraction framework has design flaws. This will no doubt break past serialisations.
 *
 *  2. The new PipelineComponent should be serializable. The serialised state should store
 *     any configuration options made for that feature extractor. It should avoid containing
 *     large resources which can just be acquired or calculated after deserialisation.
 *     A good example is "TokeniserCMUTokenAndTag"; although it's not a PipelineComponent, it
 *     demonstrates the same principle:  the tagger model is not serialised, but reloaded
 *     after deserialisation.
 *
 *  3. Include a declaration of "private static final long serialVersionUID", so we don't have
 *     issues with modifications under serialisation.
 *
 *  4. Check the relevant feature extraction interfaces for notes on responsibilities of each
 *     feature extractor.
 *
 * User: Andrew D. Robertson
 * Date: 08/08/2013
 * Time: 14:30
 */
public abstract class PipelineComponent implements Serializable, AutoCloseable {

    private static final long serialVersionUID = 0L;

    private boolean online = true;

    public void setOnline(boolean online) {
        this.online = online;
    }

    public void setOnline(){
        online = true;
    }

    public void setOffline(){
        online = false;
    }

    public boolean isOnline() {
        return online;
    }

    public void close() {}

}
