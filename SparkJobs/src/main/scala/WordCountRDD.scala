// input:  any text file
// output: frequency of words in the file

package com.thiago

import org.apache.spark.{SparkConf, SparkContext}

object WordCountRDD {
  def main(args: Array[String]): Unit = {

    val sc = new SparkContext("local[*]", "WordCountRDD")
    val lines = sc.textFile(args(0))
    val words = lines.flatMap(x => x.split("\\W+")).map(x => x.toLowerCase())
    val freq = words.map(x => (x, 1)).reduceByKey((x,y) => x + y).collect()
    for( word <- freq)
      println(word._1+ " => " + word._2)

    sc.stop()
  }
}





