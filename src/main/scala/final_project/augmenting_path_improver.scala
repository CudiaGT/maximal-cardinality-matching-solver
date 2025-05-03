package final_project

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.storage.StorageLevel

object augmenting_path_improver {
  def main(args: Array[String]): Unit = {

    if (args.length != 4) {
      println("Usage: augmenting_path_improver original_edges_path current_solution_path improved_solution_path max_iterations")
      sys.exit(1)
    }

    val graphPath = args(0)
    val matchingPath = args(1)
    val outputPath = args(2)
    val maxIterations = args(3).toInt

    val conf = new SparkConf()
      .setAppName("augmentingPathImprover")
      .set("spark.driver.memory", "12g")
      .set("spark.executor.memory", "12g")

    val sc = new SparkContext(conf)

    // start time
    val startTime = System.currentTimeMillis()

    def parseEdge(line: String): (Long, Long) = {
      val parts = line.split(",")
      val u = parts(0).trim.toLong
      val v = parts(1).trim.toLong
      if (u < v) (u, v) else (v, u)
    }

    // load the original edges (for references those have not been matched)
    val allEdges = sc.textFile(graphPath)
      .map(parseEdge)
      .distinct()
      .persist(StorageLevel.DISK_ONLY)

    // load the current matched set of edges
    var currentMatching = sc.textFile(matchingPath)
      .map(parseEdge)
      .distinct()
      .persist(StorageLevel.DISK_ONLY)

    var iteration = 0
    var improvementFound = true
    var totalAugmentingPaths = 0L

    // loop continued until the input argument iterations are reached or there are no more augmenting paths of length 3
    while (iteration < maxIterations && improvementFound) {
      iteration += 1

      val matchedVertices = currentMatching.flatMap { case (u, v) => Seq(u, v) }.distinct().persist(StorageLevel.DISK_ONLY)
      val allVertices = allEdges.flatMap { case (u, v) => Seq(u, v) }.distinct().persist(StorageLevel.DISK_ONLY)
      val freeVertices = allVertices.subtract(matchedVertices).persist(StorageLevel.DISK_ONLY)

      // bottleneck for larger graphs due to .collect() and .broadcast()
      val freeSet = freeVertices.collect().toSet
      val matchedSet = matchedVertices.collect().toSet
      val freeBC = sc.broadcast(freeSet)
      val matchedBC = sc.broadcast(matchedSet)

      val residualEdges = allEdges.subtract(currentMatching).persist(StorageLevel.DISK_ONLY)

      val directedResidual = residualEdges.flatMap { case (u, v) => Seq((u, v), (v, u)) }
      val directedMatching = currentMatching.flatMap { case (u, v) => Seq((u, v), (v, u)) }

      // finding augmenting paths of length 3 (vertex order: u - m - n - w)
      val candidateEdge1 = directedResidual.filter { case (u, m) =>
        freeBC.value.contains(u) && matchedBC.value.contains(m)
      }
      val candidateArm1 = candidateEdge1.map { case (u, m) => (m, u) }

      val candidateEdge2 = directedMatching.map { case (m, n) => (m, n) }
      val partialPaths = candidateArm1.join(candidateEdge2)

      val candidateEdge3 = directedResidual.filter { case (n, w) =>
        freeBC.value.contains(w)
      }.map { case (n, w) => (n, w) }

      val candidatePaths = partialPaths.map { case (m, (u, n)) => (n, (u, m)) }
        .join(candidateEdge3)
        .map { case (n, ((u, m), w)) => (u, m, n, w) }
        .filter { case (u, m, n, w) =>
          u != m && u != n && u != w && m != n && m != w && n != w
        }

      // resolving conflicts
      // assign unique id to each augmenting path found and resolve conflicts based on these ids when one or more vertices are used in more than one augmenting path
      val indexedPaths = candidatePaths.zipWithIndex().map { case ((u, m, n, w), idx) => (idx, (u, m, n, w)) }.cache()
      val vertexToPath = indexedPaths.flatMap { case (idx, (u, m, n, w)) => Seq(u, m, n, w).map(v => (v, idx)) }
      val conflictingPaths = vertexToPath.groupByKey().flatMap { case (_, idxs) => idxs.toSeq.sorted.drop(1) }.distinct().map((_, null))
      val validPaths = indexedPaths.leftOuterJoin(conflictingPaths)
        .filter { case (_, (_, conflict)) => conflict.isEmpty }
        .map { case (_, (path, _)) => path }

      val numAugPaths = validPaths.count()
      totalAugmentingPaths += numAugPaths
      println(s"Augmenting paths found in iteration $iteration: $numAugPaths")

      // if there are augmenting paths found, replace them (i.e. delete appropriate edges and add new edges)
      if (numAugPaths == 0) {
        improvementFound = false
      } else {
        val removedEdges = validPaths.map { case (u, m, n, w) => if (m < n) (m, n) else (n, m) }
        val newEdge1 = validPaths.map { case (u, m, n, w) => if (u < m) (u, m) else (m, u) }
        val newEdge2 = validPaths.map { case (u, m, n, w) => if (n < w) (n, w) else (w, n) }
        val newEdges = newEdge1.union(newEdge2)

        val improvedMatching = currentMatching
          .subtract(removedEdges)
          .union(newEdges)
          .distinct()
          .persist(StorageLevel.DISK_ONLY)

        val oldCount = currentMatching.count()
        val newCount = improvedMatching.count()
        println(s"Matching size before augmentation: $oldCount, after augmentation: $newCount")
        improvementFound = newCount > oldCount
        currentMatching = improvedMatching
      }
    }

    // save and produce output in one file
    currentMatching
      .map { case (u, v) => s"$u,$v" }
      .coalesce(1)
      .saveAsTextFile(outputPath)

    // end time
    val endTime = System.currentTimeMillis()
    val elapsedMillis = endTime - startTime
    val seconds = (elapsedMillis / 1000) % 60
    val minutes = (elapsedMillis / (1000 * 60)) % 60
    val hours = (elapsedMillis / (1000 * 60 * 60))

    println(s"Total augmenting paths found through iterations: $totalAugmentingPaths")
    println(s"Number of Iterations: $iteration.")
    println(f"Total time taken: ${hours}h ${minutes}m ${seconds}s")

    sc.stop()
  }
}