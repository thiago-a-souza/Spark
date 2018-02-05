// input:  any text file
// output: frequency of words in the file

package com.thiago

import org.apache.spark.sql.SparkSession

object WordCountDF {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder().master("local[*]").appName("WordCountDF").getOrCreate()
    import spark.implicits._

    val wordsDF = spark.read.textFile(args(0)).flatMap(x => x.split("\\W+")).map(x => Word(x.toLowerCase())).toDF

    wordsDF.groupBy("word").count().orderBy($"count".desc).show()

    // alternatively
    wordsDF.createOrReplaceTempView("words")
    spark.sql("SELECT WORD, COUNT(*) FROM WORDS GROUP BY WORD ORDER BY 2 DESC").show()

    spark.stop()
  }

  case class Word(word : String)
}


