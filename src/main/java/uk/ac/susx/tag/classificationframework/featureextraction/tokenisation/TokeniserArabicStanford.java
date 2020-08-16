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

public class TokeniserArabicStanford implements Tokeniser{
    private static final long serialVersionUID = 0L;
    private transient StanfordCoreNLP pipeline;

    public TokeniserArabicStanford() throws IOException {
        loadPipeline();
    }

    @Override
    public Document tokenise(Instance document) {

        Document tokenised = new Document(document);
        if (!Util.isNullOrEmptyText(document)) {
            int end = 0;

            // need to handle unexpected surrogate characters.
            /* Ahmed Younes:
            The previous comment was the original comment
            My addition to this is that I am not sure if Arabic has similar issues as chinese but there are some issues i faced before regarding this
            such as RTL code point \u200f some times it comes up when you tokenise it comes attached to the word or the character
            also some times when you read Arabic file the beginning  of that file starts with \ufeff which is called the byte order mark BOM
            i have decided to leave this line of code as i understand it is a range and i think the range contains both \u200f and
            \ufeff but you might want to confirm that for me or remove it if it is extra */
            /*Ahmed Younes: very important note it turns out that the affects the stop words removal some stop words include special characters which
            * are removed during tokenisation and removing these characters prevent the removal of stop words */
            Annotation annotation = new Annotation(document.text.replaceAll("[^\u0000-\uffff]", ""));
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
        Properties props = StringUtils.argsToProperties("-props", "StanfordCoreNLP-arabic.properties");
        props.setProperty("annotators", "tokenize");
        pipeline = new StanfordCoreNLP(props);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        loadPipeline();
    }
}
