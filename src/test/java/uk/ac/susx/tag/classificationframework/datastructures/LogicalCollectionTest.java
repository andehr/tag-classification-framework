package uk.ac.susx.tag.classificationframework.datastructures;

import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.Iterator;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


/**
 * Unit tests for the LogicalCollection class.
 *
 * User: Andrew D. Robertson
 * Date: 08/07/2014
 * Time: 15:18
 */
public class LogicalCollectionTest {

    /**
     * Test that the logical collection skips empty lists gracefully while iterating over elements in the original
     * order.
     */
    @Test
    public void iteratesInOriginalOrderAndSkipsEmpties() {
        List<Integer> a = Lists.newArrayList(0, 1, 2, 3, 4, 5);
        List<Integer> b = Lists.newArrayList();
        List<Integer> c = Lists.newArrayList(6, 7);
        List<Integer> d = Lists.newArrayList();
        List<Integer> e = Lists.newArrayList(8, 9);
        LogicalCollection<Integer> l = new LogicalCollection<>(a, b, c, d, e);

        int index = 0;
        for (Iterator<Integer> iterator = l.iterator(); iterator.hasNext(); index++) {
            int i = iterator.next();
            assertThat(i, is(index));
        }
    }

    /**
     * Test that building a collection via the constructors and the individual add methods produce identical results.
     */
    @Test
    public void constructorsEquateToAddMethod(){
        List<Integer> a = Lists.newArrayList(0, 1, 2, 3, 4, 5);
        List<Integer> b = Lists.newArrayList(6, 7);
        List<Integer> c = Lists.newArrayList(8, 9);
        List<Integer> d = Lists.newArrayList(10, 11);
        List<Integer> e = Lists.newArrayList(12, 13);

        LogicalCollection<Integer> c1 = new LogicalCollection<>(a, b, c, d, e);
        LogicalCollection<Integer> c2 = new LogicalCollection<>(a).add(b).add(c).add(d).add(e);

        for(Iterator<Integer> i1 = c1.iterator(), i2 = c2.iterator(); i1.hasNext(); ){
            assertThat(i1.next(), is(i2.next()));
        }
    }
}
