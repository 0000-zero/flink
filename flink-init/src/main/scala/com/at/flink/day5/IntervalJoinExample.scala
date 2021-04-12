package com.at.flink.day5

import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.co.ProcessJoinFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.util.Collector

object IntervalJoinExample {

  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment

    env.setParallelism(1)
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)


    // 点击流
    val clickStream = env
      .fromElements(
        ("1", "click", 3600 * 1000L)
      )
      .assignAscendingTimestamps(_._3)
      .keyBy(_._1)

    // 浏览流
    val browseStream = env
      .fromElements(
        ("1", "browse", 2000 * 1000L),
        ("1", "browse", 3100 * 1000L),
        ("1", "browse", 3200 * 1000L),
        ("1", "browse", 4000 * 1000L),
        ("1", "browse", 7200 * 1000L)
      )
      .assignAscendingTimestamps(_._3)
      .keyBy(_._1)

    clickStream
      .intervalJoin(browseStream)
      //3600 =>  3000 ~ 4100
      .between(Time.seconds(-600), Time.seconds(500))
      .process(new MyIntervalJoin)
      .print()


    env.execute()
  }

  //public abstract class ProcessJoinFunction<IN1, IN2, OUT>
  class MyIntervalJoin extends ProcessJoinFunction[(String,String,Long),(String,String,Long),String]{
    override def processElement(in1: (String, String, Long), in2: (String, String, Long), context: ProcessJoinFunction[(String, String, Long), (String, String, Long), String]#Context, collector: Collector[String]): Unit ={
      collector.collect(in1 + " ==> " + in2)
    }
  }

}
