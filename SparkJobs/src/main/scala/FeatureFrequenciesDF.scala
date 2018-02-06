// input:  input/NationalFile.txt
// output: frequency of features listed in the datasource

package com.thiago

import org.apache.spark.sql.SparkSession
import org.apache.spark.{SparkConf, SparkContext}

object FeatureFrequenciesDF {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder().master("local[*]").appName("FeatureFrequenciesDF").getOrCreate()
    import spark.implicits._

    val df = spark.read.textFile(args(0)).flatMap(x => parseLine(x)).toDF
    val result = df.groupBy("feature").count().filter($"count" > 100 && $"count" < 5000).orderBy($"count").collect()

    println("FEATURE, FREQUENCY")
    result.foreach(x => println( x(0) + ", " + x(1)))

    println("")

    // running SQL
    df.createOrReplaceTempView("BGN")
    val result2 = spark.sql("SELECT FEATURE, COUNT(*) FROM BGN GROUP BY FEATURE HAVING COUNT(*) > 5000 ORDER BY 2").collect()
    result2.foreach(x => println( x(0) + ", " + x(1)))

    spark.stop()
  }

  def parseLine(line : String)  : Option[BGN] = {
    val feat = line.split("\\|")
    if(feat.size > 2)
      return Some(new BGN(feat(2)))
    return None
  }

  case class BGN(feature : String)
}






