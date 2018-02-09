/*
 description: returns the average of features, similar to the query below:
 select
   feature
   ,avg(cnt)
 from (
    select
     feature
     ,state
     ,count(*) cnt
    from table
    group by feature, state
 ) where cnt > 100
 group by feature

 input: national file from US Board on Geographic Names - https://geonames.usgs.gov/domestic/download_data.htm
 output: average of features listed in the datasource

*/

package com.thiago


import org.apache.spark.sql.SparkSession
import org.apache.spark.{SparkConf, SparkContext}


object AvgFeaturesDF {


  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder().master("local[*]").appName("AvgFeaturesDF").getOrCreate()
    import spark.implicits._

    val featuresDF = spark.read.textFile(args(0)).flatMap(x => parseLine(x)).toDF

    val featuresCountDF = featuresDF.groupBy("feature", "state").count()

    featuresCountDF.filter($"count" > 100).groupBy("feature").avg("count").orderBy($"avg(count)".desc)

    // alternatively

    featuresDF.createOrReplaceTempView("BGN")
    val result = spark.sql("SELECT FEATURE, AVG(CNT) FROM " +
      "(SELECT FEATURE, STATE, COUNT(*) CNT FROM BGN GROUP BY FEATURE, STATE) " +
      "WHERE CNT > 100 " +
      "GROUP BY FEATURE " +
      "ORDER BY 2 DESC "
     ).show()

    spark.stop()
  }

  def parseLine(line : String)  : Option[BGN] = {
    val feat = line.split("\\|")
    if(feat.size > 3)
      return Some(new BGN(feat(2), feat(3)))
    return None
  }

  case class BGN(feature : String, state : String)
}



