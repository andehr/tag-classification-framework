package uk.ac.susx.tag.classificationframework.featureextraction.tokenisation;

import uk.ac.susx.tag.classificationframework.Util;
import uk.ac.susx.tag.classificationframework.datastructures.AnnotatedToken;
import uk.ac.susx.tag.classificationframework.datastructures.Document;
import uk.ac.susx.tag.classificationframework.datastructures.Instance;


import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.util.StringUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;




public class TokeniserChineseStanford implements Tokeniser {

    private static final long serialVersionUID = 0L;
    private transient StanfordCoreNLP pipeline;

    public TokeniserChineseStanford() throws IOException {
        loadPipeline();
    }

    @Override
    public Document tokenise(Instance document) {

        Document tokenised = new Document(document);
        if (!Util.isNullOrEmptyText(document)) {
            int end = 0;

            // need to remove potential chars that would break the pipeline (mainly no-break space)
            // want to replace cuz we still want to keep space in the text (first replaceAll statement)
            document.text = document.text.replaceAll("[\\s\\p{Z}]", " ").trim();
            // white-list to maintain, in order to be careful about other potential strange characters.
            document.text = document.text.replaceAll("[^\\p{L}\\p{N}\\p{Z}\\p{Sm}\\p{Sc}\\p{Sk}\\p{P}\\p{Mc}]","");
            // need to handle unexpected surrogate characters.
            document.text = document.text.replaceAll("[^\u0000-\uffff]", "");

            Annotation annotation = new Annotation(document.text);
            pipeline.annotate(annotation);
            List<CoreLabel> tokens = annotation.get(CoreAnnotations.TokensAnnotation.class);
            for (CoreLabel token : tokens) {
                String word = token.get(CoreAnnotations.TextAnnotation.class);
                int start = document.text.indexOf(word, end);
                end = start + word.length();
                AnnotatedToken annotatedToken = new AnnotatedToken(word);
                annotatedToken.start(start);
                annotatedToken.end(end);
                tokenised.add(annotatedToken);
            }
        }

        return tokenised;
    }

    @Override
    public String configuration() {
        return "";
    }

    public void loadPipeline(){
        Properties props = StringUtils.argsToProperties("-props", "StanfordCoreNLP-chinese.properties");
        props.setProperty("annotators", "tokenize");
        pipeline = new StanfordCoreNLP(props);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        loadPipeline();
    }
}
