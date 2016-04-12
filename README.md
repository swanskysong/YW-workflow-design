Workflow Design with the YesWokrlfow system
========================

Proof of concept implemetation for a workflow system with the [YesWorkflow system] (https://github.com/yesworkflow-org/yw-prototypes).

Build and Run
----------------------

Run the following command to build the jar file

    mvn package 
  
The resulting jar file resides in the "target" folder, run the following command to check whether the build is successful:

    java -jar target/yesworkflow-0.2-SNAPSHOT-jar-with-dependencies.jar --help
  
To create a script with existing code snippets, use the create option. Example:

    java -jar target/yesworkflow-0.2-SNAPSHOT-jar-with-dependencies.jar create ../src/main/resources/example.py
    
To run the analysis tool on a script, use the analysis option. Example:

    java -jar target/yesworkflow-0.2-SNAPSHOT-jar-with-dependencies.jar analysis ../src/main/resources/example.py

Analysis can be run with a specific query, specified by "-q". Example:

    java -jar target/yesworkflow-0.2-SNAPSHOT-jar-with-dependencies.jar anaysis ../src/main/resources/example.py -q "example.query"
    
To run the validation module to check whether the YW annoation is cooresponding to the code, use the "validate" option. Example:

    java -jar target/yesworkflow-0.2-SNAPSHOT-jar-with-dependencies.jar anaysis ../src/main/resources/example.py -q "example.query"
