package com.mingzz__.h26x.code_play;


import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.mingzz__.a2022h26x.R;
import com.mingzz__.util.L;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class PlayActivity extends AppCompatActivity {

    final String H264 = MediaFormat.MIMETYPE_VIDEO_AVC;
    final String H265 = MediaFormat.MIMETYPE_VIDEO_HEVC;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.h26x_play_activity);
        initView();
        initCheck();
        play();
    }

    private void initCheck() {
        L.i("everything init success!!");
    }

    MediaCodec h26xDecode;
    final int WIDTH = 720;
    final int HEIGHT = 1280;

    private void initMediaCodec() {
        try {
            h26xDecode = MediaCodec.createDecoderByType(H264);
            MediaFormat videoFormat = MediaFormat.createVideoFormat(H264, WIDTH, HEIGHT);
            videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, WIDTH * HEIGHT);
            videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 25);
            h26xDecode.configure(videoFormat, surface, null, 0);
            startDecode();
        } catch (IOException e) {
            L.i(e.toString());
            e.printStackTrace();
        }
    }

    private void startDecode() {
        String DATA_PATH = getCacheDir().getAbsolutePath() +
                File.separator + "demo.h264";
        new DecodeThread(h26xDecode, DATA_PATH).start();

    }

    private void play() {

    }

    SurfaceView surfaceView;
    Surface surface;

    private void initView() {
        surfaceView = findViewById(R.id.surface_view);
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                L.i("surfaceCreated!!!!");
                surface = holder.getSurface();
                initMediaCodec();
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

            }
        });
    }


    static class DecodeThread extends Thread {


        MediaCodec h26xDecode;
        String dataPath;

        public DecodeThread(MediaCodec mediaCodec, String path) {
            this.h26xDecode = mediaCodec;
            this.dataPath = path;
            h26xDecode.start();
        }

        @Override
        public void run() {
            super.run();
            L.i("DecodeThread run!!");
            byte[] h264data = getH264data(dataPath);
            decoding(h264data);
        }

        private void decoding(byte[] h264data) {

            int previousSeparatorIndex = 0;
            int nextSeparatorIndex;
            long frameCount = 0;
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            work = true;
            while (work) {

                //get buffer index form buffer queue
                int index = h26xDecode.dequeueInputBuffer(10000);
                if (index >= 0) {
                    nextSeparatorIndex = findNALU(h264data, h264data.length, previousSeparatorIndex + 2);
                    if (nextSeparatorIndex == -1)
                        return;
                    int len = nextSeparatorIndex - previousSeparatorIndex;
                    L.i("previousSeparatorIndex--->" + previousSeparatorIndex + " length-->" + len);
                    //get buffer from index
                    ByteBuffer inputBuffer = h26xDecode.getInputBuffer(index);
                    inputBuffer.clear();
                    //put data
                    inputBuffer.put(h264data, previousSeparatorIndex, len);
                    //queue buffer
                    long pts = computePresentationTime(frameCount);
                    h26xDecode.queueInputBuffer(index, 0, len, pts, 0);
                    previousSeparatorIndex = nextSeparatorIndex;

                    //get decode buffer index
                    int index_ = h26xDecode.dequeueOutputBuffer(bufferInfo, 10000);
                    long presentationTimeUs = bufferInfo.presentationTimeUs;
                    L.i("frameCount-->" + frameCount + "  presentationTimeUs-->" + presentationTimeUs);
                    L.i("pts-->" + pts);
                    while (index_ >= 0) {
                        //render buffer to surface view
                        h26xDecode.releaseOutputBuffer(index_, true);
                        index_ = h26xDecode.dequeueOutputBuffer(bufferInfo, 10000);
                        ++frameCount;
                    }
                }
            }
        }

        private long computePresentationTime(long frameIndex) {
            return 200 + frameIndex * 1000000 / 30;
        }

        private int findNALU(byte[] h264data, int length, int i) {
            while (i + 3 < length) {
                if ((h264data[i] == 0x00 && h264data[i + 1] == 0x00 && h264data[i + 2] == 0x00 && h264data[i + 3] == 0x01) ||
                        (h264data[i] == 0x00 && h264data[i + 1] == 0x00 && h264data[i + 2] == 0x01)) {
                    return i;
                }
                i++;
            }
            return -1;
        }


        private byte[] getH264data(String path) {
            int size = 1024;
            int len;
            byte[] data = new byte[size];
            try {
                InputStream inputStream = new DataInputStream(new FileInputStream(path));
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                while ((len = inputStream.read(data, 0, size)) != -1)
                    outputStream.write(data, 0, len);
                data = outputStream.toByteArray();
                L.i("data size---->" + data.length / 1024 + "KB");
            } catch (Exception e) {
                L.i(e.toString());
            }
            return data;
        }

    }

    @Override
    protected void onDestroy() {
        release();
        super.onDestroy();
    }

    static boolean work = false;

    private void release() {
        work = false;
        if (null != h26xDecode) {
            h26xDecode.stop();
            h26xDecode.release();
        }
    }
}
