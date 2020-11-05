package com.yuwenzhi.bean.utils;

import java.util.Random;

/**
 * @version 1.0
 * @description:
 * @author: 宇文智
 * @date 2020/11/3 18:14
 */
public class RandomNum {
    public static int getRandInt(int fromNum,int toNum){
        return fromNum + new Random().nextInt(toNum-fromNum+1);
    }
}