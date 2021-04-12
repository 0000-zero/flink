package com.at.flink.day2

import org.apache.flink.streaming.api.functions.co.CoMapFunction
import org.apache.flink.streaming.api.scala._

object CoMapExample {
  def main(args: Array[String]): Unit = {

    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment

    env.setParallelism(1)


    val stream1: DataStream[(String,Int)] = env.fromElements(
      ("lisi", 120),
      ("laowang", 130)
    )

    val stream2: DataStream[(String,Int)] = env.fromElements(
      ("lisi", 29),
      ("laowang", 27)
    )


    val connected: ConnectedStreams[(String, Int), (String, Int)] = stream1
      .keyBy(_._1)
      .connect(stream2.keyBy(_._1))


    connected.map(new MyCoMapFunction).print()


    env.execute()
  }

  class MyCoMapFunction extends CoMapFunction[(String,Int),(String,Int),String]{

    // map1处理来自第一条流的元素
    override def map1(value: (String, Int)): String = {
      value._1 + "的体重是：" + value._2 + "斤"
    }

    // map2处理来自第二条流的元素
    override def map2(value: (String, Int)): String = {
      value._1 + "的年龄是：" + value._2 + "岁"
    }

  }

}
