package uk.ac.susx.tag.classificationframework.featureextraction.documentprocessing;

import org.glassfish.jersey.client.JerseyInvocation;
import uk.ac.susx.tag.classificationframework.datastructures.Document;
import uk.ac.susx.tag.classificationframework.exceptions.FeatureExtractionException;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.*;
import javax.ws.rs.core.*;
import java.time.Instant;
import java.time.temporal.TemporalUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by Andrew D. Robertson on 30/06/2016.
 */
public class Service extends DocProcessor {

    private String url;
    private Client client;
    private static final int tries = 5;

    private Map<String, NewCookie> cookies;

    public Service(String url){
        this.url = url;
        this.client = ClientBuilder.newClient();
        cookies = new HashMap<>();
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

                if(cookies != null) {
                    cookies.forEach((key, val) -> {

                        request.cookie(val);
                    });
                }

                Response r = request.post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));


                r.getCookies().forEach((key, cookie) -> {
                    cookies.put(key, cookie);
                });

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
    public void close(){
        client.close();
    }

    @Override
    public String configuration() {
        return "url:"+url;
    }
}
