package uk.ac.susx.tag.classificationframework.featureextraction.documentprocessing;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import uk.ac.susx.tag.classificationframework.datastructures.AnnotatedToken;
import uk.ac.susx.tag.classificationframework.datastructures.Document;

import java.io.ObjectInputStream;
import java.util.List;
import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 * User: Andrew D. Robertson
 * Date: 10/02/2015
 * Time: 16:32
 */
public class StanfordNERPipeline extends DocProcessor {

    private transient StanfordCoreNLP pipeline;

    public StanfordNERPipeline(){
        loadPipeline();
    }

    @Override
    public Document process(Document document) {
        String originalText = document.source.text;
        if (!originalText.trim().isEmpty()) {
            document.clear(); // Overwrite any current tokenisation
            Annotation toBeAnnotated = new Annotation(originalText);
            pipeline.annotate(toBeAnnotated);

            List<CoreMap> sentences = toBeAnnotated.get(CoreAnnotations.SentencesAnnotation.class);
            for(CoreMap sentence: sentences) {
                for (CoreLabel token: sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                    String word = token.get(CoreAnnotations.TextAnnotation.class);
                    String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                    String ne = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);
                    int start = token.get(CoreAnnotations.BeginIndexAnnotation.class);
                    int end = token.get(CoreAnnotations.EndIndexAnnotation.class);

                    AnnotatedToken t = new AnnotatedToken(word);
                    t.start(start);
                    t.end(end);
                    t.put("pos", pos);
                    t.put("NERTag", ne);
                    document.add(t);
                }
            }
        }
        return document;
    }

    @Override
    public String configuration() {
        return "";
    }

    public void loadPipeline(){
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner");
        pipeline = new StanfordCoreNLP(props);
    }

    private void readObject(ObjectInputStream in) throws Exception {
        in.defaultReadObject();
        loadPipeline();
    }

    @Override
    public boolean isThreadSafe() {
        return true;
    }
}
