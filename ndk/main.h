#pragma once
#include <jni.h>
#include <pak.h>
#include <runtime.h>

jobject pak_ndk_create_module(JNIEnv *env, int (*get)(struct Module *mod));