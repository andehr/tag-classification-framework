package uk.ac.susx.tag.testingtools;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: Andrew D. Robertson
 * Date: 09/07/2014
 * Time: 12:14
 */
public class CustomMatchers {

    public static <T extends Collection> Matcher<T> isEmpty(T value){
        return new BaseMatcher<T>() {
            @Override
            public boolean matches(Object o) {
                Collection c = (Collection)o;
                return c.isEmpty();
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("is empty");
            }
        };
    }
}
