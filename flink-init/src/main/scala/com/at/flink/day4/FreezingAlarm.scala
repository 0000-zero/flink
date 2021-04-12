package com.at.flink.day4

import com.at.flink.day2.{SensorReading, SensorSource}
import org.apache.flink.streaming.api.functions.ProcessFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector

object FreezingAlarm {

  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment

    env.setParallelism(1)

    val stream: DataStream[SensorReading] = env
      .addSource(new SensorSource)
      // 没有keyBy，没有开窗！
      .process(new FreesingAlarmFunction)

    // 侧输出标签的名字必须是一样的
    stream.getSideOutput(new OutputTag[String]("freezing-alarm")).print()

    env.execute()
  }

  // `ProcessFunction`处理的是没有keyBy的流
  class FreesingAlarmFunction extends ProcessFunction[SensorReading,SensorReading]{

    // 定义一个侧输出标签，实际上就是侧输出流的名字
    // 侧输出流中的元素的泛型是String
    lazy val freezingAlarmOut = new OutputTag[String]("freezing-alarm")

    override def processElement(value: SensorReading, context: ProcessFunction[SensorReading, SensorReading]#Context, collector: Collector[SensorReading]): Unit = {

      if(value.temperature < 32.0) context.output(freezingAlarmOut,s"${value.id}的传感器低温报警")

      // 将所有读数发送到常规输出
      collector.collect(value)
    }


  }

}
