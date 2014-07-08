package uk.ac.susx.tag.classificationframework.datastructures;

import com.google.common.collect.Sets;
import org.junit.Test;

import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Unit tests for the Instance class.
 *
 * User: Andrew D. Robertson
 * Date: 08/07/2014
 * Time: 15:53
 */
public class InstanceTest {

    @Test
    public void stringCreation(){
        Instance i = new Instance("LABEL", "TEXT", "12345");

        assertThat(i.toString(), is("{id=12345, label=LABEL, text=TEXT}"));
    }

    @Test
    public void hashByID(){
        Set<Instance> s = Sets.newHashSet(new Instance("LABEL1", "TEXT1", "1"));

        // Assert that an instance with all but the ID different is hashed the same
        assertThat(s.contains(new Instance("LABEL2", "TEXT2", "1")), is(true));

        // Assert that an instance with all but the ID the same is hashed differently
        assertThat(s.contains(new Instance("LABEL1", "TEXT1", "2")), is(false));

    }
}
