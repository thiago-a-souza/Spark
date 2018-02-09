// input:  input/NationalFile.txt
// output: counties listed in the dataset without a hospital

package com.thiago

import org.apache.spark.sql.SparkSession

object CountiesWithoutHospitalDF {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder().master("local[*]").appName("CountiesWithoutHospitalDF").getOrCreate()
    import spark.implicits._

    val featuresDF = spark.read.textFile(args(0)).flatMap(x => parseLine(x)).toDF


    featuresDF.select("COUNTYSTATE").distinct().createOrReplaceTempView("ALL_COUNTIES")
    featuresDF.select("COUNTYSTATE").filter($"FEATURE" === "Hospital").distinct().createOrReplaceTempView("HOSP_COUNTIES")

    // faster than running the outer join below
    spark.sql("SELECT COUNTYSTATE FROM ALL_COUNTIES MINUS SELECT COUNTYSTATE FROM HOSP_COUNTIES").show()

    // alternatively
    println("")
    spark.sql("SELECT A.COUNTYSTATE FROM ALL_COUNTIES "+
              "A LEFT OUTER JOIN HOSP_COUNTIES B " +
              "ON A.COUNTYSTATE = B.COUNTYSTATE " +
              "WHERE B.COUNTYSTATE IS NULL").show()


    spark.stop()
  }



  def parseLine(line : String)  : Option[BGN] = {
    val feat = line.split("\\|")

    if(feat.size > 5 && feat(3).size == 2)
      return Some(new BGN(feat(2), feat(5) +"-" + feat(3))) // (feature, county-state)
    return None
  }

  case class BGN(feature : String, countyState : String)
}



