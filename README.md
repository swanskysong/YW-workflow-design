Workflow Design with the YesWokrlfow system
========================

Proof of concept implemetation for a workflow system with the [YesWorkflow system] (https://github.com/yesworkflow-org/yw-prototypes).

Build and Run
----------------------
Make sure you have Java installed, to check:

    $ java -version

Run the following command to build the jar file (more about [maven] (https://maven.apache.org/))

    $ mvn package 
  
The resulting jar file resides in the "target" folder, run the following command to check whether the build is successful:

    $ java -jar target/yesworkflow-0.2-SNAPSHOT-jar-with-dependencies.jar --help

Options
------------------------
Workflow can be viewed as a graph. Use the `graph` option to produce the graph specification, which can be rendered by [GraphViz] (http://www.graphviz.org/). Example:

    $ java -jar target/yesworkflow-0.2-SNAPSHOT-jar-with-dependencies.jar create ../src/main/resources/example.py > ../src/main/resources/example.gv
    $ dot dot -Tpdf ../src/main/resources/example.gv -o ../src/main/resources/example.pdf
    
Code block annotated with `@CREATE` can be filled with exisitng code blocks. To fill a target script with exisiting code snippets, use the `create` option. Example:

    $ java -jar target/yesworkflow-0.2-SNAPSHOT-jar-with-dependencies.jar create ../src/main/resources/example.py
    
Workflow analysis queries provenance information and find whether there exist some design issues. To run the analysis tool on a script, use the `analysis` option. Example:

    $ java -jar target/yesworkflow-0.2-SNAPSHOT-jar-with-dependencies.jar analysis ../src/main/resources/example.py

Analysis can be run with a specific query, specified by `-q`. Example:

    $ java -jar target/yesworkflow-0.2-SNAPSHOT-jar-with-dependencies.jar anaysis ../src/main/resources/example.py -q "example.query"
    
In some cases, the annotation may not cooresponding to the code. To run the validation module to check, use the `validate` option. Example:

    $ java -jar target/yesworkflow-0.2-SNAPSHOT-jar-with-dependencies.jar anaysis ../src/main/resources/example.py -q "example.query"
