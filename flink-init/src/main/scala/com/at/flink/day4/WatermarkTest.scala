package com.at.flink.day4

import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector

object WatermarkTest {

  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment

    //设置事件时间语义
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.setParallelism(1)

    val stream1: DataStream[(String, Long)] = env
      .socketTextStream("hadoop102", 9999, '\n')
      .map(line => {
        val arr = line.split(" ")
        (arr(0), arr(1).toLong * 1000)
      })
      .assignAscendingTimestamps(_._2)


    val stream2: DataStream[(String, Long)] = env
      .socketTextStream("hadoop102", 9998, '\n')
      .map(line => {
        val arr = line.split(" ")
        (arr(0), arr(1).toLong * 1000)
      })
      .assignAscendingTimestamps(_._2)


    stream1
      .union(stream2)
      .keyBy(_._1)
      .process(new Keyed)
      .print()





    env.execute()

  }

  //public abstract class KeyedProcessFunction<K, I, O>
  class Keyed extends KeyedProcessFunction[String,(String,Long),String]{
    // 每到一条数据就会调用一次
    override def processElement(i: (String, Long), context: KeyedProcessFunction[String, (String, Long), String]#Context, collector: Collector[String]): Unit = {
      //输出当前的水位线
      collector.collect("当前的水位线为："+context.timerService().currentWatermark())
    }
  }

}
