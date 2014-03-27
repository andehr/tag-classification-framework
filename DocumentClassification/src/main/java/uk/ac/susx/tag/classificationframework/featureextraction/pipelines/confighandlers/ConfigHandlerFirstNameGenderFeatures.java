package uk.ac.susx.tag.classificationframework.featureextraction.pipelines.confighandlers;

/*
 * #%L
 * ConfigHandlerFirstNameGenderFeatures.java - classificationframework - CASM Consulting - 2,013
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

import uk.ac.susx.tag.classificationframework.featureextraction.inference.FeatureInferrerFirstNameGender;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.PipelineBuilder;

import java.util.List;

/**
 * If optionValue is true, then add features for classifying gender given a first name.
 *
 * Option value type expected: boolean (will tolerate String "true" and "false" in any capitalisation pattern)
 *
 * User: Andrew D. Robertson
 * Date: 18/02/2014
 * Time: 12:47
 */
public class ConfigHandlerFirstNameGenderFeatures extends ConfigHandler{
    @Override
    public void handle(FeatureExtractionPipeline pipeline, Object optionValue, List<PipelineBuilder.Option> other) {
        if (cast2Boolean(optionValue)) pipeline.add(new FeatureInferrerFirstNameGender());
    }

    @Override
    public String getKey() {
        return "first_name_gender_features";  //To change body of implemented methods use File | Settings | File Templates.
    }
}
