package com.at.flink.day4

import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector

import java.sql.Timestamp

object ProcessingTimeTimer {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment

    env.setParallelism(1)
//    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    env
      .socketTextStream("hadoop102",9999)
      .map(line => {
        val arr = line.split(" ")
        (arr(0),arr(1).toLong*1000)
      })
      .keyBy(_._1)
      .process(new Keyed)
      .print()


    env.execute()

  }


  class Keyed extends KeyedProcessFunction[String,(String,Long),String]{

    override def processElement(i: (String, Long), context: KeyedProcessFunction[String, (String, Long), String]#Context, collector: Collector[String]): Unit = {
      context.timerService().registerProcessingTimeTimer(context.timerService().currentProcessingTime()+10*1000L)
    }

    override def onTimer(timestamp: Long, ctx: KeyedProcessFunction[String, (String, Long), String]#OnTimerContext, out: Collector[String]): Unit = {
      out.collect("定时器触发了！" + "定时器执行的时间戳是：" +new Timestamp(timestamp))
    }

  }

}
