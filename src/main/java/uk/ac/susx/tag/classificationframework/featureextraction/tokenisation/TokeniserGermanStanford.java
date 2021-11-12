package uk.ac.susx.tag.classificationframework.featureextraction.tokenisation;


import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.StringUtils;
import uk.ac.susx.tag.classificationframework.Util;
import uk.ac.susx.tag.classificationframework.datastructures.AnnotatedToken;
import uk.ac.susx.tag.classificationframework.datastructures.Document;
import uk.ac.susx.tag.classificationframework.datastructures.Instance;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Properties;

public class TokeniserGermanStanford implements Tokeniser {
    private static final long serialVersionUID = 0L;
    private transient StanfordCoreNLP pipeline;

    public TokeniserGermanStanford() {
        loadPipeline();
    }

    @Override
    public Document tokenise(Instance document) {

        Document tokenised = new Document(document);

        if (!Util.isNullOrEmptyText(document)) {
            int end = 0;

            Annotation annotation = new Annotation(document.text);
            pipeline.annotate(annotation);
            // The characters get changed in this line after calling the previous method List<CoreLabel> words = (List)annotation.get(CoreAnnotations.TokensAnnotation.class);
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
        // here is that code that adds the Arabic properties and enable the stanford Arabic Tokeniser
        Properties props = StringUtils.argsToProperties("-props", "StanfordCoreNLP-german.properties");
        props.setProperty("annotators", "tokenize");
        pipeline = new StanfordCoreNLP(props);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        loadPipeline();
    }


    public static void main(String[] args) throws IOException {

        Tokeniser t = new TokeniserGermanStanford();

//        Instance instance = new Instance(null,"The brown fox jumped over the sleeping dog.", null);
        Instance instance = new Instance(null,"Der Roman steht vor dem Hintergrund einer von Tolkien sein Leben lang entwickelten Fantasiewelt.", null);

        System.out.println(t.tokenise(instance));

    }
}
