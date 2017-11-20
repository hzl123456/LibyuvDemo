package com.hzl.libyuvdemo.listener;


/**
 * 作者：请叫我百米冲刺 on 2017/11/7 上午10:38
 * 邮箱：mail@hezhilin.cc
 */
public interface CameraSurfaceListener {

    void startAutoFocus(float x, float y);

    void openCamera();

    void releaseCamera();

    int changeCamera();
}
