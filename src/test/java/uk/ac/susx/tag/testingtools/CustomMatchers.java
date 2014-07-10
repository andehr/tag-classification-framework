package uk.ac.susx.tag.testingtools;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Matchers are used by the JUnit "assertThat" function, to assert properties about objects.
 *
 * The following are some custom ones.
 *
 * User: Andrew D. Robertson
 * Date: 09/07/2014
 * Time: 12:14
 */
public class CustomMatchers {

    public static <T extends Collection> TypeSafeMatcher<T> isEmptyCollection(){
        return new TypeSafeMatcher<T>() {
            @Override
            public boolean matchesSafely(T t) {
                return t.isEmpty();
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("empty collection");
            }
        };
    }

    public static <T extends Map> TypeSafeMatcher<T> isEmptyMap(){
        return new TypeSafeMatcher<T>() {
            @Override
            public boolean matchesSafely(T t) {
                return t.isEmpty();
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("empty map");
            }
        };
    }

    public static <T extends Set> TypeSafeMatcher<T> contains(final Object element){
        return new TypeSafeMatcher<T>() {
            @Override
            public boolean matchesSafely(T t) {
                return t.contains(element);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("set containing: ");
                description.appendValue(element);
                description.appendText(" Type: " + element.getClass());
            }
        };
    }
}
