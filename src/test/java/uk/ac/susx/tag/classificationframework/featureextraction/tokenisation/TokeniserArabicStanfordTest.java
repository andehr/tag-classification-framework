package uk.ac.susx.tag.classificationframework.featureextraction.tokenisation;

import org.junit.Test;
import uk.ac.susx.tag.classificationframework.datastructures.Instance;

import static org.junit.Assert.*;

/**
 * Created by ci53 on 07/12/2020.
 */
public class TokeniserArabicStanfordTest {
    @Test
    public void tokenise() throws Exception {
        Instance i = new Instance("", "هذا نص رمزي! مع البعض ، تعرف ... علامات الترقيم؟", "");
        Tokeniser tokeniser = new TokeniserArabicStanford();
        System.out.println(tokeniser.tokenise(i));
    }

}