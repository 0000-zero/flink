package com.at.flink.day5

import com.at.flink.day2.{SensorReading, SensorSource}
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.function.ProcessWindowFunction
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.triggers.{Trigger, TriggerResult}
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

object ProcessingTimeTrigger {

  def main(args: Array[String]): Unit = {

    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setParallelism(1)


    val stream = env
      .addSource(new SensorSource)
      .filter(r => r.id.equals("sensor_1"))
      .keyBy(_.id)
      .timeWindow(Time.seconds(5))
      .trigger(new MyTigger)
      .process(new WindowCount)
      .print()


    env.execute()

  }

  class MyTigger extends Trigger[SensorReading,TimeWindow]{
    override def onElement(t: SensorReading, l: Long, w: TimeWindow, triggerContext: Trigger.TriggerContext): TriggerResult ={
      TriggerResult.CONTINUE
    }

    override def onProcessingTime(l: Long, w: TimeWindow, triggerContext: Trigger.TriggerContext): TriggerResult = {
      TriggerResult.CONTINUE
    }

    override def onEventTime(l: Long, w: TimeWindow, triggerContext: Trigger.TriggerContext): TriggerResult = {
      println("事件被触发")
      TriggerResult.CONTINUE
    }

    override def clear(w: TimeWindow, triggerContext: Trigger.TriggerContext): Unit = {

    }
  }


  class WindowCount extends ProcessWindowFunction[SensorReading,String,String,TimeWindow]{
    override def process(key: String, context: Context, elements: Iterable[SensorReading], out: Collector[String]): Unit = {
      out.collect("窗口中有 " + elements.size + " 条数据！窗口结束时间是" + context.window.getEnd)
    }
  }


}
