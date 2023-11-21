package com.mingzz__.h26x.camera2;

import android.content.Context;
import android.graphics.Point;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.mingzz__.a2022h26x.R;
import com.mingzz__.h26x.web_socket.Client;
import com.mingzz__.util.L;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;

public class Camera2ClientActivity extends AppCompatActivity implements Client.DataCallBack {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.camera2_client_activity);

        initWH();

        initView();

    }

    private void initWH() {
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(dm);
        WIDTH = dm.widthPixels;         // 屏幕宽度（像素）
        HEIGHT = dm.heightPixels;       // 屏幕高度（像素）
        L.i("WIDTH,HEIGHT-->" + WIDTH + "," + HEIGHT);
        Point point = new Point();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getDisplay().getRealSize(point);
            //include status height
            WIDTH = point.x;
            HEIGHT = point.y;
            L.i("WIDTH,HEIGHT-->" + WIDTH + "," + HEIGHT);
        }

        WIDTH = 1080;
        HEIGHT = 1440;
    }

    final static String H264 = MediaFormat.MIMETYPE_VIDEO_AVC;
    final static String H265 = MediaFormat.MIMETYPE_VIDEO_HEVC;
    final static int PORT = 9876;
    static int WIDTH = 1080;
    static int HEIGHT = 1920;

    private void initMediaCodec() {

        try {
            h26xDecode = MediaCodec.createDecoderByType(H264);
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(H264,
                    WIDTH, HEIGHT);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, WIDTH * HEIGHT);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10);
            h26xDecode.configure(mediaFormat, surface, null, 0);
            prepareDecode();
        } catch (Exception e) {
            L.i(e.toString());
        }


    }

    MediaCodec h26xDecode;
    Client client;
    String ROOT_PATH;
    private void prepareDecode() throws URISyntaxException {
       ROOT_PATH = getCacheDir().getAbsolutePath();
        h26xDecode.start();
        client = new Client(new URI(URI));
        client.setDataCallBack(this);
        client.startMe();
    }

    //static final String URI = "ws://10.216.32.217:9876";
    //static final String URI = "ws://10.216.32.234:9876";
    static final String URI = "ws://10.216.32.214:9876";

    static class DecodeThread extends Thread {
        MediaCodec h26xDecode;

        public DecodeThread(MediaCodec h26xDecode) {
            this.h26xDecode = h26xDecode;

        }

        @Override
        public void run() {
            super.run();

        }


    }

    @Override
    public void getData(byte[] bytes) {
        //L.i("thread-->" + Thread.currentThread().getName());
        L.i("socket receive data size-->" + bytes.length);
        //FileUtil.writeBytesTo16Chars(bytes,ROOT_PATH+ File.separator+"socket_encode.txt");
        decodeAndDisplay(bytes);
    }

    private void decodeAndDisplay(byte[] bytes) {

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

        int inIndex = h26xDecode.dequeueInputBuffer(10000);
        if (inIndex >= 0) {
            ByteBuffer inputBuffer = h26xDecode.getInputBuffer(inIndex);
            inputBuffer.clear();
            inputBuffer.put(bytes, 0, bytes.length);
            h26xDecode.queueInputBuffer(inIndex, 0, bytes.length,
                    SystemClock.currentThreadTimeMillis(), 0);
            int outIndex = h26xDecode.dequeueOutputBuffer(bufferInfo, 10000);
            while (outIndex >= 0) {
                h26xDecode.releaseOutputBuffer(outIndex, true);
                outIndex = h26xDecode.dequeueOutputBuffer(bufferInfo, 10000);
            }
        }

    }

    boolean work = false;

    @Override
    protected void onDestroy() {
        release();
        super.onDestroy();
    }

    private void release() {
        work = false;
        if (null != h26xDecode) {
            h26xDecode.stop();
            h26xDecode.release();
        }
        if (null != client)
            client.closeMe();
    }

    SurfaceView surfaceView;
    Surface surface;

    private void initView() {

        surfaceView = findViewById(R.id.surface_view);
        SurfaceHolder holder = surfaceView.getHolder();
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                L.i("surfaceCreated");
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
}
