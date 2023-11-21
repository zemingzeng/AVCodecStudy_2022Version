package com.mingzz__.h26x.rtmp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.WindowManager;

import com.mingzz__.a2022h26x.R;
import com.mingzz__.h26x.projection.ProjectionCallback;
import com.mingzz__.h26x.projection.ProjectionService;
import com.mingzz__.util.L;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ProjectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.projection_server_activity);
        initWH();
        initProjection();
    }

    MediaProjection projection;
    MediaProjectionManager projectionManager;

    static int WIDTH = 1080;
    static int HEIGHT = 1920;

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
    }

    private void initProjection() {
        projectionManager
                = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        Intent screenCaptureIntent = projectionManager.createScreenCaptureIntent();
        startActivityForResult(screenCaptureIntent, 1990);
    }

    Intent dataIntent;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || requestCode != 1990)
            return;
        dataIntent = new Intent(this, ProjectionService.class);
        dataIntent.putExtra("code", resultCode);
        dataIntent.putExtra("intent", data);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            bindService(dataIntent, new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    projection = ((ProjectionCallback) service).getProjection();
                    L.i("MediaProjectionActivity onServiceConnected get projection-->" + projection);
                    prepareProjection();
                    //unbindService(this);
                }
                @Override
                public void onServiceDisconnected(ComponentName name) {

                }
            }, BIND_AUTO_CREATE);
        } else {
            projection = projectionManager.getMediaProjection(resultCode, data);
        }
    }

    Surface surface;

    MediaCodec h26xEncode;

    final static String H264 = MediaFormat.MIMETYPE_VIDEO_AVC;
    final static String H265 = MediaFormat.MIMETYPE_VIDEO_HEVC;
    final static String MEDIACODEC_TYPE = H264;

    private void initMediaCodec() {

        try {
            h26xEncode = MediaCodec.createEncoderByType(MEDIACODEC_TYPE);
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(MEDIACODEC_TYPE, WIDTH, HEIGHT);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
            //30 frames one I frame
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, WIDTH * HEIGHT);
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            h26xEncode.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        } catch (IOException e) {
            L.i(e.toString());
        }
    }

    private void prepareProjection() {
        L.i("MediaProjectionActivity startProject");
        initMediaCodec();
        surface = h26xEncode.createInputSurface();
        projection.createVirtualDisplay("projection_h26x",
                WIDTH, HEIGHT, 3,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                surface, null, null);
        startProjectionAndEncode();
    }

    String ROOT_PATH;

    private void startProjectionAndEncode() {
        ROOT_PATH = getCacheDir().getAbsolutePath();
        new EncodeThread(h26xEncode,
                ROOT_PATH + File.separator + "projection.h264")
                .start();
    }

    private static boolean work = false;
    //I frame
    private static final byte H265_KEY_FRAME_TYPE = 19;
    //vps=32 sps=33 pps=34 p_frame=1 b_frame=0
    private static final byte H265_KEY_CONFIG_TYPE = 32;
    private static final byte H264_KEY_FRAME_TYPE = 5;
    private static final byte H264_KEY_CONFIG_TYPE = 7;
    private static byte[] key_config_bytes;

    static class EncodeThread extends Thread {
        MediaCodec h26xEncode;
        String filePath;

        public EncodeThread(MediaCodec mediaCodec, String filePath) {
            this.h26xEncode = mediaCodec;
            this.filePath = filePath;
            h26xEncode.start();
        }

        @Override
        public void run() {
            super.run();

            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

            work = true;
            while (work) {
                int outIndex = h26xEncode.dequeueOutputBuffer(bufferInfo, 1000);
                if (outIndex >= 0) {
                    ByteBuffer outputBuffer = h26xEncode.getOutputBuffer(outIndex);
                    //L.i("outputBuffer size----->" + bufferInfo.size);
                    byte[] bytes = new byte[bufferInfo.size];
                    //buffer to bytes
                    outputBuffer.get(bytes);
                    //write buffer to file
                    //FileUtil.writeBytes(bytes, filePath);
                    //FileUtil.writeBytesTo16Chars(bytes, filePath);
                    dealBuffer(bytes);
                    h26xEncode.releaseOutputBuffer(outIndex, false);
                }
            }
        }

        private void dealBuffer(byte[] bytes) {

            if (H264.equals(MEDIACODEC_TYPE)) {
                //I frame
                if (H264_KEY_FRAME_TYPE == (bytes[4] & 0x1f)) {
                    L.i("I frame------");
                    //add sps pps
                    byte[] temp = bytes;
                    bytes = new byte[key_config_bytes.length + bytes.length];
                    System.arraycopy(key_config_bytes, 0, bytes, 0, key_config_bytes.length);
                    System.arraycopy(temp, 0, bytes, key_config_bytes.length, temp.length);
                } else if (H264_KEY_CONFIG_TYPE == (bytes[4] & 0x1f)) {
                    //sps pps save it!!
                    L.i("sps pps------");
                    key_config_bytes = bytes;
                }

            } else if (H265.equals(MEDIACODEC_TYPE)) {

            }

            //prepare to send to rtmp jni
        }
    }


    @Override
    protected void onDestroy() {
        release();
        super.onDestroy();
    }

    private void release() {
        work = false;
        if (null != h26xEncode) {
            h26xEncode.stop();
            h26xEncode.release();
        }
    }

}
