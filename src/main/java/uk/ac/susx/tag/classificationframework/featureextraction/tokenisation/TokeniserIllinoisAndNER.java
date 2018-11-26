package uk.ac.susx.tag.classificationframework.featureextraction.tokenisation;

import edu.illinois.cs.cogcomp.annotation.handler.IllinoisNerExtHandler;
import edu.illinois.cs.cogcomp.curator.RecordGenerator;
import edu.illinois.cs.cogcomp.thrift.base.AnnotationFailedException;
import edu.illinois.cs.cogcomp.thrift.base.Labeling;
import edu.illinois.cs.cogcomp.thrift.base.Span;
import edu.illinois.cs.cogcomp.thrift.curator.Record;
import org.apache.thrift.TException;
import uk.ac.susx.tag.classificationframework.Util;
import uk.ac.susx.tag.classificationframework.datastructures.AnnotatedToken;
import uk.ac.susx.tag.classificationframework.datastructures.Document;
import uk.ac.susx.tag.classificationframework.datastructures.Instance;
import uk.ac.susx.tag.classificationframework.exceptions.ConfigurationException;
import uk.ac.susx.tag.classificationframework.exceptions.FeatureExtractionException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Iterator;

/**
 * Tokenise text using the Illinois tokeniser,
 *
 *
 * User: Andrew D. Robertson
 * Date: 05/02/2015
 * Time: 17:25
 */
public class TokeniserIllinoisAndNER implements Tokeniser {

    private static final long serialVersionUID = 0L;

    private transient IllinoisNerExtHandler handler;

    public TokeniserIllinoisAndNER() {
        loadHandler();
    }

    @Override
    public Document tokenise(Instance document) {
        Document processed = new Document(document);

        if (!Util.isNullOrEmptyText(document)) {
            try {
                Record input = RecordGenerator.generateTokenRecord(document.text, false);
                Iterator<Span> labels = handler.performNer(input).getLabelsIterator();
                Span currentLabelSpan = labels.hasNext()? labels.next() : null;
                for (Span token : input.getLabelViews().get("tokens").getLabels()){
                    AnnotatedToken annotatedToken = new AnnotatedToken(document.text.substring(token.start, token.ending));
                    annotatedToken.start(token.start);
                    annotatedToken.end(token.ending);
                    if (currentLabelSpan != null && token.start >= currentLabelSpan.start && token.ending <= currentLabelSpan.ending){
                        String prefix = token.start == currentLabelSpan.start? "B-" : "I-";
                        annotatedToken.put("NERTag", prefix+currentLabelSpan.getLabel());
                        if (token.ending == currentLabelSpan.ending)
                            currentLabelSpan = labels.hasNext()? labels.next() : null;

                    } else {
                        annotatedToken.put("NERTag", "O");
                    }
                    processed.add(annotatedToken);
                }
            } catch (AnnotationFailedException | TException e) {
                throw new FeatureExtractionException( "NER tagging and tokenising failed...", e);
            }
        }
        return processed;
    }

    @Override
    public String configuration() {
        return "";
    }

    private void loadHandler() {
        try {
//            handler = new IllinoisNerExtHandler("/Volumes/LocalDataHD/adr27/Downloads/illinois-ner/config/ontonotes.config");
            handler = new IllinoisNerExtHandler("ontonotes.config");
        } catch (Exception e){
            throw new ConfigurationException("Failed to load Illinois tagger", e);
        }
    }

    private void readObject(ObjectInputStream in) throws Exception {
        in.defaultReadObject();
        loadHandler();
    }
}
