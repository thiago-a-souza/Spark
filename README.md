# Author
Thiago Alexandre Domingues de Souza

# Apache Spark
Apache Spark is a high performance cluster computing platform originally developed at UC Berkeley AMPLab in 2009. The researchers realized that MapReduce programs were inefficient for iterative algorithms, which reuse intermediate results between computations, because it saves operations in disk instead of memory. As result of that, they proposed a structure called Resilient Distributed Dataset (RDD) to perform in-memory computations on large clusters [(1)](#references).

Each RDD represents a collection of immutable objects, which are split into multiple partitions that may be distributed and processed among different cluster nodes. RDDs are usually created from a provided dataset or another RDD. After that, there are two operations on RDDs: transformations and actions [(2)](#references).

- **[transformations](#transformations):** lazy operations (nothing is executed until an action operation is performed) that return a new RDD from an existing RDD. Transformations include: *map, flatMap, filter, distinct, sample, union, intersection, subtract, cartesian,* etc.
- **[actions](#actions):** operations have immediate execution and returns data. Actions include: *collect, reduce, first, count, countByValue, take, top,* etc.

Before diving further into Spark actual operations, it's important to understand how they work. Transformations dependencies  between RDDs can be divided into two categories: narrow and wide, as illustrated in Figure 1. In Scala, wide transformations return a *ShuffleDependency* for the *dependency* property.

- **Narrow dependencies:** each partition of the parent RDD is used by at most one partition of the child RDD. No shuffling is needed for these operations, making them faster. Functions: *map, flatMap, union, sample,* etc.
- **Wide dependencies:** child partitions may come from multiple parent partitions. Shuffling the data over the network make them slower. Functions: *reduceByKey, intersection, groupByKey, cartesian,* etc.

<p align="center">
<img src="https://github.com/thiago-a-souza/Spark/blob/master/img/spark_dependencies.png"  height="40%" width="40%"> <br>
Figure 1: RDD dependencies <a href="https://github.com/thiago-a-souza/Spark/blob/master/README.md#references">(1)</a> </p> 
</p>


When a Spark program finds an action it creates a Directed Acyclic Graph (DAG) of stages,  optimizing the execution flow and determining tasks that each executor should run - that way it can recover from node failures. Each stage contains as many narrow dependencies as possible to reduce shuffling, shown in Figure 2. 

<p align="center">
<img src="https://github.com/thiago-a-souza/Spark/blob/master/img/spark_stages.png"  height="40%" width="40%"> <br>
Figure 2: Spark stages <a href="https://github.com/thiago-a-souza/Spark/blob/master/README.md#references">(1)</a> </p> 
</p>

The execution of a Spark application is illustrated in Figure 3. When a *SparkContext* is executed, the cluster manager (e.g. YARN, Mesos, etc) starts the executors on the work nodes of the cluster. Each node has one or more executors. After that, when it finds an action, it creates a job consisting of stages. Stages are divided into tasks, which are the smallest units in the Spark hierarchy, and run on a single executor. Resources allocation (i.e. number of executors, cores and memory per executor) can be configured at the application level.


<p align="center">
<img src="https://github.com/thiago-a-souza/Spark/blob/master/img/spark_execution.png"  height="40%" width="40%"> <br>
Figure 3: Spark execution <a href="https://github.com/thiago-a-souza/Spark/blob/master/README.md#references">(3)</a> </p> 
</p>



## Creating RDDs
Apache Spark was written in Scala, but it also supports Java, Python and R. Scala for Spark has been largely adopted as result of its high performance and simplicity, reducing boiler-plate code often found in Java.

Creating an RDD requires a reference to *SparkContext*. This object represents the front door to Spark, allowing your application to communicate with your cluster manager (e.g. standalone, YARN, Mesos, etc). Spark properties can be hard-coded for each application using a *SparkConf* object that's passed to the *SparkContext*. Alternatively, *spark-submit* accepts runtime  parameters and also reads configurations from *spark-defaults.conf*. However, configurations specified in *SparkConf* take a higher precedence [(4)](#references).

Hard-coded *SparkContext*  in Scala using the number of threads corresponding to the number of machine cores:

```scala
val config = new SparkConf().setAppName("YourAppName").setMaster("local[*]")
val sc = new SparkContext(config);
// or alternatively:
val sc = new SparkContext("local[*]", "YourAppName")
```

*SparkContext* provided at runtime:
```
./bin/spark-submit --name "YourAppName" --master local[*] --class yourClassHere yourJarHere.jar
``` 


Once a *SparkContext* is available, there are three alternatives to create an RDD: from an external dataset, using the *parallelize* method or from an existing RDD.

```scala
val rdd1 = sc.textFile("file:///your/path/here/your_file.txt")
val rdd2 = sc.parallelize(List(1, 2, 3, 4, 5, 6))
val rdd3 = rdd2.filter(_ % 2 == 0) 
```

### Spark shell
Spark provides an interactive environment based on Scala REPL (Read Eval-Print-Loop) called *spark-shell*. This environment  makes it easier to test small programs because the code is interpreted immediately, so the program doesn't need to be compiled and then submitted to Spark to return a result. From *spark-shell*, users can access *SparkContext* as *sc* and SparkSession as *spark*.

Counting words example:

```sh
$ ./bin/spark-shell 
scala> val lines = sc.parallelize(List("orange mango apple banana", "mango papaya mango"))
scala> val words = lines.flatMap(x => x.split(" "))
scala> val freq = words.map(x => (x, 1)).reduceByKey((x,y) => x + y).collect()
scala> for (word <- freq)
            println(word._1+ " = " + word._2)
```

## Transformations 

* **map:** applies a function to every element in the RDD and returns a new RDD with the same number of elements
```scala
scala> val a = sc.parallelize(List(1, 2, 3, 4, 5))
scala> a.map(x => x + 1)
scala> a.collect()
res1: Array[Int] = Array(2, 3, 4, 5, 6)
```

* **flatMap:** similar to *map*, but each input can be mapped to zero or more output items, so it may return a different number of elements - useful to discard or add data. 
```scala
scala> val a = List("aaa bbb ccc", "ddd", "eee")
scala> a.map(x => x.split(" "))
res2: List[Array[String]] = List(Array(aaa, bbb, ccc), Array(ddd), Array(eee))

scala> a.flatMap(x => x.split(" "))
res3: List[String] = List(aaa, bbb, ccc, ddd, eee)
```


* **filter:** returns a new RDD containing only elements that satisfy the conditional logic provided
```scala
scala> val nbrs = sc.parallelize(1 to 10)
scala> val even = nbrs.filter(_ % 2 == 0)
scala> even.collect()
res4: Array[Int] = Array(2, 4, 6, 8, 10)
```

* **distinct:** returns a new RDD with distinct elements
```scala
scala> val a = sc.parallelize(List(1, 2, 2, 3, 1, 4))
scala> val b = a.distinct()
scala> b.collect()
res5: Array[Int] = Array(4, 2, 1, 3)                                           
```

* **union, intersection, subtract, cartesian:** returns a new RDD after applying the specified set operation
```scala
scala> val a = sc.parallelize(List(1, 2, 3))
scala> val b = sc.parallelize(List(3, 4, 5))

scala> val c = a.union(b)
scala> c.collect()
res6: Array[Int] = Array(1, 2, 3, 3, 4, 5)

scala> val d = a.intersection(b)
scala> d.collect()
res7: Array[Int] = Array(3)

scala> val e = a.subtract(b)
scala> e.collect()
res8: Array[Int] = Array(2, 1)

scala> val f = a.cartesian(b)
scala> f.collect()
res9: Array[(Int, Int)] = Array((1,3), (1,4), (1,5), (2,3), (3,3), (2,4), (2,5), (3,4), (3,5))
```


### Transformations on Pair RDDs
Similarly to MapReduce model, a key/value pair is a common data structure used to perform data aggregations in Spark. Evidently, transformations on pairs require a *(key, value)* pair format.  In case multiple values are provided, they should be formatted as *(key, (value1, value2, ..., valueN))*, preserving the pair format. Some functions on pair RDDs are described below, and a complete list can be found in the *PairRDDFunctions* of the Spark Scala API (5).

* **reduceByKey:** merge values for each key using an associative and commutative reduce function. In practice, the reduce function is applied to each pair of elements with the same key and the result is applied to the next element with the same key, visiting all elements with the same key.

```scala
scala> val gradesPerId = sc.parallelize( List((1, 10), (2, 7), (1, 8), (2, 9)))
scala> val sumGradesPerId = gradesPerId.reduceByKey((x,y) => x+y)
scala> sumGradesPerId.collect()
res10: Array[(Int, Int)] = Array((2,16), (1,18))

scala> val anotherRDD = sc.parallelize(List((1,(10, 100)), (2,(7, 70)), (1,(8, 80)), (2,(9, 90))))
scala> val sumRDD = anotherRDD.reduceByKey((x,y) => (x._1 + y._1, x._2 + y._2))
scala> sumRDD.collect()
res11: Array[(Int, (Int, Int))] = Array((2,(16,160)), (1,(18,180)))
```

* **groupByKey:** group values with the same key. This is a time-consuming function and should be avoided. It shuffles and then aggregates the data - *reduceByKey* is faster because it aggregates and then shuffle the data. Other alternatives to prefer over *groupByKey*: *combineByKey* and *foldByKey*.


* **mapValues:** applies the provided function to the RDD values, preserving the keys.
```scala
scala> val a = sc.parallelize(List((1, 10), (2, 7), (1, 8), (2, 9)))
scala> val b = a.mapValues(x => x + 1)
scala> b.collect()
res12: Array[(Int, Int)] = Array((1,11), (2,8), (1,9), (2,10))

scala> val c = sc.parallelize(List((1, (10, 100)), (2, (7, 70)), (1, (8, 80)),(2, (9, 90))))
scala> val d = c.mapValues(x => (x._1 + 1, x._2 + 10))
scala> d.collect()
res13: Array[(Int, (Int, Int))] = Array((1,(11,110)), (2,(8,80)), (1,(9,90)), (2,(10,100)))
```

* **keys:** returns an RDD with the keys from a given RDD
```scala
scala> val a = sc.parallelize(List((1,10),(2,7),(5,8)))
scala> val b = a.keys
scala> b.collect()
res14: Array[Int] = Array(1, 2, 5)
```

* **values:** returns an RDD with the values from a given RDD
```scala
scala> val a = sc.parallelize(List((1,10),(2,7),(5,8)))
scala> val b = a.values
scala> b.collect
res15: Array[Int] = Array(10, 7, 8)
```

* **sortByKey:** returns an RDD sorted by key
```scala
scala> val a = sc.parallelize(List((5, 2), (3, 7), (1, 8), (2, 9)))
scala> val b = a.sortByKey()
scala> b.collect()
res16: Array[(Int, Int)] = Array((1,8), (2,9), (3,7), (5,2))
```

* **join:** returns an RDD from an inner join between two RDDs
```scala
scala> val a = sc.parallelize(List((5, 2), (3, 7), (1, 8), (2, 9)))
scala> val b = sc.parallelize(List((3,9)))
scala> val c = a.join(b)
scala> c.collect()
res17: Array[(Int, (Int, Int))] = Array((3,(7,9)))
```

* **leftOuterJoin:** returns an RDD from a left outer inner join between two RDDs
```scala
scala> val a = sc.parallelize(List((1,2), (3,4)))
scala> val b = sc.parallelize(List((3,9)))
scala> val c = a.leftOuterJoin(b)
scala> c.collect()
res18: Array[(Int, (Int, Option[Int]))] = Array((1,(2,None)), (3,(4,Some(9))))
```


## Actions
* **reduce:** unlike *reduceByKey*, the *reduce* function is an action, so it returns a value. It reduces the elements of the RDD using the provided commutative and associative function. **Remark:** this is not similar to the MapReduce function, which receives the data aggregated and sorted.

```scala
scala> val a = sc.parallelize(List(1, 2, 3))
scala> a.reduce((x,y) => x + y)
res19: Int = 6

scala> a.reduce((x,y) => Math.min(x, y))
res20: Int = 1
```

* **collect:** returns an array with all elements from the RDD
```scala
scala> val a = sc.parallelize(List(1, 2, 3, 3))
scala> a.collect
res21: Array[Int] = Array(1, 2, 3, 3)
```

* **count:** returns the number of items in the RDD
```scala
scala> val a = sc.parallelize(List(1, 2, 3, 3))
scala> a.count
res22: Long = 4
```

* **countByValue:** returns a map of *(value, count)* from the RDD
```scala
scala> val a = sc.parallelize(Array(1,2,2,3,3,3,3))
scala> a.countByValue
res23: scala.collection.Map[Int,Long] = Map(2 -> 2, 1 -> 1, 3 -> 4)
```

* **take:** returns an array with the first N elements from the RDD
```scala
scala> val a = sc.parallelize(List(3, 1, 5, 7))
scala> a.take(2)
res24: Array[Int] = Array(3, 1)
```

* **top:** returns the top k values from the RDD
```scala
scala> val b = sc.parallelize(List(6, 9, 4, 7, 5, 8))
scala> b.top(2)
res25: Array[Int] = Array(9, 8)
```



## Persistence
Spark recomputes an RDD and its dependencies every time an action is executed. This behavior can impact iterative algorithms, which reuse the same data multiple times. Preventing this effect can be achieved using persistence functions, that can save the RDD in memory, disk or both. Levels available: *MEMORY_ONLY* (default), *MEMORY_ONLY_SER*, *MEMORY_AND_DISK*, *MEMORY_AND_DISK_SER* and *DISK_ONLY*.

```scala
scala> rdd.persist(org.apache.spark.storage.StorageLevel.MEMORY_AND_DISK)
scala> result.unpersist()
```

# Spark SQL
RDDs were created to support a wide variety of data formats using flexible APIs to control them. However, by knowing the data format in advance allows Spark to perform additional optimizations. As result of that, Spark introduced a component called Spark SQL [(6)](#references), enabling programmers to work with structured data using SQL or HQL (Hive Query Language). This component organizes the data into Dataframes/Datasets, which is similar to a database table. Dataframes/Datasets store the data more efficiently in binary format (a.k.a. Project Tungsten), rather than expensive Java serializations. It also takes advantage of optimized execution plans (a.k.a. Catalyst Optimizer) to get a better performance than RDDs. 


Similarly to RDDs, Dataframes/Datasets represent a collection of distributed immutable objects, containing additional data format information not present in RDDs. Dataframes, introduced in Spark 1.3, and Datasets added in version 1.6, had their APIs unified in Spark 2.0. The difference between them is that Dataframes don't have a specific type, whereas Datasets use strongly typed objects. **In practice, a DataFrame is an alias to Dataset[Row].**

## Creating a SparkSession
A *SparkSession* object is the entry point for Spark SQL to work with Dataframes/Datasets. As mentioned before, *spark-shell* provides a *SparkSession* object as *spark*.

```scala
val spark = SparkSession
  .builder()
  .master("local[*]")
  .appName("YourAppName")
  .getOrCreate()
```

## Creating Dataframes
Dataframes have a schema information about the columns stored - column names, data types and if nulls are allowed. Spark has three alternatives to provide a schema to a Dataframe: inferring from metadata (using information from Scala case classes, JSON or JDBC), inferring from data (if data doesn't provide a schema) and specified programmatically.

Spark SQL supports a wide variety of data sources (e.g. CSV, JSON, JDBC, Parquet, etc). As result of that, there are several options to create Dataframes. The most common solution is creating an RDD with a Scala case class and then convert to a Dataframe using the function *toDF* or *createDataFrame*. However, Scala case classes are limited to 22 fields. In that case,  Dataframes can be specified programmatically using the *createDataFrame* function.



- **toDF:** converting RDD to *DataFrame*
```scala
scala> import spark.implicits._
scala> case class Person ( name : String,  age : Int )
scala> val df = sc.parallelize(List(Person("john", 30), Person("peter", 17))).toDF()
df: org.apache.spark.sql.DataFrame = [name: string, age: int]

scala> val rdd = sc.textFile("file:///path/to/file.txt")
scala> val df = rdd.map(x => x.split(",")).map(x => Person(x(0), x(1).toInt)).toDF()
df: org.apache.spark.sql.DataFrame = [name: string, age: int]
```

- **createDataFrame:**
```scala
scala> val inputRDD = sc.textFile("file:///path/to/file.txt")
scala> val personRDD = inputRDD.map(x => x.split(",")).map(x => Person(x(0), x(1).toInt))
scala> val df = spark.createDataFrame(rdd)
df: org.apache.spark.sql.DataFrame = [name: string, age: int]

// specifying the schema programmatically
scala> val schema = StructType(List(StructField("Name", StringType, true), StructField("Age", IntegerType, true)))
scala> val rowRDD = inputRDD.map(x => x.split(",")).map(x => Row(x(0), x(1).toInt))
scala> val df = spark.createDataFrame(rowRDD, schema)
df: org.apache.spark.sql.DataFrame = [Name: string, Age: int]
```

- **CSV:** if the header is available in the input file it can be enabled to capture the column names. Unless the *inferSchema* is enabled, the default column type is *String*. In addition to that, the schema can be provided programmatically.

```scala
scala> val df = spark.read.option("header", "true").csv("/path/to/file.csv")
df: org.apache.spark.sql.DataFrame = [name: string, age: string]

scala> val df = spark.read.option("header", "true").option("inferSchema", "true").csv("/path/to/file.csv")
df: org.apache.spark.sql.DataFrame = [name: string, age: int]

// specifying the schema programmatically
scala> val schema = StructType(List(StructField("Name", StringType, true), StructField("Age", IntegerType, true)))
scala> val df = spark.read.schema(schema).csv("file:///path/to/file.txt")
df: org.apache.spark.sql.DataFrame = [Name: string, Age: int]

```

- **JSON:** infers data types from metadata

```scala
scala> val df = spark.read.json("file:///path/to/file.json")
df: org.apache.spark.sql.DataFrame = [age: bigint, name: string]
```

- **JDBC:** infers data types from metadata
```scala
scala> val df = spark.read.jdbc(url, table, properties)
df: org.apache.spark.sql.DataFrame = [name: string, age: int]
```



## Creating Datasets
Datasets can be created from Dataframes using the function *as[T]*, where *T* is either a case class or a tuple, or the function *toDS()* after importing the *implicits* package. In addition to that, it's also possible to create datasets from collections using the function *createDataset*.

```scala
scala> case class Person ( name : String,  age : Int )
scala> val ds = spark.read.option("header", "true").option("inferSchema", "true").csv("file:///path/to/file.csv").as[Person]
ds: org.apache.spark.sql.Dataset[Person] = [name: string, age: int]

scala> import spark.implicits._
scala> val rdd = sc.parallelize(List(("john", 30),("peter", 17)))
scala> val ds = rdd.toDS()    
ds: org.apache.spark.sql.Dataset[(String, Int)] = [_1: string, _2: int]

scala> val ds = spark.createDataset(List(17, 30, 25))
ds: org.apache.spark.sql.Dataset[Int] = [value: int]

scala> val ds = spark.createDataset(List(("john", 30),("peter", 17)))
ds: org.apache.spark.sql.Dataset[(String, Int)] = [_1: string, _2: int]
```

## Working with Dataframes
Most Dataframe operations take a Column or a *String* to refer to some attribute. There are three alternatives to work with columns:

**1. Using $-notation (requires importing implicits)**
```
scala> import spark.implicits._
scala> empDF.filter($"age" > 20).show()
+----+---+
|name|age|
+----+---+
|john| 30|
+----+---+
```

**2. Using the Dataframe**
```
scala> empDF.filter(df("age") > 20).show()
+----+---+
|name|age|
+----+---+
|john| 30|
+----+---+
```

**3. Using SQL query string**
```
scala> empDF.filter("age > 20").show()
+----+---+
|name|age|
+----+---+
|john| 30|
+----+---+

```


### Dataframe transformations
*Dataframe/Dataset* transformations are also lazily evaluated, but return a *Dataframe* instead of an RDD.


* **select:** returns a new *Dataframe* with the columns provided
```
scala> empDF.select("name", "country").show(2)
+---------+-------+
|     name|country|
+---------+-------+
|     john|    usa|
|francisco| brazil|
+---------+-------+
```

* **filter:** returns a new *Dataframe* with rows that passed the test condition
```
scala> empDF.filter($"country" === "brazil").show()
+---------+-------+---+------+
|     name|country|age|salary|
+---------+-------+---+------+
|francisco| brazil| 32|  2000|
|    pedro| brazil| 32|  1000|
+---------+-------+---+------+
```

* **distinct:** returns a new *Dataframe* with distinct rows
```
scala> empDF.select("country").distinct().show()
+-------+
|country|
+-------+
| brazil|
|    usa|
+-------+
```

* **groupBy:** groups the *Dataframe* using the columns provided
```
scala> empDF.groupBy("country").count().orderBy($"count" desc).show()
+-------+-----+                                                                 
|country|count|
+-------+-----+
|    usa|    3|
| brazil|    2|
+-------+-----+

scala> empDF.groupBy("country").sum("salary").orderBy("sum(salary)").show()
+-------+-----------+                                                           
|country|sum(salary)|
+-------+-----------+
| brazil|       3000|
|    usa|       6500|
+-------+-----------+
```

* **union, intersect:** returns a new *Dataframe* after applying the set operation
```
scala>  empDF.union(anotherDF)
scala>  empDF.intersect(anotherDF)
```

* **joins:** RDD provides functions for each join type (e.g. *join, leftOuterJoin, rightOuterJoin,* etc), but Dataframe provides a single function *join* and take a string parameter to specify the join type. Available join types: *inner, cross, outer, full, full_outer, left, left_outer, right, right_outer, left_semi and left_anti*.
```
scala> val empDF = spark.read.option("header", "true").option("inferSchema", "true").csv("file:///path/to/employee.csv")
scala> val deptDF = spark.read.option("header", "true").option("inferSchema", "true").csv(""file:///path/to/departments.csv")

scala> empDF.join(deptDF, empDF("deptId") === deptDF("deptId")).select("name", "deptName").show()
+---------+---------+
|     name| deptName|
+---------+---------+
|     john|       IT|
|francisco|       HR|
|   george|Marketing|
|    pedro|       HR|
+---------+---------+

scala> empDF.join(deptDF, empDF("deptId") === deptDF("deptId"), "left_outer").select("name", "deptName").show()
+---------+---------+
|     name| deptName|
+---------+---------+
|     john|       IT|
|francisco|       HR|
|   george|Marketing|
|    james|     null|
|    pedro|       HR|
+---------+---------+
```

### Dataframe actions

* **printSchema:** displays the *Dataframe* schema in a tree format
```
scala> empDF.printSchema
root
 |-- name: string (nullable = true)
 |-- country: string (nullable = true)
 |-- age: integer (nullable = true)
 |-- salary: integer (nullable = true)
 |-- deptId: integer (nullable = true)
 ```
 
 * **show:** unless specified, displays the top 20 *Dataframe* rows in a tabular format
 ```
scala> empDF.show()
+---------+-------+---+------+------+
|     name|country|age|salary|deptId|
+---------+-------+---+------+------+
|     john|    usa| 30|  1000|     1|
|francisco| brazil| 32|  2000|     2|
|   george|    usa| 45|  1500|     3|
|    james|    usa| 45|  4000|  null|
|    pedro| brazil| 32|  1000|     2|
+---------+-------+---+------+------+
```

 * **take:** returns an *Array* with the first N elements in the *Dataframe*
 ```scala
 scala> empDF.take(2)
res26: Array[org.apache.spark.sql.Row] = Array([john,usa,30,1000,1], [francisco,brazil,32,2000,2])
```
 
 * **count:** returns the number of elements in the *Dataframe*
```scala
scala> empDF.count()
res27: Long = 5
```
 * **collect:**  returns an *Array* with all elements in the *Dataframe*
 ```scala
 scala> empDF.collect()
res28: Array[org.apache.spark.sql.Row] = Array([john,usa,30,1000,1], [francisco,brazil,32,2000,2], [george,usa,45,1500,3], [james,usa,45,4000,null], [pedro,brazil,32,1000,2])
```


### Running SQLs
Spark SQL allows creating queries against a temporary view and returns a new *Dataframe*, making it easier to create complex queries. Temporary views can be assigned to the current session, using the function *createOrReplaceTempView*, or shared among all sessions while the application is active, using the function *createGlobalTempView*.

```
scala> empDF.createOrReplaceTempView("employees")
scala> deptDF.createOrReplaceTempView("departments")
scala> spark.sql("select name, age from employees")
res29: org.apache.spark.sql.DataFrame = [name: string, age: int]
scala> spark.sql("select * from departments")
res30: org.apache.spark.sql.DataFrame = [deptId: int, deptName: string]

scala> spark.sql("select name, age from employees").show()
+---------+---+
|     name|age|
+---------+---+
|     john| 30|
|francisco| 32|
|   george| 45|
|    james| 45|
|    pedro| 32|
+---------+---+
scala> spark.sql("select country, count(*) from employees group by country").show()
+-------+--------+                                                              
|country|count(1)|
+-------+--------+
| brazil|       2|
|    usa|       3|
+-------+--------+
scala> spark.sql("select a.name, b.deptName from employees a left outer join departments b on a.deptId = b.deptId").show()
+---------+---------+
|     name| deptName|
+---------+---------+
|     john|       IT|
|francisco|       HR|
|   george|Marketing|
|    james|     null|
|    pedro|       HR|
+---------+---------+
```

### User Defined Functions (UDFs)
User Defined Functions (UDFs) allow programmers to create customized functions not available in the Spark SQL API. Otherwise it would be required to convert the *Dataframe* to an RDD and then modify the data. It's important to highlight that UDFs should avoided whenever possible as Catalyst may not optimize the function created.


```
scala> val reverseString = (input : String ) => {
           var ans = ""
           for(i <- input)
               ans = i + ans
           ans
       }
scala> spark.udf.register("reverse_str", reverseString )
scala> spark.sql("select name, reverse_str(name) from employees").show()
+---------+---------------------+
|     name|UDF:reverse_str(name)|
+---------+---------------------+
|     john|                 nhoj|
|francisco|            ocsicnarf|
|   george|               egroeg|
|    james|                semaj|
|    pedro|                ordep|
+---------+---------------------+
```





# References
(1) Zaharia, Matei, et al. Spark: Cluster computing with working sets. HotCloud 10.10-10 (2010): 95.

(2) Karau, Holden, et al. Learning spark: lightning-fast big data analysis. O'Reilly Media, Inc.", 2015.

(3) Karau, Holden and Warren, Rachel. High Performance Spark: Best Practices for Scaling and Optimizing Apache Spark. O'Reilly Media, Inc.", 2017.

(4) Spark Configuration - https://spark.apache.org/docs/latest/configuration.html

(5) Spark Scala API - https://spark.apache.org/docs/latest/api/scala/index.html

(6) Spark SQL Guide - https://spark.apache.org/docs/latest/sql-programming-guide.html
