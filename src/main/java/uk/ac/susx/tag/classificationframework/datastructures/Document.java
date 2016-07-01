package uk.ac.susx.tag.classificationframework.datastructures;

/*
 * #%L
 * Document.java - classificationframework - CASM Consulting - 2,013
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

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import uk.ac.susx.tag.classificationframework.featureextraction.documentprocessing.TweetTagConverter;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A Document during the feature extraction phase. It is designed to act as a List of AnnotatedTokens, but
 * can be augmented with extra properties which do not easily map to individual tokens (using the "attributes" field).
 *
 * A reference to the source that produced the document is maintained (so that source text, and id can be retrieved)
 *
 * Use cloneEmpty() to create a new Document with a reference to the same source (e.g. for using it as a replacement)
 *
 * See FeatureExtractionPipeline class.
 *
 * User: Andrew D. Robertson
 * Date: 19/08/2013
 * Time: 14:09
 */
public class Document extends ArrayList<AnnotatedToken> {

    private static final long serialVersionUID = 0L;

    // Additional annotations that can't be added to the list of annotated tokens
    // Map entries of the form:   ATTRIBUTE-NAME --> ATTRIBUTE
    private Map<String, Object> attributes = new HashMap<>();

    // Reference to the instance from which this document was made
    public Instance source = null;

    private static transient Gson gson = null;

    public Document(){
        this(null);
    }

    public Document(Instance source){
        super();
        this.source = source;
    }

    public Document(List<AnnotatedToken> tokens, Instance source){
        super(tokens);
        this.source = source;
    }

    /**
     * Make a new document that has the same Instance source as this document, but the document itself is empty
     * (empty list).
     */
    public Document cloneEmpty(){
        return new Document(source);
    }

    public Object getAttribute(String name){
        return attributes.get(name);
    }

    public void putAttribute(String name, Object attribute){
        attributes.put(name, attribute);
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString()); sb.append("\n");
        sb.append("Doc Attributes: "); sb.append("\n");
        for (Map.Entry<String, Object> entry : attributes.entrySet()){
            sb.append(" "); sb.append(entry.getKey()); sb.append(": ");
            sb.append(entry.getValue().toString()); sb.append("\n");
        }
        return sb.toString();
    }

    public String toJson(){
        setupGson();
        return gson.toJson(this);
    }

    public static Document fromJson(String jsonDocument){
        return gson.fromJson(jsonDocument, Document.class);
    }

    public static class DocumentDeserializer implements JsonDeserializer<Document> {
        @Override
        public Document deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            final Document document = new Document();
            final JsonObject root = json.getAsJsonObject();
            final JsonArray annotatedTokens = root.get("annotatedTokens").getAsJsonArray();
            for (JsonElement e : annotatedTokens) {
                document.add(context.deserialize(e, AnnotatedToken.class));
            }
            for (Map.Entry<String, JsonElement> entry : root.get("attributes").getAsJsonObject().entrySet()){
                String name = entry.getKey();
                switch (name) {
                    case "ExpandedTokens":
                        document.putAttribute(name, deserialiseExpandedTokens(entry.getValue(), context)); break;
                    default:
                        document.putAttribute(name, context.deserialize(entry.getValue(), Object.class));
                }
            }

            JsonElement sourceElement = root.get("source");
            if (sourceElement != null)
                document.source = context.deserialize(root.get("source").getAsJsonObject(), Instance.class);

            return document;
        }
    }

    public static class DocumentSerializer implements JsonSerializer<Document> {
        @Override
        public JsonElement serialize(Document src, Type typeOfSrc, JsonSerializationContext context) {
            final JsonObject root = new JsonObject();
            final JsonArray annotatedTokens = new JsonArray();
            for (AnnotatedToken t : src) {
                annotatedTokens.add(context.serialize(t));
            }
            root.add("annotatedTokens", annotatedTokens);
            root.add("attributes", context.serialize(src.attributes));
            root.add("source", context.serialize(src.source));
            return root;
        }
    }

    private void setupGson(){
        if (gson == null){
            gson = new GsonBuilder()
                    .registerTypeAdapter(Document.class, new Document.DocumentSerializer())
                    .registerTypeAdapter(Document.class, new Document.DocumentDeserializer())
                    .create();
        }
    }

    private static List<TweetTagConverter.Token> deserialiseExpandedTokens(JsonElement e, JsonDeserializationContext context){
       return context.deserialize(e, new TypeToken<List<TweetTagConverter.Token>>(){}.getType());
    }
}
