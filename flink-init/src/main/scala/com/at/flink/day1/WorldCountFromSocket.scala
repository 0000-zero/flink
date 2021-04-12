package com.at.flink.day1

import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time

object WorldCountFromSocket {

  case class WorldWidthCount(world: String, count: Int)

  def main(args: Array[String]): Unit = {

    //获取运行时环境
    val ev: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    //设置分区(并行数量)
    ev.setParallelism(1)

    //建立数据源
    //hadoop102 'nc -lk 9999'
    val textStream: DataStream[String] = ev.socketTextStream("hadoop102", 9999)


    val tr = textStream
      //使用空格切分
      .flatMap(line => line.split("\\s"))
      //map
      .map(w => WorldWidthCount(w, 1))
      //使用world字段分组
      .keyBy(0)
      //开了一个5s的窗口
      .timeWindow(Time.seconds(5))
      //对count字段累加
      .sum(1)


    tr.print()

    ev.execute()

  }

}
