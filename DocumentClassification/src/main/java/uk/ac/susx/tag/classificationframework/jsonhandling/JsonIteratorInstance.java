package uk.ac.susx.tag.classificationframework.jsonhandling;

/*
 * #%L
 * JsonIteratorInstance.java - classificationframework - CASM Consulting - 2,013
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

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import uk.ac.susx.tag.classificationframework.datastructures.Instance;

import java.io.IOException;
import java.util.NoSuchElementException;

/**
 * An iterator over the elements in a JSON array, objects read
 * iteratively from file.
 *
 * Objects are expected to be Instances.
 *
 * See JsonListStreamReader for usage.
 *
 * User: Andrew D. Robertson
 * Date: 13/08/2013
 * Time: 13:58
 */
public class JsonIteratorInstance extends JsonIterator<Instance> {

    public JsonIteratorInstance(JsonReader jsonReader, Gson gson)  throws IOException {
        super(jsonReader, gson);
    }

    @Override
    public Instance next() {
        try {
            if (jsonReader.hasNext()) {
                return gson.fromJson(jsonReader, Instance.class);
            } else {
                throw new NoSuchElementException();
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new NoSuchElementException();
        }
    }
}
