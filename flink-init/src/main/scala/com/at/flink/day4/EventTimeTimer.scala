package com.at.flink.day4

import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector

object EventTimeTimer {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment

    env.setParallelism(1)
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)


    env
      .socketTextStream("hadoop102",9999,'\n')
      .map(
        line => {
          val arr = line.split(" ")
          (arr(0),arr(1).toLong*1000)
        }
      )
      .assignAscendingTimestamps(_._2)
      .keyBy(_._1)
      .process(new Keyed)
      .print


    env.execute()

  }

  class Keyed extends KeyedProcessFunction[String,(String,Long),String]{
    override def processElement(i: (String, Long), context: KeyedProcessFunction[String, (String, Long), String]#Context, collector: Collector[String]): Unit ={
      //注册一个定时器：事件携带的时间加上10s
      context.timerService().registerEventTimeTimer(i._2+10*1000L)
    }

    override def onTimer(timestamp: Long, ctx: KeyedProcessFunction[String, (String, Long), String]#OnTimerContext, out: Collector[String]): Unit = {
      out.collect("定时器触发了！"+"定时器执行的时间戳是："+timestamp)
    }

  }




}
