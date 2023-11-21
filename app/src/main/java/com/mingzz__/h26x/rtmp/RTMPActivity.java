package com.mingzz__.h26x.rtmp;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.mingzz__.a2022h26x.R;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;

public class RTMPActivity extends AppCompatActivity {

    static {

        System.loadLibrary("rtmp_play");

    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.rtmp_activity);

        play();

        initView();

//        setProp("fw.max_users", 5);
    }

    private void initView() {

    }

    private void play() {

        test();
        

    }

    public static native void test();


    public void setProp(String key, int value) {
        String value_ = value + "";
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("set", String.class, String.class);
            get.invoke(c, key, value_);
        } catch (Exception e) {
            Log.d("mingzz__", e.toString());
        }
    }


}
