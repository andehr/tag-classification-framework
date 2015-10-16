package uk.ac.susx.tag.classificationframework.featureextraction.normalisation;

import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;
import uk.ac.susx.tag.classificationframework.datastructures.AnnotatedToken;
import uk.ac.susx.tag.classificationframework.datastructures.Document;

import java.io.IOException;

/**
 * User: Simon Wibberley <sw206@sussex.ac.uk>
 * Date: 07/10/2015
 * Time: 13:32
 */
public class TokenNormaliserStemmer extends TokenNormaliser {

    private static final long serialVersionUID = 0L;

    private transient SnowballStemmer stemmer;

    private final String lang;

    public TokenNormaliserStemmer(String lang) {
        this.lang = lang;
        switch (lang) {
            case "en":
                stemmer = new englishStemmer();
                break;
            default:
                stemmer = new englishStemmer();
                break;
        }
    }

    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        switch (lang) {
            case "en":
                stemmer = new englishStemmer();
                break;
            default:
                stemmer = new englishStemmer();
                break;
        }
    }

    @Override
    public synchronized boolean normalise(int index, Document tokens) {
        AnnotatedToken token = tokens.get(index);

        String form = token.get("form");

        stemmer.setCurrent(form);
        stemmer.stem();

        String stemmer = this.stemmer.getCurrent();

        token.put("form", stemmer);

        return false;
    }
}
