package uk.ac.susx.tag.classificationframework.featureextraction.normalisation;

import uk.ac.susx.tag.classificationframework.datastructures.AnnotatedToken;
import uk.ac.susx.tag.classificationframework.datastructures.Document;

import java.util.regex.Pattern;

/**
 * Strip leading and trailing punctuation from each token.
 *
 * Created by Andrew D. Robertson on 06/12/16.
 */
public class TokenNormaliserLeadingTrailingPunctuation extends TokenNormaliser{

    private static final long serialVersionUID = 0L;

    public static final Pattern leadingPunctuation = Pattern.compile("^\\p{Punct}+");
    public static final Pattern leadingPunctuationExcludingTwitterTags = Pattern.compile("^[\\p{Punct}&&[^#@]]\\p{Punct}*");  // "Any single punctuation character that is not @/#, followed by 0 or more other punctuation of any kind."
    public static final Pattern trailingPunctuation = Pattern.compile("\\p{Punct}+$");

    private boolean excludeTwitterTags;

    public TokenNormaliserLeadingTrailingPunctuation(){
        this(false);
    }

    public TokenNormaliserLeadingTrailingPunctuation(boolean excludeTwitterTags){
        this.excludeTwitterTags = excludeTwitterTags;
    }

    @Override
    public boolean normalise(int index, Document tokens) {
        AnnotatedToken token = tokens.get(index);

        String form = token.get("form");
        String leadingStripped = excludeTwitterTags?
                                        leadingPunctuationExcludingTwitterTags.matcher(form).replaceFirst("") :
                                        leadingPunctuation.matcher(form).replaceFirst("");
        String trailingStripped = trailingPunctuation.matcher(leadingStripped).replaceFirst("");

        token.put("form", trailingStripped);
        return true;
    }

    @Override
    public boolean isThreadSafe() {
        return true;
    }
}
