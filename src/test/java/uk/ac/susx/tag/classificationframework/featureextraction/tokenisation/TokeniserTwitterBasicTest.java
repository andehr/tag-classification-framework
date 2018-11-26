package uk.ac.susx.tag.classificationframework.featureextraction.tokenisation;

import org.junit.Assert;
import org.junit.Test;
import uk.ac.susx.tag.classificationframework.datastructures.Document;
import uk.ac.susx.tag.classificationframework.datastructures.Instance;

import java.io.IOException;

/**
 * Created by simon on 28/09/16.
 */
public class TokeniserTwitterBasicTest {



    @Test
    public void test1() {

        boolean filterPunctuation = true;
        boolean lowerCase = true;
        boolean normaliseURLs = true;

        TokeniserTwitterBasic tokeniser = new TokeniserTwitterBasic(filterPunctuation? null : "[!?\"#$%&'()*+,-./:;<=>@\\[\\]^_`{|}~]+", lowerCase, normaliseURLs);

        Instance input = new Instance(null, "This, is, a, normal. sentence! `with numbers numb3rs 4umber5 4014365 .", "1");

        Document output = tokeniser.tokenise(input);


        Assert.assertEquals(output.get(0).get("form"), "this");
        Assert.assertEquals(output.get(1).get("form"), "is");
        Assert.assertEquals(output.get(2).get("form"), "a");
        Assert.assertEquals(output.get(3).get("form"), "normal");
        Assert.assertEquals(output.get(4).get("form"), "sentence");
        Assert.assertEquals(output.get(5).get("form"), "with");
        Assert.assertEquals(output.get(6).get("form"), "numbers");
        Assert.assertEquals(output.get(7).get("form"), "numb3rs");
        Assert.assertEquals(output.get(8).get("form"), "4umber5");
        Assert.assertEquals(output.get(9).get("form"), "4014365");

    }



    @Test
    public void testCMUTokeniseOnlySpan() {
        boolean filterPunctuation = true;
        boolean lowerCase = true;
        boolean normaliseURLs = true;

        TokeniserTwitterBasic tokeniser = new TokeniserTwitterBasic(filterPunctuation? null : "[!?\"#$%&'()*+,-./:;<=>@\\[\\]^_`{|}~]+", lowerCase, normaliseURLs);

        Instance input = new Instance(null, "This, is, a, normal. sentence! `with numbers numb3rs 4umber5 4014365 .", "1");

        Document output = tokeniser.tokenise(input);

        Assert.assertEquals(61, output.get(9).start());
        Assert.assertEquals(68, output.get(9).end());
    }

    @Test
    public void testCMUTokeniseAndTagOnlySpan() {
        TokeniserCMUTokenOnly tokeniser = new TokeniserCMUTokenOnly();

        Instance input = new Instance(null, "This, is, a, normal. sentence! `with numbers numb3rs 4umber5 4014365 .", "1");

        Document output = tokeniser.tokenise(input);

        Assert.assertEquals(61, output.get(14).start());
        Assert.assertEquals(68, output.get(14).end());
    }

    @Test
    public void testCMUTwitterSpan() throws IOException {
        TokeniserCMUTokenAndTag tokeniser = new TokeniserCMUTokenAndTag();

        Instance input = new Instance(null, "This, is, a, normal. sentence! `with numbers numb3rs 4umber5 4014365 .", "1");

        Document output = tokeniser.tokenise(input);

        Assert.assertEquals(61, output.get(14).start());
        Assert.assertEquals(68, output.get(14).end());
    }

    //this test takes ages to load and need lots of memory and nobody uses illinois - trust me it passes :)
    //@Test
    public void testIllinoisSpan() {
        TokeniserIllinoisAndNER tokeniser = new TokeniserIllinoisAndNER();

        Instance input = new Instance(null, "This, is, a, normal. sentence! `with numbers numb3rs 4umber5 4014365 .", "1");

        Document output = tokeniser.tokenise(input);

        Assert.assertEquals(61, output.get(14).start());
        Assert.assertEquals(68, output.get(14).end());
    }
}
