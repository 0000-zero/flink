package com.at.flink.day4

import org.apache.flink.api.common.state.ValueStateDescriptor
import org.apache.flink.api.scala.typeutils.Types
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.function.ProcessWindowFunction
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

object UpdateWindowResultWithLateElement {


  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment

    env.setParallelism(1)
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)


   val stream =  env
      .socketTextStream("hadoop102", 9999)
      .map(line => {
        val arr = line.split(" ")
        (arr(0), arr(1).toLong * 1000L)
      })
      .assignTimestampsAndWatermarks(
        //设置最大延迟时间5s
        new BoundedOutOfOrdernessTimestampExtractor[(String, Long)](Time.seconds(5)) {
          override def extractTimestamp(t: (String, Long)): Long = t._2
        }
      )
      .keyBy(_._1)
      //设置窗口的大小5s
      .timeWindow(Time.seconds(5))
      // 窗口闭合以后，等待迟到元素的时间也是5s
      .allowedLateness(Time.seconds(5))
      .process(new UpdateWinRes)

    //当水位线没过了窗口结束时间+allowed lateness时间时，窗口会被删除，并且所有后来的迟到的元素都会被丢弃

    stream.print()

    env.execute()
  }

  class UpdateWinRes extends ProcessWindowFunction[(String, Long), String, String, TimeWindow] {

    override def process(key: String, context: Context, elements: Iterable[(String, Long)], out: Collector[String]): Unit = {

      // 当第一次对窗口进行求值时，也就是水位线超过窗口结束时间的时候
      // 会第一次调用process函数
      // 这是isUpdate为默认值false
      // 窗口内初始化一个状态变量使用windowState，只对当前窗口可见
      val isUpdate = context.windowState.getState(
        new ValueStateDescriptor[Boolean]("update", Types.of[Boolean])
      )

      if (!isUpdate.value()) {
        // 当水位线超过窗口结束时间时，第一次调用
        out.collect("窗口第一次进行求值！元素的数量一共有：" + elements.size)
        isUpdate.update(true)
      } else {
        out.collect("迟到元素到来了！更新的元素数量为 " + elements.size + " 个！")
      }

    }

  }

}
