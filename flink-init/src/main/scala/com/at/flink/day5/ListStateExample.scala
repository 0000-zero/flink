package com.at.flink.day5

import com.at.flink.day2.{SensorReading, SensorSource}
import org.apache.flink.api.common.state.{ListStateDescriptor, ValueStateDescriptor}
import org.apache.flink.api.scala.typeutils.Types
import org.apache.flink.runtime.state.StateBackend
import org.apache.flink.runtime.state.filesystem.FsStateBackend
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

object ListStateExample {

  def main(args: Array[String]): Unit = {

    val env = StreamExecutionEnvironment.getExecutionEnvironment

    //D:\workspace\workspace2021\bigdata\flink\flink-init\src\main\resources\cp
    env.enableCheckpointing(1000L)
    env.setStateBackend(new FsStateBackend("file:///D:\\workspace\\workspace2021\\bigdata\\flink\\flink-init\\cp").asInstanceOf[StateBackend])

    env.setParallelism(1)

    env
      .addSource(new SensorSource)
      .filter(_.id.equals("sensor_1"))
      .keyBy(_.id)
      .process(new Keyed)
      .print()

    env.execute()
  }

  //public abstract class KeyedProcessFunction<K, I, O>
  class Keyed extends KeyedProcessFunction[String,SensorReading,String]{

    lazy val listState = getRuntimeContext.getListState(
      new ListStateDescriptor[SensorReading]("list-state",Types.of[SensorReading])
    )

    lazy val timer = getRuntimeContext.getState(
      new ValueStateDescriptor[Long]("timer",Types.of[Long])
    )


    override def processElement(i: SensorReading, context: KeyedProcessFunction[String, SensorReading, String]#Context, collector: Collector[String]): Unit = {

      listState.add(i)
      if(timer.value() == 0L){
        val ts = context.timerService().currentProcessingTime() + 5 * 1000L
        context.timerService().registerProcessingTimeTimer(ts)
        timer.update(ts)
      }
    }

    override def onTimer(timestamp: Long, ctx: KeyedProcessFunction[String, SensorReading, String]#OnTimerContext, out: Collector[String]): Unit ={
      // 不能直接统计状态变量列表

      val readings: ListBuffer[SensorReading] = new ListBuffer()

      // 隐式类型转换必须导入//
      for (r <- listState.get) {
        readings += r
      }
      out.collect("当前时刻列表状态变量里面共有 " + readings.size + "条数据！")
      timer.clear()




    }




  }


}
