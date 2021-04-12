package com.at.flink.day2


import org.apache.flink.api.common.functions.MapFunction
import org.apache.flink.streaming.api.scala._


object MapExample{
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment

    env.setParallelism(1)


    val stream: DataStream[SensorReading] = env.addSource(new SensorSource)

    val map1: DataStream[String] = stream.map(r => r.id)

    val map2: DataStream[String] = stream
      .map(new MapFunction[SensorReading, String] {
        override def map(value: SensorReading): String = value.id
      })


    val map3:DataStream[String] = stream.map(new MyMapFunction)


//    map1.print()
//    map2.print()
    map3.print()

    env.execute()

  }

  // 输入泛型：SensorReading; 输出泛型：String;
  class MyMapFunction extends MapFunction[SensorReading,String]{
    override def map(t: SensorReading): String = t.id
  }



}
