To install dependencies:
mvn dependency:copy-dependencies "-DoutputDirectory=./lib"

To compile:
javac -classpath ".:lib/*" JCloudsNova.java

To run:
java -classpath ".:lib/*" JCloudsNova
