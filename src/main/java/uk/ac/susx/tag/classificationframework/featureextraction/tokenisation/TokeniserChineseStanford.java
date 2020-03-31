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

            // need to handle unexpected surrogate characters. 
            Annotation annotation = new Annotation(document.text.replaceAll("[^\u0000-\uffff]", ""));
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
