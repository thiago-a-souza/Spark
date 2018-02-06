// input: national file from US Board on Geographic Names - https://geonames.usgs.gov/domestic/download_data.htm
// output: frequency of features listed in the datasource

package com.thiago

import org.apache.spark.{SparkConf, SparkContext}


object FeatureFrequenciesRDD {
  def parseLine(line : String)  : Option[(String, Int)] = {
  val feat = line.split("\\|")
  if(feat.size > 2)
    return Some((feat(2), 1))
  return None
  }

  def main(args: Array[String]): Unit = {
    val sc = new SparkContext("local[*]", "FeatureFrequenciesRDD")
    val inputRDD = sc.textFile(args(0))
    val featuresRDD = inputRDD.flatMap(x => parseLine(x))
    val freq = featuresRDD.reduceByKey(_+_).filter(x => x._2 > 100).sortByKey()

    val result = freq.collect()

    println("FEATURE, FREQUENCY")
    result.foreach(x => println(x._1 + ", " + x._2))

    sc.stop()
  }
}




