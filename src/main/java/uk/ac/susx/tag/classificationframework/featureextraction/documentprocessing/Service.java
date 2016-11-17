package uk.ac.susx.tag.classificationframework.featureextraction.documentprocessing;

import uk.ac.susx.tag.classificationframework.datastructures.Document;
import uk.ac.susx.tag.classificationframework.exceptions.FeatureExtractionException;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Andrew D. Robertson on 30/06/2016.
 */
public class Service extends DocProcessor {

    private static final long serialVersionUID = -1401249243453799130L;

    private String url;
    private transient Client client;
    private static final int tries = 5;


    public Service(String url){
        this.url = url;
        this.client = ClientBuilder.newClient();
    }

    public String getUrl(){
        return url;
    }

    public void setUrl(String url){
        this.url = url;
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
                Form form = new Form();
                form.param("document", jsonQuery);
                Invocation.Builder request = target.request(MediaType.APPLICATION_JSON_TYPE);

                Response r = request.post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));

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
    public List<Document> processBatch(List<Document> documents){
        String jsonQuery = Document.toJsonList(documents);
        String jsonResponse = null;
        int lastHTTPCode = 0;
        int triesRemaining = tries;
        // Keep requesting until we run out of tries or get a successful response
        while (triesRemaining > 0 && jsonResponse == null){
            triesRemaining--;
            try {
                WebTarget target = client.target(url);
                Form form = new Form();
                form.param("documents", jsonQuery);
                Invocation.Builder request = target.request(MediaType.APPLICATION_JSON_TYPE);

                Response r = request.post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));

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
            return Document.fromJsonList(jsonResponse);
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

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        client = ClientBuilder.newClient();
    }
}
