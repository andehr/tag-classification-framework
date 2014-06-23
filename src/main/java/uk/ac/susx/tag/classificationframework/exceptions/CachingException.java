package uk.ac.susx.tag.classificationframework.exceptions;

/*
 * #%L
 * CachingException.java - classificationframework - CASM Consulting - 2,013
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
 * Created with IntelliJ IDEA.
 * User: Andrew D. Robertson
 * Date: 22/01/2014
 * Time: 15:42
 */
public class CachingException extends RuntimeException {

    public CachingException(String msg) {
        super(msg);
    }

    public CachingException(Throwable cause){
        super(cause);
    }
}