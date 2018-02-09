// input:  input/NationalFile.txt
// output: counties listed in the dataset without a hospital

package com.thiago

import org.apache.spark.SparkContext


object CountiesWithoutHospitalRDD {
  def parseLine(line : String)  : Option[(String, String)] = {
    val feat = line.split("\\|")

    if(feat.size > 5 && feat(3).size == 2)
      return Some((feat(2), feat(5) +"-" + feat(3))) // (feature, county-state)
    return None
  }

  def main(args: Array[String]): Unit = {
    val sc = new SparkContext("local[*]", "CountiesWithoutHospitalRDD")
    val inputRDD = sc.textFile(args(0))

    // (feature, county-state)
    val parsedRDD = inputRDD.flatMap(x => parseLine(x))

    // distinct county-state's - (county-state)
    val allCountiesRDD = parsedRDD.map(x => x._2).distinct()

    // all counties with hospitals - (county-state)
    val allCountiesWithHospitalsRDD = parsedRDD.filter(x => x._1 == "Hospital").map(x => x._2).distinct()

    val countiesWithoutHospital = allCountiesRDD.subtract(allCountiesWithHospitalsRDD).collect()

    println("COUNTIES WITHOUT A HOSPITAL")
    countiesWithoutHospital.foreach(x => println(x))




  }
}





