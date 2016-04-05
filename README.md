Install
=======

This project is best used as a library within another maven java project; install this project to your maven repository, and then you're sorted.

Take the following steps to do so:


1. Download the source, and remove this section from the `pom.xml` file:

    ```
    <parent>
      <version>1.0.0</version>
      <groupId>uk.ac.susx.tag</groupId>
      <artifactId>tag-dist</artifactId>
      <relativePath>../tag-dist</relativePath>
    </parent>
    ```

2. This project requires another Taglab project; install to your Maven repo the `tag-dependency-parser` project, which is also [available under this Github profile](https://github.com/andehr/tag-dependency-parser). See that project for installation guidelines.

3. Now in the project directory run the following command:

    `mvn install`


**Illinois NER:**
If you wish to use the Illinois NER software in this framework, you cannot use it for commercial purposes, and must take responsibility for including their system yourself. To do this, in your maven project, include their repositories and dependencies, then the components in this project will function correctly. If you take no action, you will not be able to use the Illinois NER functionality (because it will not be included), and you are therefore free from its licensing constraints.

