package uk.ac.susx.tag.classificationframework.datastructures;

import org.junit.Test;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Testing the StringIndexer
 *
 * User: Andrew D. Robertson
 * Date: 09/07/2014
 * Time: 13:06
 */
public class StringIndexerTest {

    /**
     * Check that indexes begin at 0 and count up properly. Check that the values can be acquired from these indices.
     */
    @Test
    public void indexing() {
        StringIndexer stringIndexer = new StringIndexer();

        for(int i = 0; i < 20; i++) {
            assertThat(stringIndexer.getIndex("test"+i), is(i));
        }

        for(int i = 0; i < 20; i++) {
            assertThat(stringIndexer.getValue(i), is("test"+i));
        }
    }

    /**
     * Test serialisation and deserialisation.
     */
    @Test
    public void serialisation() throws IOException, ClassNotFoundException {

        File tempfile = File.createTempFile("serialisationTest", null);
        tempfile.deleteOnExit();

        StringIndexer stringIndexer = new StringIndexer();

        stringIndexer.getIndex("test1");
        stringIndexer.getIndex("test2");
        stringIndexer.getIndex("test3");
        stringIndexer.getIndex("test4");
        stringIndexer.getIndex("test5");

        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(tempfile))){
            out.writeObject(stringIndexer);
        }

        StringIndexer deserialised;

        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(tempfile))){
            deserialised = (StringIndexer)in.readObject();
        }

        if(!tempfile.delete()) throw new RuntimeException("Couldn't delete temp file: " + tempfile.getAbsolutePath());

        assertThat(deserialised.getIndex("test1", false), is(stringIndexer.getIndex("test1", false)));
        assertThat(deserialised.getIndex("test2", false), is(stringIndexer.getIndex("test2", false)));
        assertThat(deserialised.getIndex("test3", false), is(stringIndexer.getIndex("test3", false)));
        assertThat(deserialised.getIndex("test4", false), is(stringIndexer.getIndex("test4", false)));
        assertThat(deserialised.getIndex("test5", false), is(stringIndexer.getIndex("test5", false)));
    }

}
