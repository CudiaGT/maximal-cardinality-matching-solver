package final_project

import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.storage.StorageLevel
import scala.util.Random

object israeli_itai_matching {
  def main(args: Array[String]): Unit = {
    if (args.length != 2) {
      println("Usage: israeli_itai_matching input_graph_path output_solution_path")
      sys.exit(1)
    }

    val inputPath = args(0)
    val outputPath = args(1)

    val conf = new SparkConf()
      .setAppName("israeliItaiMatching")
      .set("spark.driver.memory", "12g")
      .set("spark.executor.memory", "12g")

    val sc = new SparkContext(conf)

    // start time
    val startTime = System.currentTimeMillis()

    def parseEdge(line: String): (Long, Long) = {
      val fields = line.split(",")
      val u = fields(0).toLong
      val v = fields(1).toLong
      if (u < v) (u, v) else (v, u)
    }

    var edges = sc.textFile(inputPath)
      .map(parseEdge)
      .persist(StorageLevel.DISK_ONLY)

    var activeVertices = edges
      .flatMap { case (u, v) => Seq(u, v) }
      .distinct()
      .persist(StorageLevel.DISK_ONLY)

    var iteration = 0
    var zeroMatchStreak = 0
    var totalGreedyIterations = 0
    val tempOutputDir = outputPath + "_temp"
    var continue = true

    while (continue) {
      iteration += 1

      // active vertices & active edges
      val activeSet = activeVertices.map((_, null)).persist(StorageLevel.DISK_ONLY)

      val activeEdges = edges
        .join(activeSet)
        .map { case (u, (v, _)) => (v, u) }
        .join(activeSet)
        .map { case (v, (u, _)) => if (u < v) (u, v) else (v, u) }
        .distinct()
        .persist(StorageLevel.DISK_ONLY)

      // exit loop if there are no more active edges
      if (activeEdges.isEmpty()) {
        continue = false
      } else {
        val vertexBits = activeVertices.map(v => (v, Random.nextInt(2))).persist(StorageLevel.DISK_ONLY)

        // identify candidate matches (proposals) through assigning random bits to each vertex
        val candidateMatches = activeEdges
          .join(vertexBits)
          .map { case (u, (v, bitU)) => (v, (u, bitU)) }
          .join(vertexBits)
          .filter { case (_, ((u, bitU), bitV)) => bitU == 0 && bitV == 1 }
          .map { case (v, ((u, _), _)) => if (u < v) (u, v) else (v, u) }
          .distinct()
          .persist(StorageLevel.DISK_ONLY)

        // if there is a mutual agreement (0 -> 1 proposal), consider them to be confirmed matches
        val confirmedMatches = candidateMatches
          .flatMap { case (u, v) => Seq((u, (u, v)), (v, (u, v))) }
          .reduceByKey { (edge1, edge2) =>
            if (edge1.hashCode() < edge2.hashCode()) edge1 else edge2 // hashing to break conflicts
          }
          .map { case (vertex, edge) => (edge, vertex) }
          .groupByKey()
          .filter { case ((u, v), voters) => voters.toSet == Set(u, v) }
          .map(_._1)
          .distinct()
          .persist(StorageLevel.DISK_ONLY)

        // number of confirmed matches
        val matchCount = confirmedMatches.count()

        // if there are 0 matches made in this iteration, increment the zero streak
        if (matchCount == 0) {
          zeroMatchStreak += 1
          println("=======================================================")
          println(s"No matches this round. Zero streak = $zeroMatchStreak.")
          println("=======================================================")
        } else {
          zeroMatchStreak = 0
        }

        // update active vertices & 
        val matchedVertices = confirmedMatches
          .flatMap { case (u, v) => Seq(u, v) }
          .distinct()
          .persist(StorageLevel.DISK_ONLY)

        matchedVertices.count()

        activeVertices = activeVertices
          .subtract(matchedVertices)
          .persist(StorageLevel.DISK_ONLY)

        activeVertices.count()

        // outputs the matched edges for logging/debugging purposes
        confirmedMatches
          .map { case (u, v) => s"$u,$v" }
          .saveAsTextFile(s"$tempOutputDir/iter_$iteration")

        // if the zeroMatchStreak has reached 3, fall back to greedy algorithm
        if (zeroMatchStreak >= 3) {
          println("Fallback to Greedy Random Matching")

          var greedyIteration = 0
          var greedyMatchCount = 1L

          while (greedyMatchCount > 0) {
            greedyIteration += 1
            totalGreedyIterations += 1

            val activeSetGreedy = activeVertices.map((_, null)).persist(StorageLevel.DISK_ONLY)

            val activeEdgesGreedy = edges
              .join(activeSetGreedy)
              .map { case (u, (v, _)) => (v, u) }
              .join(activeSetGreedy)
              .map { case (v, (u, _)) => if (u < v) (u, v) else (v, u) }
              .distinct()
              .persist(StorageLevel.DISK_ONLY)

            // similar symmetry-breaking applied
            val fallbackMatches = activeEdgesGreedy
              .flatMap { case (u, v) => Seq((u, (u, v)), (v, (u, v))) }
              .reduceByKey((e1, e2) => if (e1.hashCode() < e2.hashCode()) e1 else e2)
              .map { case (vertex, edge) => (edge, vertex) }
              .groupByKey()
              .filter { case ((u, v), voters) => voters.toSet == Set(u, v) }
              .map(_._1)
              .distinct()
              .persist(StorageLevel.DISK_ONLY)

            // matches found in the greedy algorithm iteration
            greedyMatchCount = fallbackMatches.count()

            if (greedyMatchCount > 0) {
              fallbackMatches
                .map { case (u, v) => s"$u,$v" }
                .saveAsTextFile(s"$tempOutputDir/greedy_iter_$greedyIteration")

              val fallbackMatchedVertices = fallbackMatches
                .flatMap { case (u, v) => Seq(u, v) }
                .distinct()
                .persist(StorageLevel.DISK_ONLY)

              activeVertices = activeVertices
                .subtract(fallbackMatchedVertices)
                .persist(StorageLevel.DISK_ONLY)

              activeVertices.count()
              fallbackMatchedVertices.unpersist()
            }

            fallbackMatches.unpersist()
            activeEdgesGreedy.unpersist()
            activeSetGreedy.unpersist()
          }

          continue = false
        }

        activeEdges.unpersist()
        vertexBits.unpersist()
        candidateMatches.unpersist()
        confirmedMatches.unpersist()
        matchedVertices.unpersist()
        activeSet.unpersist()
      }
    }
    
    println("=================================")
    println("       Algorithm Complete.       ")
    println("=================================")

    val allMatches = sc.textFile(tempOutputDir + "/*")
    allMatches.coalesce(1).saveAsTextFile(outputPath)

    // end time
    val endTime = System.currentTimeMillis()
    val elapsedMillis = endTime - startTime
    val seconds = (elapsedMillis / 1000) % 60
    val minutes = (elapsedMillis / (1000 * 60)) % 60
    val hours = (elapsedMillis / (1000 * 60 * 60))

    println(f"Matching completed and saved to $outputPath")
    println(f"Total iterations: $iteration")
    println(f"Total greedy fallback iterations: $totalGreedyIterations")
    println(f"Total time taken: ${hours}h ${minutes}m ${seconds}s")

    sc.stop()
  }
}