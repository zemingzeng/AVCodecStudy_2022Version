//
// Created by gms on 2022/7/12.
//

#ifndef INC_2022H26X_LOAD_JNI_H
#define INC_2022H26X_LOAD_JNI_H

#include <android/log.h>
#include <jni.h>

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,"mingzz__",__VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,"mingzz__",__VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,"mingzz__",__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,"mingzz__",__VA_ARGS__)

class Test {
public:
    void play();
};


#endif //INC_2022H26X_LOAD_JNI_H
