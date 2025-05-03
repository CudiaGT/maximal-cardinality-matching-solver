error id: 
file://<WORKSPACE>/build.sbt
empty definition using pc, found symbol in pc: 
empty definition using semanticdb
empty definition using fallback
non-local guesses:
	 -Seq.
	 -Seq#
	 -Seq().
	 -scala/Predef.Seq.
	 -scala/Predef.Seq#
	 -scala/Predef.Seq().
offset: 175
uri: file://<WORKSPACE>/build.sbt
text:
```scala
name := "Final_Project"

version := "1.0"

scalaVersion := "2.12.10"

resolvers += "SparkPackages" at "https://dl.bintray.com/spark-packages/maven"

libraryDependencies ++= Se@@q(
  "org.apache.spark" %% "spark-core" % "3.5.5",
  "org.apache.spark" %% "spark-sql" % "3.5.5",
  "org.apache.spark" %% "spark-graphx" % "3.5.5"
)


```


#### Short summary: 

empty definition using pc, found symbol in pc: 