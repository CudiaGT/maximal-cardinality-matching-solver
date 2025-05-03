package final_project

import org.apache.spark.sql.SparkSession
import org.apache.spark.SparkContext
import org.apache.spark.SparkConf

object merge_output {
  def main(args: Array[String]): Unit = {

    if (args.length != 2) {
      println("Usage: merge_output input_folder_path output_folder_path")
      sys.exit(1)
    }

    val inputFolder = args(0)
    val outputFile = args(1)

    val conf = new SparkConf().setAppName("MergeOutputFiles")
    val sc = new SparkContext(conf)
    val spark = SparkSession.builder.config(conf).getOrCreate()

    val merged = sc.textFile(inputFolder + "/*")

    merged
      .coalesce(1)
      .saveAsTextFile(outputFile)

    println("====================================================")
    println("          Output Solution Merging Complete          ")
    println("====================================================")

    sc.stop()
  }
}
