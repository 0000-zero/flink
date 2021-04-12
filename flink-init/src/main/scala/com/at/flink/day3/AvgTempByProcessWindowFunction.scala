package com.at.flink.day3

import com.at.flink.day2.{SensorReading, SensorSource}
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.function.ProcessWindowFunction
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

object AvgTempByProcessWindowFunction {

  case class AvgInfo(id: String, avgTemp: Double, windowStart: Long, windowEnd: Long)

  def main(args: Array[String]): Unit = {

    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment

    env.setParallelism(1)

    val stream :DataStream[SensorReading]= env.addSource(new SensorSource)

    stream
      .keyBy(_.id)
      .timeWindow(Time.seconds(5))
      .process(new AvgFun)
      .print()

    env.execute()


  }

  // 相比于增量聚合函数，缺点是要保存窗口中的所有元素
  // 增量聚合函数只需要保存一个累加器就行了
  // 优点是：全窗口聚合函数可以访问窗口信息
  //abstract class ProcessWindowFunction[IN, OUT, KEY, W <: Window]
  class AvgFun extends ProcessWindowFunction[SensorReading,AvgInfo,String,TimeWindow]{
    /**
     * 在窗口关闭时调用
     * @param key
     * @param context
     * @param elements  保存了窗口中的所有数据信息
     * @param out
     */
    override def process(key: String, context: Context, elements: Iterable[SensorReading], out: Collector[AvgInfo]): Unit = {

      //关闭窗口时一共有多少条记录
      var count = elements.size

      var sum = 0.0
      for (elem <- elements) {
        sum+=elem.temperature
      }

      // 单位是ms
      out.collect(AvgInfo(key,sum/count,context.window.getStart,context.window.getEnd))

    }
  }


}
