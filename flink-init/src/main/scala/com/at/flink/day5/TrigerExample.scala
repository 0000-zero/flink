package com.at.flink.day5

import org.apache.flink.api.common.state.ValueStateDescriptor
import org.apache.flink.api.scala.typeutils.Types
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.function.ProcessWindowFunction
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.triggers.{Trigger, TriggerResult}
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

object TrigerExample {

  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment

    env.setParallelism(1)
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)


    var stream = env
      .socketTextStream("hadoop102", 9999)
      .map(line => {
        val split = line.split(" ")
        (split(0), split(1).toLong)
      })
      .assignAscendingTimestamps(_._2)
      .keyBy(_._1)
      .timeWindow(Time.seconds(10))
      .trigger(new OneSecondInternaltrigger)
      .process(new WinCount)


    stream.print()

    env.execute()
  }

  /**
   *  该触发器在整数秒和窗口结束时间时触发窗口计算
   */
  class OneSecondInternaltrigger extends Trigger[(String,Long),TimeWindow]{

    //每来一条数据就会触发一次
    override def onElement(value: (String, Long), timeStamp: Long, window: TimeWindow, context: Trigger.TriggerContext): TriggerResult = {

      /*
          在第一条数据来的时候注册两个触发器
            1.窗口结束时的触发器
            2.在整数秒时的触发器


       */

      //设置一个标志位 只在窗口的第一条数据的时候为false
      var firstSeen = context.getPartitionedState(
        new ValueStateDescriptor[Boolean]("fist-seen",Types.of[Boolean])
      )

      if(!firstSeen.value()){

        //默认的水位线为  new Watermark(this.currentTimestamp == -9223372036854775808L ? -9223372036854775808L : this.currentTimestamp - 1L);
        println("第一条数据来了！当前的水位线为getCurrentWatermark : " + context.getCurrentWatermark)
        //事件时间的下一个整数秒
        val t = context.getCurrentWatermark + (1000 - context.getCurrentWatermark % 1000)

        println("第一条数据来后，注册的触发器的整数秒时间戳为:" + t)

        println("当前窗口事件的结束时间为 window.getEnd：" + window.getEnd)

        //注册两个事件触发器
        context.registerEventTimeTimer(window.getEnd)
        context.registerEventTimeTimer(t)

        //注册完后将firstSeen 更新为true
        firstSeen.update(true)

      }
      //到来的其他数据均不处理
      TriggerResult.CONTINUE

    }

    override def onProcessingTime(l: Long, w: TimeWindow, triggerContext: Trigger.TriggerContext): TriggerResult = TriggerResult.CONTINUE


    //定时器函数在水位线到达time时触发
    override def onEventTime(tameStamp: Long, window: TimeWindow, context: Trigger.TriggerContext): TriggerResult ={

      if(tameStamp == window.getEnd){
        //时间为结束窗口定时器 触发窗口函数并销毁定时器
        TriggerResult.FIRE_AND_PURGE
      }else{
        val t = context.getCurrentWatermark + (1000 - context.getCurrentWatermark % 1000)

        //注册窗口的时间不能大于窗口的结束时间
        if(t < window.getEnd){
          println("注册的定时器的整数秒的时间戳是：" + t)
          context.registerEventTimeTimer(t)
        }

        //触发窗口计算
        println("在 " + tameStamp + " 触发了窗口计算！")
        TriggerResult.FIRE

      }

    }

    override def clear(window: TimeWindow, context: Trigger.TriggerContext): Unit ={
      var firstSeen = context.getPartitionedState(
        new ValueStateDescriptor[Boolean]("fist-seen",Types.of[Boolean])
      )

      firstSeen.clear

    }



  }


  class WinCount extends ProcessWindowFunction[(String, Long), String, String, TimeWindow] {
    override def process(key: String, context: Context, elements: Iterable[(String, Long)], out: Collector[String]): Unit = {
      out.collect("窗口中一共有" + elements.size + "条元素！窗口的结束时间为：" + context.window.getEnd)
    }
  }

}
