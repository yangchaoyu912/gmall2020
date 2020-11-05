package com.yuwenzhi.utils

import java.io.InputStreamReader
import java.util.Properties

/**
 * @description:
 * @author: 宇文智
 * @date 2020/11/4 18:52
 * @version 1.0
 */
object PropertiesUtil {

  def load(propertieName:String): Properties ={
    val prop=new Properties()
    prop.load(new InputStreamReader(Thread.currentThread().getContextClassLoader.getResourceAsStream(propertieName) , "UTF-8"))
    prop
  }
}
