package uk.ac.susx.tag.classificationframework.featureextraction.documentprocessing;

import uk.ac.susx.tag.classificationframework.datastructures.Document;
import uk.ac.susx.tag.classificationframework.exceptions.FeatureExtractionException;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

/**
 * Created by Andrew D. Robertson on 30/06/2016.
 */
public class Service extends DocProcessor {

    private String url;
    private Client client;
    private static final int tries = 5;

    public Service(String url){
        this.url = url;
        this.client = ClientBuilder.newClient();
    }

    @Override
    public Document process(Document document) {
        String jsonQuery = document.toJson();
        String jsonResponse = null;
        int lastHTTPCode = 0;
        int triesRemaining = tries;
        // Keep requesting until we run out of tries or get a successful response
        while (triesRemaining > 0 && jsonResponse == null){
            triesRemaining--;
            try {
                WebTarget target = client.target(url);
                Response r = target.request().post(Entity.json(jsonQuery));

                if (r.getStatus() >= 400){ // If error code, record last error code for potential reporting if we run out of tries
                    lastHTTPCode = r.getStatus();
                } else { // Otherwise get JSON response
                    jsonResponse = r.readEntity(String.class);
                }
            } catch (ProcessingException | WebApplicationException e){
                if (triesRemaining == 0){
                    throw new FeatureExtractionException("Service not working. Url: "+url, e);
                }
            }
        }
        if (jsonResponse != null) { // If we got a successful response
            // Return a deserialised document
            return Document.fromJson(jsonResponse);
        } else throw new FeatureExtractionException("Service not working (last error code: " + lastHTTPCode + " url: "+url);
    }


    @Override
    public boolean isThreadSafe() {
        return true;
    }

    @Override
    public void close(){
        client.close();
    }

    @Override
    public String configuration() {
        return "url:"+url;
    }
}
