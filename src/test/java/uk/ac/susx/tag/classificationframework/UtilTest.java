package uk.ac.susx.tag.classificationframework;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

import static org.junit.Assert.*;

public class UtilTest {



    @Test
    public void testSafeSave () throws Exception {

        Path file = Paths.get("temp-test-file");
        Files.deleteIfExists(file);
        file = Files.createFile(file);

        boolean thrown = false;

        try {
            new Util.SafeSave().add(file.toFile(), (File f) ->{
                throw new IOException();
            }).save();

        } catch (IOException e) {

            thrown = true;
        }

        assertTrue(thrown);

        boolean originalExists = Files.exists(file);

        assertTrue(originalExists);

        Files.deleteIfExists(file);
    }

}