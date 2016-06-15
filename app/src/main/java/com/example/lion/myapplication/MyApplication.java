package com.example.lion.myapplication;

import android.app.Application;
import android.content.res.AssetFileDescriptor;

/**
 * Created by Lion on 2016/6/15.
 */
public class MyApplication extends Application {
    public AssetFileDescriptor fileDescriptor;
    public int quality = 85;
    public int repeatTimes = 30;
}
