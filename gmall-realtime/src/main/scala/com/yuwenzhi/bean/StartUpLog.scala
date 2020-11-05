package com.yuwenzhi.bean

/**
 * @description:
 * @author: 宇文智
 * @date 2020/11/4 18:54
 * @version 1.0
 */
case class StartUpLog(mid:String, //设备idffff
                      uid:String,
                      appid:String,
                      area:String,
                      os:String,
                      ch:String,
                      `type`:String,
                      vs:String,
                      var logDate:String,
                      var logHour:String,
                      var ts:Long)
