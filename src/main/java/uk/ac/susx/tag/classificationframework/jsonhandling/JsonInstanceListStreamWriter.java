package uk.ac.susx.tag.classificationframework.jsonhandling;

/*
 * #%L
 * JsonInstanceListStreamWriter.java - classificationframework - CASM Consulting - 2,013
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

import com.google.gson.stream.JsonWriter;
import uk.ac.susx.tag.classificationframework.datastructures.Instance;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * Using this class, it is possible to write a JSON array of Instance objects iteratively.
 *
 * Usage:
 *
 *  Iterable<Instance> instanceList = some iterable over Instance objects
 *  try (JsonInstanceListStreamWriter sw = new JsonInstanceListStreamWriter(new File("text.json"))){
 *      sw.write(instanceList);
 *  }
 *
 * User: Andrew D. Robertson
 * Date: 22/08/2013
 * Time: 10:32
 */
public class JsonInstanceListStreamWriter implements AutoCloseable {

    private final JsonWriter jsonWriter;

    public JsonInstanceListStreamWriter(File jsonFile) throws IOException {
        jsonWriter = new JsonWriter(new OutputStreamWriter(new FileOutputStream(jsonFile), "UTF8"));
        jsonWriter.beginArray();
    }

    public void close() throws IOException {
        try {jsonWriter.endArray();}
        catch (IllegalStateException ignored){} // We've already ended the array
        jsonWriter.close();
    }

    public void write(Instance i) throws IOException {
        jsonWriter.beginObject();
        jsonWriter.name("label").value(i.label);
        jsonWriter.name("id").value(i.id);
        jsonWriter.name("text").value(i.text);
        jsonWriter.endObject();
    }

    public void write(Iterable<Instance> instances) throws IOException {
        for (Instance i : instances) write(i);
    }
}
