package uk.ac.susx.tag.classificationframework.datastructures;

import com.google.gson.Gson;

import java.io.*;
import java.util.Date;
import java.util.List;

/**
 * Created by Andrew D. Robertson on 23/06/2014.
 */
public class UserActionLog {




    public static class Session {

        private List<Event> events;

        public void userAnnotatedDocument(ProcessedInstance document, String label){
            events.add(new Event("Annotated document (" + document.source.id + ") as '"+label+"': " + document.source.text));
        }

        public void userAnnotatedDocument(Instance document, String label){
            events.add(new Event("Annotated document (" + document.id + ") as '"+label+"': " + document.text));
        }

        public void userAnnotatedFeature(String feature, String label) {
            events.add(new Event("Annotated feature as '" + label+"': " + feature));
        }

        public void classifierPerformance(String performance){
            events.add(new Event("Classifier performance: " + performance));
        }
    }

    public static class Event {

        private String action;
        private String timestamp;

        public Event(String action){
            this.action = action;
            timestamp = new Date().toString();
        }
    }

    public static void saveSession(Session s, File saveFile) throws IOException {
        String jsonString = new Gson().toJson(s);
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(saveFile), "UTF8"))){
            bw.write(jsonString);
        }
    }
}
