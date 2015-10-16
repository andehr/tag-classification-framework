package uk.ac.susx.tag.classificationframework.featureextraction.normalisation;

import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.*;
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
        assignStemmer();
    }

    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        assignStemmer();
    }

    private final void assignStemmer() {
        switch (lang) {
            case "en":
                stemmer = new englishStemmer();
                break;
            case "da":
                stemmer = new danishStemmer();
                break;
            case "nl":
                stemmer = new dutchStemmer();
                break;
            case "fi":
                stemmer = new finnishStemmer();
                break;
            case "fr":
                stemmer = new frenchStemmer();
                break;
            case "de":
                stemmer = new germanStemmer();
                break;
            case "hu":
                stemmer = new hungarianStemmer();
                break;
            case "it":
                stemmer = new italianStemmer();
                break;
            case "no":
                stemmer = new norwegianStemmer();
                break;
            case "pt":
                stemmer = new portugueseStemmer();
                break;
            case "ro":
                stemmer = new romanianStemmer();
                break;
            case "ru":
                stemmer = new russianStemmer();
                break;
            case "es":
                stemmer = new spanishStemmer();
                break;
            case "sv":
                stemmer = new swedishStemmer();
                break;
            case "tr":
                stemmer = new turkishStemmer();
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
