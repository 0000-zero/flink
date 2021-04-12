package com.at.flink.day3

import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.AssignerWithPeriodicWatermarks
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.function.ProcessWindowFunction
import org.apache.flink.streaming.api.watermark.Watermark
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector


object GenWatermark {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment

    env.setParallelism(1)

    // 设置时间语义为事件时间
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    // 系统默认每隔200ms插入一次水位线
    // 设置为每隔30s插入一次水位线
    env.getConfig.setAutoWatermarkInterval(30000)

    val stream: DataStream[String] = env
      .socketTextStream("hadoop102", 9999, '\n')
      .map(line => {
        val arr = line.split(" ")
        (arr(0), arr(1).toLong * 1000)
      })
      .assignTimestampsAndWatermarks(
        // 分配时间戳和水位线一定要在keyBy之前进行！
        // 水位线 = 系统观察到的元素携带的最大时间戳 - 最大延迟时间
        new MyAssigner
      )
      .keyBy(_._1)
      .timeWindow(Time.seconds(10))
      .process(new WinResult)

    stream.print()

    env.execute()

  }

  class MyAssigner extends AssignerWithPeriodicWatermarks[(String,Long)]{
    //设置最大延迟时间
    private val bound: Long = 10 * 1000L

    //系统观察到的元素包含的最时间戳
    var maxTs: Long = Long.MinValue + bound

    // 定义抽取时间戳的逻辑，每到一个事件就调用一次
    override def extractTimestamp(t: (String, Long), l: Long): Long = {
      //// 更新观察到的最大时间戳
      maxTs = maxTs.max(t._2)
      // 将抽取的时间戳返回
      t._2
    }

    // 产生水位线的逻辑
    // 默认每隔200ms调用一次
    // 我们设置了每隔1分钟调用一次
    override def getCurrentWatermark: Watermark = {
      //观察到的最大事件时间-最大延迟时间
      println("观察到的最大时间戳是：" + maxTs)
      new Watermark(maxTs - bound)
    }


  }

  class WinResult extends ProcessWindowFunction[(String,Long),String,String,TimeWindow]{

    override def process(key: String, context: Context, elements: Iterable[(String, Long)], out: Collector[String]): Unit = {
      out.collect(context.window.getStart + "~" + context.window.getEnd + " 的窗口中有 " + elements.size + " 个元素")
    }

  }

}
