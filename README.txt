This project is best used as a library within another maven java project. Install this project to your maven repo then you're away!

Download the source, and remove this line from the pom.xml:

<parent>
  <version>1.0.0</version>
  <groupId>uk.ac.susx.tag</groupId>
  <artifactId>tag-dist</artifactId>
  <relativePath>../tag-dist</relativePath>
</parent>

Now in the project directory run the following command:

mvn install

For this to work, you must have already installed the tag-dependency-parser project (which is also on the same github page).