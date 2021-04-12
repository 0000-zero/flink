package com.at.flink.day2

import org.apache.flink.streaming.api.functions.co.CoFlatMapFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector

object CoFlatMapExample {
  def main(args: Array[String]): Unit = {

    val env:StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment

    env.setParallelism(1)

    val stream1: DataStream[(String,Int)] = env.fromElements(
      ("lisi", 120),
      ("laowang", 130)
    )

    val stream2: DataStream[(String,Int)] = env.fromElements(
      ("lisi", 29),
      ("laowang", 27)
    )

    val s1: KeyedStream[(String, Int), String] = stream1
      .keyBy(_._1)

    val s2: KeyedStream[(String, Int), String] = stream2.keyBy(_._1)

    val connect: ConnectedStreams[(String, Int), (String, Int)] = s1.connect(s2)

    connect.flatMap(new MyCoFlatMapFuntion).print()

    env.execute()

  }

  class MyCoFlatMapFuntion extends CoFlatMapFunction[(String, Int), (String, Int),String]{

    override def flatMap1(value: (String, Int), out: Collector[String]): Unit = {
      out.collect(value._1 + "的体重是：" + value._2 + "斤")
      out.collect(value._1 + "的体重是：" + value._2 + "斤")
    }

    override def flatMap2(value: (String, Int), out: Collector[String]): Unit = {
      out.collect(value._1 + "的年龄是：" + value._2 + "岁")
    }

  }


}
