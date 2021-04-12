package com.at.flink.day4

import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.ProcessFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector

object RedirectLateEventCustom {

  def main(args: Array[String]): Unit = {

    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment

    env.setParallelism(1)
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)


    val stream = env
      .socketTextStream("hadoop102", 9999)
      .map(line => {
        val arr = line.split(" ")
        (arr(0), arr(1).toLong * 1000L)
      })
      .assignAscendingTimestamps(_._2)
      .process(new LateEventpro)


    stream.print()
    stream
      .getSideOutput(new OutputTag[String]("late"))
      .print()

    env.execute()


  }


  //public abstract class ProcessFunction<I, O>
  class LateEventpro extends ProcessFunction[(String,Long),(String,Long)]{

    val late = new OutputTag[String]("late")

    override def processElement(i: (String, Long), context: ProcessFunction[(String, Long), (String, Long)]#Context, collector: Collector[(String, Long)]): Unit = {
      // 如果到来的元素所包含的时间戳小于当前数据流的水位线，即为迟到元素
      if(i._2 < context.timerService().currentWatermark()){
        // 将迟到元素发送到侧输出流中去
        context.output(late,"迟到事件来了！！")
      }else{
        collector.collect(i)
      }
    }




  }

}
