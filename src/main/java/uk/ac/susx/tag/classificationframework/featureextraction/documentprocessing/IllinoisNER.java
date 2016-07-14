package uk.ac.susx.tag.classificationframework.featureextraction.documentprocessing;

import edu.illinois.cs.cogcomp.annotation.handler.IllinoisNerExtHandler;
import edu.illinois.cs.cogcomp.curator.RecordGenerator;
import edu.illinois.cs.cogcomp.thrift.base.AnnotationFailedException;
import edu.illinois.cs.cogcomp.thrift.base.Span;
import edu.illinois.cs.cogcomp.thrift.curator.Record;
import org.apache.thrift.TException;
import uk.ac.susx.tag.classificationframework.datastructures.Document;
import uk.ac.susx.tag.classificationframework.exceptions.ConfigurationException;
import uk.ac.susx.tag.classificationframework.exceptions.FeatureExtractionException;

import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Use this to add NER annotations to your tokens.
 *
 * This will work with any tokeniser which performs a Simple Tokenisation. Where as simple tokenisation is defined as:
 *
 * A tokenisation in which all tokens are non-overlapping, sequential substrings of the original text.
 *
 * Be aware that performance may degrade with unfamiliar tokenisation styles. This NER system was original used with
 * the Illinois tokeniser. If you wish to use both the Illinois tokeniser AND NER, it's best to just use the
 * TokeniserIllinoisAndNER component instead of this component.
 *
 * Use this component if you're using a different tokenisation scheme. The following tokenisers should be compatible:
 *
 *   - TokeniserCMUTokenAndTag  (so long as you're un-escaping HTML entities in the text before passing them through the pipeline)
 *   - TokeniserCMUTokenOnly
 *   - TokeniserTwitterBasic
 *
 * User: Andrew D. Robertson
 * Date: 06/02/2015
 * Time: 11:26
 */
public class IllinoisNER extends DocProcessor {


    private static final long serialVersionUID = 0L;

    private transient IllinoisNerExtHandler handler;

    public IllinoisNER() {
        loadHandler();
    }

    @Override
    public Document process(Document document) {
        String originalText = document.source.text;
        if (!originalText.trim().isEmpty()) {
            try {
                Record input = RecordGenerator.generateTokenRecord(originalText, false);
                Iterator<Span> labels = handler.performNer(input).getLabelsIterator();

                Span currentLabelSpan = labels.hasNext()? labels.next() : null;
                boolean atStart = true;

                List<SimpleSpan> preTokens = getSpans(document, originalText);

                for (SimpleSpan token : preTokens){

                    if (currentLabelSpan == null) { // No more NER tags if true
                        document.get(token.index).put("NERTag", "O");
                    } else {
                        if (token.start >= currentLabelSpan.start && token.ending <= currentLabelSpan.ending){
                            String prefix = atStart? "B-" : "I-";
                            atStart = false;
                            document.get(token.index).put("NERTag", prefix+currentLabelSpan.getLabel());
                        } else {
                            document.get(token.index).put("NERTag", "O");
                            if (token.start >= currentLabelSpan.ending) {
                                currentLabelSpan = labels.hasNext()? labels.next() : null;
                                atStart = true;
                            }
                        }
                    }
                }
            } catch (AnnotationFailedException | TException e) {
                throw new FeatureExtractionException( "NER tagging and tokenising failed...", e);
            }
        }
        return document;
    }

    @Override
    public String configuration() {
        return "";
    }

    private static List<SimpleSpan> getSpans(Document document, String originalText){
        List<SimpleSpan> spans = new ArrayList<>();
        int lastEnd = 0;
        for (int i = 0; i < document.size(); i++) {
            String token = document.get(i).get("form");
            int start = originalText.indexOf(token, lastEnd);
            lastEnd = start+token.length();
            spans.add(new SimpleSpan(i, start, lastEnd));
        }
        return spans;
    }

    @Override
    public boolean isThreadSafe() {
        return true;
    }

    public static class SimpleSpan {
        public int start;
        public int ending;
        public int index;

        public SimpleSpan(int index, int start, int ending) {
            this.index = index;
            this.start = start;
            this.ending = ending;
        }
    }

    private void loadHandler() {
        try {
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
