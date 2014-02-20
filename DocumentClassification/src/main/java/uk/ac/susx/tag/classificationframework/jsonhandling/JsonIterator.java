package uk.ac.susx.tag.classificationframework.jsonhandling;

/*
 * #%L
 * JsonIterator.java - classificationframework - CASM Consulting - 2,013
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

import java.io.IOException;
import java.util.Iterator;

/**
 * An iterator over the elements in a JSON array, objects read
 * iteratively from file.
 *
 * See JsonListStreamReader for usage.
 *
 * User: Andrew D. Robertson
 * Date: 13/08/2013
 * Time: 14:02
 */
public abstract class JsonIterator<E> implements Iterator<E>, AutoCloseable {

    protected final Gson gson;
    protected final JsonReader jsonReader;

    public JsonIterator(JsonReader jsonReader, Gson gson)  throws IOException {
        this.gson = gson;
        this.jsonReader = jsonReader;
        jsonReader.beginArray();
    }

    public void close() throws IOException {
        try {jsonReader.endArray();}
        catch (IllegalStateException ignored){} // We've already ended the array
        jsonReader.close();
    }

    @Override
    public boolean hasNext(){
        try {
            boolean next = jsonReader.hasNext();
            if (!next) {
                jsonReader.endArray();
                jsonReader.close();
            }
            return next;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public abstract E next();

    @Override
    public void remove() { throw new UnsupportedOperationException(); }
}
