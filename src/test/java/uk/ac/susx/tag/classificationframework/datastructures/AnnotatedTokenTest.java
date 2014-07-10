package uk.ac.susx.tag.classificationframework.datastructures;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests for AnnotatedToken
 *
 * User: Andrew D. Robertson
 * Date: 09/07/2014
 * Time: 11:13
 */
public class AnnotatedTokenTest {

    /**
     * Test that the string form is produced correctly.
     */
    @Test
    public void stringCreation() {

        AnnotatedToken token = new AnnotatedToken(
                                    ImmutableMap.of(
                                        "form", "FORM",
                                        "pos", "POS",
                                        "deprel", "DEPREL"
                                    )
                               );

        assertThat(token.toString(), is("FORM (pos: \"POS\", deprel: \"DEPREL\")"));
    }

    /**
     * Test that the attributes used to initialise the token are stored and retrievable.
     */
    @Test
    public void attributesStoredCorrectly(){
        AnnotatedToken token = new AnnotatedToken(
                                    ImmutableMap.of(
                                        "form", "FORM",
                                        "pos", "POS",
                                        "deprel", "DEPREL"
                                    )
                               );
        assertThat(token.getWithNullFeature("form"), is("FORM"));
        assertThat(token.getWithNullFeature("pos"), is("POS"));
        assertThat(token.getWithNullFeature("deprel"), is("DEPREL"));
    }

    /**
     * Test that when a token is queried for a feature that isn't present using the "getWithNullFeature",
     * that they properly receive the null feature String.
     */
    @Test
    public void nullFeature(){
        AnnotatedToken token = new AnnotatedToken("FORM");

        assertThat(token.getWithNullFeature("deprel"), is(AnnotatedToken.nullFeature));
    }
}
