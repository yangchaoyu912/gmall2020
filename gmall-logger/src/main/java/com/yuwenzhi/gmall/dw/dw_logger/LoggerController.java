package com.yuwenzhi.gmall.dw.dw_logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.yuwenzhi.constants.GmallConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version 1.0
 * @description:
 * @author: 宇文智
 * @date 2020/11/3 18:57
 */
@RestController
@Slf4j
public class LoggerController {
    @Autowired
    KafkaTemplate<String,String> kafkaTemplate;
    @ResponseBody
    @RequestMapping("testDemo")
    public String testDemo(){
        return "hello demo";
    }

    @RequestMapping("log")
    @ResponseBody
    public String getLogger(@RequestParam("logString") String logString) {
        //在json字符串中添加时间戳字段
        JSONObject jsonObject = JSON.parseObject(logString);
        jsonObject.put("ts",System.currentTimeMillis());
        //打印日志，并落盘到file中
        String jsonString = jsonObject.toString();
        log.info(jsonString);
        //根据文件信息，推送到kafka不同的主题中
        if("startup".equals(jsonObject.getString("type"))){
            kafkaTemplate.send(GmallConstants.KAFKA_TOPIC_STARTUP,jsonString);
        }else{
            kafkaTemplate.send(GmallConstants.KAFKA_TOPIC_EVENT,jsonString);
        }
        return "success";
    }
}