package com.mingzz__.h26x.camera2;

import android.annotation.SuppressLint;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import com.mingzz__.a2022h26x.R;
import com.mingzz__.h26x.web_socket.Server;
import com.mingzz__.util.L;
import com.mingzz__.util.PermissionUtil;
import com.mingzz__.util.YUVUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class H26xCamera2Activity extends AppCompatActivity
        implements TextureView.SurfaceTextureListener {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.h26x_camera2_activity);
        initView();
    }

    TextureView textureView;

    private void initView() {

        PermissionUtil.checkPermission(this);

        textureView = findViewById(R.id.texture_view);

        textureView.setSurfaceTextureListener(this);

    }

    SurfaceTexture surfaceTexture;

    //view w h
    Size previewSize;

    //camera output w h
    Size suitablePreviewSize;

    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {

        surfaceTexture = surface;

        previewSize = new Size(width, height);

        try {
            initCamera();
        } catch (Exception e) {
            L.i(e.toString());
        }

    }

    CameraManager cameraManager;

    String ROOT_PATH;

    CameraDevice cameraDevice;

    @SuppressLint("MissingPermission")
    private void initCamera() throws Exception {

        // /data/data/package_name/cache , app private dir
        ROOT_PATH = getCacheDir().getAbsolutePath();

        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);

        String[] cameraIdList = cameraManager.getCameraIdList();

        for (String cameraID : cameraIdList) {
            //camera id 0 一般为后主摄像头
            L.i("Camera id : " + cameraID);
        }

        String mainBackCameraID = cameraIdList[0];

        //描述了camera硬件设备以及该设备的可用设置和输出参数
        CameraCharacteristics cameraCharacteristics = cameraManager
                .getCameraCharacteristics(mainBackCameraID);

        //底层相机数据流配置信息 key value
        StreamConfigurationMap streamConfigurationMap = cameraCharacteristics
                .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (streamConfigurationMap == null) {
            throw new RuntimeException("Cannot get available preview/video sizes");
        }

        //相机Clockwise顺时针 sensor 旋转方向
        int cameraSensorClockwiseOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        L.i("sensor Clockwise 角度 : " + cameraSensorClockwiseOrientation);

        //Get a list of sizes compatible with  class to use as an output.
        Size[] outputSizes = streamConfigurationMap.getOutputSizes(SurfaceTexture.class);

        for (Size size : outputSizes)
            L.i("Camera支持的输出尺寸宽高 : " + size.getWidth() + "  " + size.getHeight());

        suitablePreviewSize = getSuitablePreviewSize(previewSize.getWidth(),
                previewSize.getHeight(), outputSizes);

        cameraManager.openCamera(mainBackCameraID, new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice camera) {

                cameraDevice = camera;

                try {
                    startPreview();
                } catch (Exception e) {
                    L.i(e.toString());
                }

            }

            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {

            }

            @Override
            public void onError(@NonNull CameraDevice camera, int error) {

            }
        }, null);

    }

    Surface previewSurface;

    CaptureRequest.Builder captureRequestBuilder;

    ImageReader imageReader;

    List<Surface> surfaceList = new ArrayList<>();

    CameraCaptureSession captureSession;

    private void startPreview() throws Exception {

        closeCaptureSession();

        surfaceTexture.setDefaultBufferSize(suitablePreviewSize.getWidth(),
                suitablePreviewSize.getHeight());

        setUpImageReader();

        previewSurface = new Surface(surfaceTexture);

        surfaceList.clear();
        surfaceList.add(previewSurface);
        surfaceList.add(imageReader.getSurface());

        //创建个和camera的会话
        cameraDevice.createCaptureSession(surfaceList, new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(@NonNull CameraCaptureSession session) {

                captureSession = session;

                updatePreview();

            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession session) {

            }
        }, null);

    }


    //更新预览，一直去请求预览数据
    private void updatePreview() {

        configCaptureRequestBuilder();

        configCaptureSession();

    }

    //设置capture request builder
    private void configCaptureRequestBuilder() {
        try {
            //capture 预览请求构建
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            //AF_MODE_CONTINUOUS_PICTURE：快速持续聚焦，用于静态图片的ZSL捕获。一旦达到扫描目标，触发则立即锁住焦点。取消而继续持续聚焦。
            //captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            captureRequestBuilder.addTarget(previewSurface);
            captureRequestBuilder.addTarget(imageReader.getSurface());
        } catch (CameraAccessException e) {
            L.i(e.toString());
        }
    }

    private void configCaptureSession() {

        try {
            captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null);
        } catch (CameraAccessException e) {
            L.i(e.toString());
        }

    }


    private void closeCaptureSession() {

        if (null != captureSession) {

            captureSession.close();

            captureSession = null;

        }

    }

    //TextureView Listener
    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

    }

    @Override
    protected void onDestroy() {
        release();
        super.onDestroy();
    }

    private void release() {
        try {
            if (null != server) {
                server.closeMe();
            }
        } catch (Exception e) {
            L.i(e.toString());
        }
        if (null != h26xEncode) {
            h26xEncode.stop();
            h26xEncode.release();
        }
        if (null != captureSession)
            captureSession.close();
        if (null != cameraDevice)
            cameraDevice.close();
    }

    final static String H264 = MediaFormat.MIMETYPE_VIDEO_AVC;
    final static String H265 = MediaFormat.MIMETYPE_VIDEO_HEVC;
    final static String MEDIACODEC_TYPE = H264;
    static int WIDTH = 1080;
    static int HEIGHT = 1920;

    MediaCodec h26xEncode;

    private void initMediaCodec() throws IOException {

        initSocket();

        WIDTH = suitablePreviewSize.getHeight();

        HEIGHT = suitablePreviewSize.getWidth();

        h26xEncode = MediaCodec.createEncoderByType(MEDIACODEC_TYPE);

        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MEDIACODEC_TYPE,
                WIDTH, HEIGHT);

        //格式
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
        //码率 越高细节越多，一般宽*高即可
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, WIDTH * HEIGHT);
        //帧率
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        //编码I帧时间间隔
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5);

        h26xEncode.configure(mediaFormat, null, null,
                MediaCodec.CONFIGURE_FLAG_ENCODE);

        h26xEncode.start();

    }

    final static int PORT = 9876;
    Server server;

    private void initSocket() {

        server = new Server(PORT);

        server.startMe();

    }

    //竖屏为准
    private Size getSuitablePreviewSize(int width, int height, Size[] choiceSizes) {

        //把高定为最大的一边
        int h, w, h_, w_;
        h_ = Math.max(width, height);
        w_ = Math.min(width, height);

        boolean fromBigToSmall = true;

        if (choiceSizes.length > 1) {
            fromBigToSmall = choiceSizes[0].getWidth() > choiceSizes[choiceSizes.length - 1].getWidth();
        }

        if (!fromBigToSmall) {
            for (int i = choiceSizes.length - 1; i >= 0; i--) {

                h = Math.max(choiceSizes[i].getWidth(), choiceSizes[i].getHeight());
                w = Math.min(choiceSizes[i].getWidth(), choiceSizes[i].getHeight());

                //先找比例尺寸相同的 再找尺寸最接近的
                if (w / h == width / height && w == width && h == height) {
                    L.i("找到了与宽高size ：" + new Size(width, height) +
                            " 比例相同的且接近的size : " + choiceSizes[i].toString());
                    return choiceSizes[i];
                } else if (h <= h_ && w <= w_) {
                    L.i("找到了一个与宽高size ：" + new Size(width, height) +
                            "  相近的size : " + choiceSizes[i].toString());
                    return choiceSizes[i];
                }
            }
        } else {
            int i = 0;
            while (i < choiceSizes.length) {

                h = Math.max(choiceSizes[i].getWidth(), choiceSizes[i].getHeight());
                w = Math.min(choiceSizes[i].getWidth(), choiceSizes[i].getHeight());

                //先找比例尺寸相同的 再找尺寸最接近的
                if (w / h == width / height && w == width && h == height) {
                    L.i("找到了与宽高size ：" + new Size(width, height) +
                            " 比例相同的且接近的size : " + choiceSizes[i].toString());
                    return choiceSizes[i];
                } else if (h <= h_ && w <= w_) {
                    L.i("找到了一个与宽高size ：" + new Size(width, height) +
                            "  相近的size : " + choiceSizes[i].toString());
                    return choiceSizes[i];
                }
                i++;
            }
        }

        Size notNiceSize = choiceSizes[choiceSizes.length - 1];
        L.i("没找到与宽高size ：" + new Size(width, height) +
                "  合适的size,被迫返回size : " + notNiceSize.toString());
        return notNiceSize;
    }

    HandlerThread yuvThread;

    Handler yuvHandler;

    private void setUpImageReader() throws IOException {

        //init mediacodec
        initMediaCodec();

        imageReader = ImageReader.newInstance(suitablePreviewSize.getWidth()
                , suitablePreviewSize.getHeight()
                , ImageFormat.YUV_420_888, 1);


        yuvThread = new HandlerThread("camera2-yuv");
        yuvThread.start();
        yuvHandler = new Handler(yuvThread.getLooper(), null);

        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {

            byte[] temp;

            @Override
            public void onImageAvailable(ImageReader reader) {

                Image image = reader.acquireNextImage();

                //yuv420(yy uu vv)->NV12(yyy uv uv)
                temp = YUV420ToNV12(image);

                temp = YUVUtil.YUV420BytesClockwise90Rotate(temp,
                        suitablePreviewSize.getWidth(),
                        suitablePreviewSize.getHeight(), YUVUtil.NV12);

                //验证yuv图片
                //FileUtil.writeBytes(temp,
                //ROOT_PATH + File.separator +
                //System.currentTimeMillis() + "_camera_NV12.yuv");

                //start encode
                startEncode(temp);

                image.close();

            }
        }, yuvHandler);
    }

    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
    int frameIndex;
    long presentationTimeUs;

    private void startEncode(byte[] temp) {

        int inputIndex = h26xEncode.dequeueInputBuffer(10000);

        if (inputIndex >= 0) {

            ByteBuffer inputBuffer = h26xEncode.getInputBuffer(inputIndex);

            inputBuffer.clear();

            inputBuffer.put(temp);

            presentationTimeUs = computePresentationTime(frameIndex);

            h26xEncode.queueInputBuffer(inputIndex, 0, temp.length,
                    presentationTimeUs, 0);

            frameIndex++;

        }

        int outputIndex = h26xEncode.dequeueOutputBuffer(bufferInfo, 10000);

        while (outputIndex >= 0) {

            ByteBuffer outputBuffer = h26xEncode.getOutputBuffer(outputIndex);

            sendEncodedBytes(outputBuffer, bufferInfo);

            h26xEncode.releaseOutputBuffer(outputIndex, false);

            outputIndex = h26xEncode.dequeueOutputBuffer(bufferInfo, 10000);
        }

    }

    private long computePresentationTime(long frameIndex) {
        return 200 + frameIndex * 1000000 / 29;
    }

    //I frame
    private static final byte H265_KEY_FRAME_TYPE = 19;
    //vps=32 sps=33 pps=34 p_frame=1 b_frame=0
    private static final byte H265_KEY_CONFIG_TYPE = 32;
    private static final byte H264_KEY_FRAME_TYPE = 5;
    private static final byte H264_KEY_CONFIG_TYPE = 7;
    private byte[] key_config_bytes;

    private void sendEncodedBytes(ByteBuffer outputBuffer, MediaCodec.BufferInfo bufferInfo) {

        byte[] bytes = new byte[bufferInfo.size];

        outputBuffer.get(bytes);

        //FileUtil.writeBytes(bytes,ROOT_PATH+
        //File.separator+"camera2.h26x");

        // 00 00 01 or 00 00 00 01
        byte typeByteOffset = 4;
        if (outputBuffer.get(2) == 0x01)
            typeByteOffset = 3;
        byte typeByte = outputBuffer.get(typeByteOffset);

        byte[] temp;
        int type;

        if (H264.equals(MEDIACODEC_TYPE)) {

            type = typeByte & 0x1f;

            if (type == H264_KEY_CONFIG_TYPE) {

                key_config_bytes = bytes;

            } else if (type == H264_KEY_FRAME_TYPE) {

                temp = bytes;
                bytes = new byte[key_config_bytes.length + bytes.length];
                System.arraycopy(key_config_bytes, 0, bytes, 0, key_config_bytes.length);
                System.arraycopy(temp, 0, bytes, key_config_bytes.length, temp.length);

            }

        } else if (H265.equals(MEDIACODEC_TYPE)) {

            type = (typeByte & 0x7e) >> 1;

            L.i("type : " + type);

            if (type == H265_KEY_CONFIG_TYPE) {

                key_config_bytes = bytes;

            } else if (type == H265_KEY_FRAME_TYPE) {

                temp = bytes;
                bytes = new byte[key_config_bytes.length + bufferInfo.size];
                System.arraycopy(key_config_bytes, 0, bytes, 0, key_config_bytes.length);
                System.arraycopy(temp, 0, bytes, key_config_bytes.length, temp.length);

            }

        }

        //one frame data
        //FileUtil.writeBytesTo16Chars(bytes,ROOT_PATH+
        //File.separator+"camera2.h26x");

        if(null!=server&&server.isOpen())
        server.sendData(bytes);

    }

    private byte[] YUV420ToNV12(Image image) {

        byte[] temp = new byte[image.getWidth() * image.getHeight() * 3 / 2];

        Image.Plane[] planes = image.getPlanes();

        ByteBuffer YBuffer = planes[0].getBuffer();
        int YBufferActualSize = YBuffer.remaining();
        //L.i("YBufferActualSize--->" + YBufferActualSize);
        ByteBuffer UBuffer = planes[1].getBuffer();
        int UBufferActualSize = UBuffer.remaining();
        //L.i("UBufferActualSize--->" + UBufferActualSize);
        ByteBuffer VBuffer = planes[2].getBuffer();
        int VBufferActualSize = VBuffer.remaining();
        //L.i("VBufferActualSize--->" + VBufferActualSize);

        int pixelStride = planes[1].getPixelStride();
        int pixelStride1 = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowStride1 = planes[1].getRowStride();
        //L.i("Image Planes pixelStride[0]--->" + pixelStride1);
        //L.i("Image Planes pixelStride[1]--->" + pixelStride);
        //L.i("Image Planes rowStride[0]--->" + rowStride);
        //L.i("Image Planes rowStride[1]--->" + rowStride1);

        int position = YBuffer.position();
        //copy y data
        for (int i = 0; i < image.getHeight(); i++) {
            YBuffer.position(position + rowStride * i);
            YBuffer.get(temp, image.getWidth() * i, image.getWidth());
        }

        //验证dsp支持NV12 uv uv uv不是NV21
        //copy vu data
        position = VBuffer.position();
        int start = image.getWidth() * image.getHeight();
        for (int j = 0; j < image.getHeight() / 2; j++) {
            //uv uv uv 00000
            VBuffer.position(position + rowStride1 * j);
            if (j == (image.getHeight() / 2 - 1)) {
                VBuffer.get(temp, start + j * image.getWidth(),
                        image.getWidth() - 1);
                temp[temp.length - 1] = UBuffer.get(UBufferActualSize - 1);
            } else {
                VBuffer.get(temp, start + j * image.getWidth(),
                        image.getWidth());
            }
        }

        //vu vu ---> uv uv
        byte tempByte = 0;
        for (int i = start; i < temp.length - 1; i += 2) {
            if (i % 2 == 0) {
                tempByte = temp[i];
                temp[i] = temp[i + 1];
                temp[i + 1] = tempByte;
            }
        }

        //L.i("thread--->" + Thread.currentThread().getName());

        return temp;

    }

}
