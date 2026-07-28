#pragma once
#include <jni.h>
#include <android/log.h>

void set_jni_env_ctx(JNIEnv *env, jobject ctx);
JNIEnv *get_jni_env(void);
jobject get_jni_ctx(void);