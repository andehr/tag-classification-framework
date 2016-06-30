package uk.ac.susx.tag.classificationframework.featureextraction.documentprocessing;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import uk.ac.susx.tag.classificationframework.datastructures.Document;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;

/**
 * Created by Andrew D. Robertson on 30/06/2016.
 */
public class Service extends DocProcessor {

    private String url;
    private Client client;

    public Service(String url){
        this.url = url;
        this.client = ClientBuilder.newClient();
    }

    @Override
    public Document process(Document document) {
        String jsonQuery = document.toJson();
        String jsonResponse = null;
        int triesRemaining = 5;
        while (triesRemaining > 0 && jsonResponse == null){
            triesRemaining--;
            try {
                jsonResponse = makeRequest(jsonQuery);
            }
        }

        return Document.fromJson(jsonResponse);
    }

    private String makeRequest(String jsonRequest){
        WebTarget target = client.target(url);

        return target.request()
                .post(Entity.json(jsonRequest), String.class);
    }

    @Override
    public void close(){
        client.close();
    }

    @Override
    public String configuration() {
        return null;
    }
}
