package com.at.flink.day1

import org.apache.flink.streaming.api.scala._

object WorldCountFromBatch {


  def main(args: Array[String]): Unit = {

    val ev = StreamExecutionEnvironment.getExecutionEnvironment

    ev.setParallelism(1)



    val stream: DataStream[String] = ev.fromElements(
      "hello world",
      "hello heihei",
      "you is hello",
      "he hello you"
    )


    val tr: DataStream[(String, Int)] = stream
      .flatMap(_.split(" "))
      .map(w => (w, 1))
      .keyBy(0)
      .sum(1)


    tr.print()

    ev.execute()

  }







}
