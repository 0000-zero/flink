package com.at.flink.day4

import com.at.flink.day2.{SensorReading, SensorSource}
import org.apache.flink.api.common.state.ValueStateDescriptor
import org.apache.flink.api.scala.typeutils.Types
import org.apache.flink.streaming.api.functions.co.CoProcessFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector

object CoProcessFunctionExample {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment

    env.setParallelism(1)

    val stream: DataStream[SensorReading] = env.addSource(new SensorSource)

    val switch = env.fromElements(
      ("sensor_2", 10 * 1000L),
      ("sensor_3", 5 * 1000L)
    )


    val res: DataStream[SensorReading] = stream
      .connect(switch)
      .keyBy(_.id, _._1)
      .process(new ReadingFile)

    res.print()


    env.execute()

  }

  //public abstract class CoProcessFunction<IN1, IN2, OUT>
  class ReadingFile extends CoProcessFunction[SensorReading, (String, Long), SensorReading] {

    // 初始值是false
    // 每一个key都有对应的状态变量
    lazy val forwardingEnabled = getRuntimeContext.getState(
      new ValueStateDescriptor[Boolean]("switch", Types.of[Boolean])
    )

    // 处理来自传感器的流数据
    override def processElement1(value: SensorReading, context: CoProcessFunction[SensorReading, (String, Long), SensorReading]#Context, collector: Collector[SensorReading]): Unit = {
      //如果开关是true，就允许数据流向下发送
      if (forwardingEnabled.value()) {
        collector.collect(value)
      }

    }

    // 处理来自开关流的数据
    override def processElement2(value: (String, Long), context: CoProcessFunction[SensorReading, (String, Long), SensorReading]#Context, collector: Collector[SensorReading]): Unit = {
      // 打开开关
      forwardingEnabled.update(true)
      // 开关元组的第二个值就是放行时间
      context.timerService().registerProcessingTimeTimer(context.timerService().currentProcessingTime() + value._2)

    }

    override def onTimer(timestamp: Long, ctx: CoProcessFunction[SensorReading, (String, Long), SensorReading]#OnTimerContext, out: Collector[SensorReading]): Unit = {
      // 关闭开关
      forwardingEnabled.clear()


    }


  }

}
