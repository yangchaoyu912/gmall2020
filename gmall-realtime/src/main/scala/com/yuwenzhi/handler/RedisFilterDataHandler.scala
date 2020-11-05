package com.yuwenzhi.handler

import java.text.SimpleDateFormat
import java.util
import java.util.Date

import com.yuwenzhi.bean.StartUpLog
import com.yuwenzhi.utils.RedisUtil
import org.apache.spark.SparkContext
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.streaming.dstream.DStream
import redis.clients.jedis.Jedis

/**
 * @description:
 * @author: 宇文智
 * @date 2020/11/4 19:37
 * @version 1.0
 */
object RedisFilterDataHandler {
  /**
   * 存入redis，标记该mid已统计过
   * @param filterByMidDStream
   */
  def saveMidToRedis(filterByMidDStream: DStream[StartUpLog]): Unit = {
      filterByMidDStream.foreachRDD(
        rdd =>{
          rdd.foreachPartition(iter =>{
            val client: Jedis = RedisUtil.getJedisClient
            iter.foreach(log=>{
              client.sadd(s"DAU:${log.logDate}",log.mid)
            })
            client.close()
          })
        }
      )
  }


  /**
   * 本批次数据过滤后，留下来的那些之前没统计的mid，还存在本批次重复登录的情况，需去重
   *
   * @param filterDataDStream
   */
  def filterByMid(filterDataDStream: DStream[StartUpLog]): DStream[StartUpLog] = {
    //根据mid和logData分组
    val value1: DStream[((String, String), StartUpLog)] = filterDataDStream.map(log=>((log.mid,log.logDate),log))
    val value2: DStream[((String, String), Iterable[StartUpLog])] = value1.groupByKey()
    //组内排序取第一个 => 去重的效果
    val result: DStream[StartUpLog] = value2.flatMap {
      case ((mid, dt), iter) => {
        iter.toList.sortWith(_.ts < _.ts).take(1)
      }
    }
    result
  }


  private val sdf = new SimpleDateFormat("yyyy-MM-dd")
  /**
   * 在redis黑名单(之前批次中登录过，已统计到HBase中了)中查询 当前批次的 mid是否已经存在，是，则过滤掉
   * @param startUplogDStream 本批次传来的数据
   * @return 过滤掉 之前批次统计过的mid 的DStream
   */
  def filterLoggedOnUser(startUplogDStream: DStream[StartUpLog], sc: SparkContext) = {
      //方案二：在分区内获取jedis客户端连接
//      startUplogDStream.transform(rdd => {
//        rdd.mapPartitions(
//          iter => {
//            //jedis客户端连接
//            val jedisClient: Jedis = RedisUtil.getJedisClient
//            //过滤
//            val logs: Iterator[StartUpLog] = iter.filter(log => {
//              //存在 => 过滤掉
//              !jedisClient.sismember(s"DAU:${log.logDate}", log.mid)
//            })
//            //关闭连接
//            jedisClient.close()
//            logs
//          }
//        )
//      })
      //方案三： 在driver端获取数据后，封装成广播变量后，广播到Executor端
      startUplogDStream.transform(rdd =>{
        //建立连接
        val client: Jedis = RedisUtil.getJedisClient
        //从redis获取mid数据
        val mids: util.Set[String] = client.smembers(s"DAU:${sdf.format(new Date(System.currentTimeMillis()))}")
        //关闭连接
        client.close()
        //封装成广播变量
        val midsBC: Broadcast[util.Set[String]] = sc.broadcast(mids)
        //在Executor端使用midsBC
        rdd.filter(log=>{
          //已经写入库的，过滤掉
          !midsBC.value.contains(log.mid)
        })
      })
  }

}
