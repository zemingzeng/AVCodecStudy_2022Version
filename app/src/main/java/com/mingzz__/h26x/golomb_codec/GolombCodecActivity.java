package com.mingzz__.h26x.golomb_codec;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.mingzz__.a2022h26x.R;
import com.mingzz__.util.L;

public class GolombCodecActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.rtmp_activity);

        initView();

        play();

    }

    private void initView() {


    }

    private void play() {

        for (int i = 0; i < 26; i++) {
            golombCodec(i);
        }

    }

    private void golombCodec(int i) {

        int zeroNumber = 1;
        while ((i + 1) >>> zeroNumber != 0) {
            zeroNumber++;
        }

        //7->111 +1 --->1000
        //1 -->2 --> 10

        StringBuilder stringBuilder = new StringBuilder();

        for (int j = 0; j < zeroNumber - 1; j++) {
            stringBuilder.append(0);
        }

        int numberBit;
        for (int j = zeroNumber - 1; j >= 0; j--) {
            numberBit = (((i + 1) & (1 << j)) == 0) ? 0 : 1;
            stringBuilder.append(numberBit);
        }

        L.i("GolombCodec number:" + i + " ------> " + stringBuilder);

    }

    private void golombDeCodec(int i, int zeroNumber) {

        int decodeNumber = 1 << zeroNumber;

    }
}
