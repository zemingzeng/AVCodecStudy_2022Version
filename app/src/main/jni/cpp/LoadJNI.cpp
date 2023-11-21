//
// Created by gms on 2022/7/12.
//

#include "load_jni.h"
#include "x264.h"
#include "x264_codec.h"

void Test::play() {
    LOGI("I am from JNI C++!!!");
}

void testX264();

extern "C"
void native_test(JNIEnv *env, jclass clazz) {
    // TODO: implement test()
    Test test;
    test.play();
    testX264();
    X264 x264;
    x264.play();
}

void testX264() {
    int width = 1080, height = 1920;
    x264_param_t param;
    x264_picture_t pic;
    x264_picture_t pic_out;
    x264_t *h;
    int i_frame = 0;
    int i_frame_size;
    x264_nal_t *nal;
    int i_nal;
    if (x264_param_default_preset(&param, "medium", NULL) < 0) {
        LOGI("x264_param_default_preset error!!");
        return;
    }

    /* Configure non-default params */
    param.i_bitdepth = 8;
    param.i_csp = X264_CSP_I420;
    param.i_width = width;
    param.i_height = height;
    param.b_vfr_input = 0;
    param.b_repeat_headers = 1;
    param.b_annexb = 1;
    param.i_bframe = 0;
    param.rc.i_rc_method = X264_RC_ABR; //平均码流控制

    /* Apply profile restrictions. */
    if (x264_param_apply_profile(&param, "high") < 0) {
        LOGI("x264_param_default_preset error!!");
        return;
    }
    if (x264_picture_alloc(&pic, param.i_csp, param.i_width, param.i_height) < 0) {
        LOGI("x264_picture_alloc error!!");
        return;
    }
    h = x264_encoder_open(&param);
    if (!h) {
        LOGI("x264_encoder_open error!!--->%p", h);
        return;
    } else {
        LOGI("x264_encoder_open success!!");
    }
    x264_encoder_close(h);
    x264_picture_clean(&pic);
}


/***********************************************************************************************************
 * register native methods
 * ********************************************************************************************************/

/*
 * Class and package name
 * static const char *classPathName = "com/qualcomm/qti/usta/core/AccelCalibrationJNI";
 * */
//com.mingzz__.h26x.rtmp.RTMPActivity
static const char *classPathName_1 = "com/mingzz__/h26x/rtmp/RTMPActivity";

/*
 * List of native methods
 *  {"startCalibration" , "()V", (void *)startCalibration},
 *  {"getAccelCalFromNative" , "()Ljava/lang/String;", (void *)getAccelCalFromNative},
 * {"stopCalibration" , "()V", (void *)stopCalibration},
 * */
static JNINativeMethod methods[] = {
        {"test", "()V", (void *) native_test},
};

/*
 * Register several native methods for one class.
 *
 *
 * */
static int
registerNativeMethods(JNIEnv *envVar, const char *inClassName, JNINativeMethod *inMethodsList,
                      int inNumMethods) {
    jclass javaClazz = envVar->FindClass(inClassName);
    if (javaClazz == NULL) {
        return JNI_FALSE;
    }
    if (envVar->RegisterNatives(javaClazz, inMethodsList, inNumMethods) < 0) {
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

/*
   * Register native methods for all classes we know about.
   *
   * Returns JNI_TRUE on success
   *
   * */
static int registerNatives(JNIEnv *env) {
    if (!registerNativeMethods(env, classPathName_1, methods,
                               sizeof(methods) / sizeof(methods[0]))) {
        return JNI_FALSE;
    }
    return JNI_TRUE;
}


typedef union {
    JNIEnv *env;
    void *venv;
} UnionJNIEnvToVoid;

/*
* This is called by the VM when the shared library is first loaded.
* */
JNIEXPORT jint
JNI_OnLoad(JavaVM *vm, void *reserved) {
    UnionJNIEnvToVoid uenv;
    uenv.venv = NULL;
    JNIEnv *env = NULL;

    if (vm->GetEnv(&uenv.venv, JNI_VERSION_1_4) != JNI_OK) {
        return -1;
    }
    env = uenv.env;

    if (registerNatives(env) != JNI_TRUE) {
        return -1;
    }
    LOGI("JNI_OnLoad Register Natives Methods Success!!!");
    return JNI_VERSION_1_4;
}

