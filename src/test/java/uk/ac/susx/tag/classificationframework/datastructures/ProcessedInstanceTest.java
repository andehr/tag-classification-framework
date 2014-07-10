package uk.ac.susx.tag.classificationframework.datastructures;

import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import org.junit.Test;

import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Testing the ProcessedInstance
 *
 * User: Andrew D. Robertson
 * Date: 09/07/2014
 * Time: 11:34
 */
public class ProcessedInstanceTest {

    /**
     * Test that the hashing of a ProcessedInstance is according to the ID field of its source Instance.
     */
    @Test
    public void hashByID() {

        ProcessedInstance i = new ProcessedInstance(0, new int[] {1, 2, 3},
                                    new Instance("LABEL", "TEXT", "123"));

        // Hash instance in a set.
        Set<ProcessedInstance> instances = Sets.newHashSet(i);

        // Ensure that an ProcessedInstance in which only the ID is different is still hashed differently.
        ProcessedInstance differentID = new ProcessedInstance(0, new int[]{1,2,3}, new Instance("LABEL","TEXT","456"));
        assertThat(instances.contains(differentID), is(false));

        // Ensure that a completely different ProcessedInstance with the same ID is hashed the same.
        ProcessedInstance sameID = new ProcessedInstance(1, new int[]{5,6,7}, new Instance("DIFFERENT_LABEL","DIFFERENT_TEXT","123"));
        assertThat(instances.contains(sameID), is(true));
    }

    /**
     * Test the ProcessedInstance's ability to reset its labelling
     */
    @Test
    public void resetLabelling() {

        ProcessedInstance i = getExampleProcessedInstance();

        // Set a probabilistic labelling
        Int2DoubleOpenHashMap labelling = new Int2DoubleOpenHashMap();
        labelling.put(0, 0.8);
        labelling.put(1, 0.2);
        i.setLabeling(labelling);

        // Rest the labelling
        i.resetLabeling();

        // Ensure the labelling is properly rest
        assertThat(i.getLabelProbabilities().isEmpty(), is(true));
        assertThat(i.getLabel(), is(-1));

    }

    /**
     * Test that after a probabilistic labelling is assigned, the getLabel() function returns the most likely label
     */
    @Test
    public void mostProbableLabel() {
        ProcessedInstance i = getExampleProcessedInstance();

        Int2DoubleOpenHashMap labelling = new Int2DoubleOpenHashMap();
        labelling.put(7, 0.6);
        labelling.put(100, 0.1);
        labelling.put(8, 0.2);
        labelling.put(9, 0.1);

        i.setLabeling(labelling);

        // Ensure that getLabel() returns the highest probability label.
        assertThat(i.getLabel(), is(7));
    }


    private static ProcessedInstance getExampleProcessedInstance() {
        return new ProcessedInstance(0, new int[] {1, 2, 3}, new Instance("LABEL", "TEXT", "123"));
    }
}
