package uk.ac.susx.tag.classificationframework.exceptions;

/*
 * #%L
 * EvaluationException.java - classificationframework - CASM Consulting - 2,013
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

/**
 * Raised when there is a problem during evaluation of a classifier.
 *
 * User: Andrew D. Robertson
 * Date: 10/02/2014
 * Time: 16:49
 */
public class EvaluationException extends RuntimeException {

    public EvaluationException(String msg) {
        super(msg);
    }

    public EvaluationException(Throwable cause){
        super(cause);
    }

}
