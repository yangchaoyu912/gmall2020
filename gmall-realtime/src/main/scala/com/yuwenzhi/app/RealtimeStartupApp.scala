package com.yuwenzhi.app

import java.text.SimpleDateFormat
import java.util.Date

import com.alibaba.fastjson.JSON
import com.yuwenzhi.bean.StartUpLog
import com.yuwenzhi.constants.GmallConstants
import com.yuwenzhi.handler.RedisFilterDataHandler
import com.yuwenzhi.utils.MykafkaUtil
import org.apache.hadoop.conf.Configuration
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.spark.streaming.dstream.{DStream, InputDStream}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.phoenix.spark._
/**
 * @description:
 * @author: 宇文智
 * @date 2020/11/4 18:56
 * @version 1.0
 */
object RealtimeStartupApp {

  def main(args: Array[String]): Unit = {
    val conf: SparkConf = new SparkConf().setMaster("local[*]").setAppName("gmall")
    val sc = new SparkContext(conf)
    //五秒一个批次的数据
    val ssc = new StreamingContext(sc,Seconds(5))
    //消费kafka中的数据
    val kafkaDStream: InputDStream[ConsumerRecord[String, String]] =
      MykafkaUtil.getKafkaStream(GmallConstants.KAFKA_TOPIC_STARTUP,ssc)
    //将每条数据封装成样例类
    val startUplogDStream: DStream[StartUpLog] = kafkaDStream.map(record => {
      val jsonInfo: String = record.value()
      val startUpLog: StartUpLog = JSON.parseObject(jsonInfo, classOf[StartUpLog])
      //json里面缺少两个字段 logDate, logHour，在样例类中添加这两个字段
      //从ts中获取
      val sdf = new SimpleDateFormat("yyyy-MM-dd HH")
      val datetimeStr: String = sdf.format(new Date(startUpLog.ts))
      val arr: Array[String] = datetimeStr.split(" ")
      startUpLog.logDate = arr(0)
      startUpLog.logHour = arr(1)
      startUpLog
    })
    //需求： 统计日活   （每个mid ,计 1 次。重复登录算 1 次）
    //1. 在redis黑名单中查询 当前批次的 mid是否已经存在，是，则过滤掉
    val filterDataDStream: DStream[StartUpLog] = RedisFilterDataHandler.filterLoggedOnUser(startUplogDStream,sc)
    
    //2. 本批次数据过滤后，留下来的那些之前没统计的mid，还存在本批次重复登录的情况，需去重
    val filterByMidDStream: DStream[StartUpLog] = RedisFilterDataHandler.filterByMid(filterDataDStream)

    //3. 存入redis，标记该mid已统计过
    RedisFilterDataHandler.saveMidToRedis(filterByMidDStream)

    //4. 将本批次中之前批次未统计到HBase的数据明细，写入HBase
    filterByMidDStream.foreachRDD(rdd=>{
      rdd.saveToPhoenix("GMALL2020_DAU",
        Seq("MID", "UID", "APPID", "AREA", "OS", "CH", "TYPE", "VS", "LOGDATE", "LOGHOUR", "TS") ,
        new Configuration,Some("hadoop102,hadoop103,hadoop104:2181"))
    })

    ssc.start()
    ssc.awaitTermination()

  }
}
