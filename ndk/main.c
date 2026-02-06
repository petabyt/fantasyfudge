#include <jni.h>
#include <stdarg.h>
#include <stdio.h>
#include <pak.h>
#include "thread.h"

JNIEXPORT void JNICALL
Java_dev_danielc_common_NativeRuntime_init(JNIEnv *env, jclass clazz) {
	set_jni_env_ctx(env, clazz);

	pak_global_log("NativeRuntime init");
}


void pak_global_log(const char *fmt, ...) {
	char buffer[512] = {0};
	va_list args;
	va_start(args, fmt);
	vsnprintf(buffer, sizeof(buffer), fmt, args);
	va_end(args);

	JNIEnv *env = get_jni_env();
	(*env)->PushLocalFrame(env, 10);
	jstring buffer_s = (*env)->NewStringUTF(env, buffer);
	jclass backend_c = (*env)->FindClass(env, "dev/danielc/common/NativeRuntime");
	jmethodID log_global_m = (*env)->GetStaticMethodID(env, backend_c, "logGlobalLine", "(Ljava/lang/String;)V");
	(*env)->CallStaticVoidMethod(env, backend_c, log_global_m, buffer_s);
	(*env)->PopLocalFrame(env, NULL);
}
