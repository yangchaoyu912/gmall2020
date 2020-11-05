package com.yuwenzhi.bean.utils;

/**
 * @version 1.0
 * @description:
 * @author: 宇文智
 * @date 2020/11/3 18:13
 */
public class RanOpt<T>{
    T value ;
    int weight;

    public RanOpt ( T value, int weight ){
        this.value=value ;
        this.weight=weight;
    }

    public T getValue() {
        return value;
    }

    public int getWeight() {
        return weight;
    }
}