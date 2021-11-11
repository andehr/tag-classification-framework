package uk.ac.susx.tag.classificationframework.featureextraction.filtering;

import org.junit.Assert;
import org.junit.Test;
import uk.ac.susx.tag.classificationframework.datastructures.Document;
import uk.ac.susx.tag.classificationframework.datastructures.Instance;
import uk.ac.susx.tag.classificationframework.featureextraction.tokenisation.Tokeniser;
import uk.ac.susx.tag.classificationframework.featureextraction.tokenisation.TokeniserGermanStanford;

/**
 * Created by Andrew D. Robertson on 03/09/2021.
 */
public class TokenFilterRelevanceStopwordsTest {

    @Test
    public void german(){
        TokenFilterRelevanceStopwords filter = new TokenFilterRelevanceStopwords("de");
        Instance i = new Instance("", "Der Roman steht vor dem Hintergrund einer von Tolkien sein Leben lang entwickelten Fantasiewelt ", null);
        Tokeniser tokeniser = new TokeniserGermanStanford();
        Document d = tokeniser.tokenise(i);
        boolean isStopwordDer = filter.filter(0, d);
        boolean isStopwordTolkien = filter.filter(8, d);
        Assert.assertTrue(isStopwordDer);
        Assert.assertFalse(isStopwordTolkien);
    }
}
