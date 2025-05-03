error id: leftOuterJoin.
file://<WORKSPACE>/src/main/scala/final_project/matching_algorithm.scala
empty definition using pc, found symbol in pc: leftOuterJoin.
empty definition using semanticdb
empty definition using fallback
non-local guesses:
	 -org/apache/spark/graphx/graph/vertices/leftOuterJoin.
	 -org/apache/spark/graphx/graph/vertices/leftOuterJoin#
	 -org/apache/spark/graphx/graph/vertices/leftOuterJoin().
	 -graph/vertices/leftOuterJoin.
	 -graph/vertices/leftOuterJoin#
	 -graph/vertices/leftOuterJoin().
	 -scala/Predef.graph.vertices.leftOuterJoin.
	 -scala/Predef.graph.vertices.leftOuterJoin#
	 -scala/Predef.graph.vertices.leftOuterJoin().
offset: 3062
uri: file://<WORKSPACE>/src/main/scala/final_project/matching_algorithm.scala
text:
```scala
package final_project

import org.apache.spark.sql.SparkSession
import org.apache.spark.SparkContext
import org.apache.spark.SparkConf
import org.apache.spark.graphx._
import org.apache.spark.storage.StorageLevel
import scala.util.Random

object israeli_itai_matching {
  def main(args: Array[String]): Unit = {

    if (args.length != 2) {
      println("Usage: israeli_itai_matching input_graph_path output_matching_path")
      sys.exit(1)
    }

    val inputPath = args(0)
    val outputPath = args(1)

    val conf = new SparkConf()
      .setAppName("IsraeliItaiMatching")
      .set("spark.driver.memory", "12g")
      .set("spark.executor.memory", "12g")
      .set("spark.sql.shuffle.partitions", "200")

    val sc = new SparkContext(conf)
    val spark = SparkSession.builder.config(conf).getOrCreate()

    def parseEdge(line: String): Edge[Int] = {
      val fields = line.split(",")
      Edge(fields(0).toLong, fields(1).toLong, 1)
    }

    val rawEdges = sc.textFile(inputPath).map(parseEdge)

    var graph = Graph.fromEdges(rawEdges, defaultValue = 1, StorageLevel.MEMORY_AND_DISK, StorageLevel.MEMORY_AND_DISK)

    var iteration = 0
    var continue = true

    val tempOutputDir = outputPath + "_temp"

    while (continue) {
      iteration += 1
      println(s"=== Starting Iteration $iteration ===")

      val activeVertices = graph.vertices.filter { case (_, status) => status == 1 }

      if (activeVertices.isEmpty()) {
        println("No active vertices remaining.")
        continue = false
      } else {

        val proposals = graph.aggregateMessages[Long](
          ctx => {
            if (ctx.srcAttr == 1 && ctx.dstAttr == 1) {
              ctx.sendToSrc(ctx.dstId)
            }
          },
          (a, b) => if (Random.nextBoolean()) a else b
        )

        val acceptances = proposals.map(_.swap).reduceByKey((a, _) => a).map(_.swap)

        val randomBits = graph.vertices.filter(_._2 == 1).mapValues(_ => Random.nextInt(2))

        val initialMatches = acceptances
          .join(randomBits)
          .map { case (u, (v, bitU)) => (v, (u, bitU)) }
          .join(randomBits)
          .filter { case (_, ((u, bitU), bitV)) => bitU == 0 && bitV == 1 }
          .map { case (v, ((u, _), _)) => (u, v) }

        // Canonicalize edges
        val canonicalMatches = initialMatches.map { case (u, v) =>
          if (u < v) (u, v) else (v, u)
        }

        // 🔥 Filter out any conflicts:
        // A vertex must not appear more than once
        val matchedVertices = canonicalMatches.flatMap { case (u, v) => Seq(u, v) }
          .map(v => (v, 1))
          .reduceByKey(_ + _)
          .filter(_._2 == 1) // Only vertices appearing once

        val validVertices = matchedVertices.map(_._1).collect().toSet
        val bcValidVertices = sc.broadcast(validVertices)

        val finalMatches = canonicalMatches.filter { case (u, v) =>
          bcValidVertices.value.contains(u) && bcValidVertices.value.contains(v)
        }.distinct()

        val updatedVertices = graph.vertices.leftOuter@@Join(
          finalMatches.flatMap { case (u, v) => Seq(u, v) }.map((_, 0))
        ).mapValues {
          case (status, Some(_)) => 0
          case (status, None) => status
        }

        graph = Graph(updatedVertices, graph.edges)

        finalMatches
          .map { case (u, v) => s"$u,$v" }
          .repartition(1)
          .saveAsTextFile(s"$tempOutputDir/iter_$iteration")

        val matchCount = finalMatches.count()
        println(s"Iteration $iteration: Found $matchCount matches.")

        if (matchCount == 0) {
          println("No matches found. Ending iterations.")
          continue = false
        }
      }
    }

    println("=== All iterations complete ===")

    val allMatches = sc.textFile(s"$tempOutputDir/iter_*")
    allMatches.saveAsTextFile(outputPath)

    println(s"✅ Matching completed and saved to $outputPath")

    sc.stop()
  }
}

```


#### Short summary: 

empty definition using pc, found symbol in pc: leftOuterJoin.