#include <stdlib.h>
#include <jni.h>
#include <stdarg.h>
#include <stdio.h>
#include <pak.h>
#include <runtime.h>
#include "thread.h"
#include "main.h"

struct RuntimePriv {
	jobject obj;
};

jobject pak_ndk_create_module(JNIEnv *env, int (*get_fn)(struct Module *mod), jobject manifest) {
	struct Module *mod = calloc(1, sizeof(struct Module));
	mod->rt = malloc(sizeof(struct RuntimePriv));

	jclass class = (*env)->FindClass(env, "dev/danielc/common/NativeModule");
	jmethodID constructor = (*env)->GetMethodID(env, class, "<init>", "(Ldev/danielc/common/ModuleManifest;)V");
	jobject o_mod = (*env)->NewObject(env, class, constructor, manifest);
	mod->rt->obj = o_mod;

	jbyteArray struct_ = (*env)->NewByteArray(env, sizeof(struct Module));
	(*env)->SetByteArrayRegion(env, struct_, 0, sizeof(struct Module), (const jbyte *)mod);

	jfieldID struct_field = (*env)->GetFieldID(env, class, "struct", "[B");
	(*env)->SetObjectField(env, o_mod, struct_field, struct_);

	get_fn(mod);

	int rc = 0;
	if (mod->init != NULL) rc = mod->init(mod);
	if (rc) return NULL;

	return o_mod;
}

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

void pak_rt_set_screen_supported(struct Module *mod, int screen, int v) {
	JNIEnv *env = get_jni_env();
	jclass module_c = (*env)->FindClass(env, "dev/danielc/common/NativeModule");
	jmethodID set_screen_supported = (*env)->GetMethodID(env, module_c, "setScreenSupported", "(IZ)V");
	(*env)->CallVoidMethod(env, mod->rt->obj, set_screen_supported, screen, (jboolean)v);
}

int get_module_dummy(struct Module *mod);

JNIEXPORT jobject JNICALL
Java_dev_danielc_common_NativeRuntime_getDummyModule(JNIEnv *env, jclass clazz, jobject manifest) {
	set_jni_env_ctx(env, clazz);
	return pak_ndk_create_module(env, get_module_dummy, manifest);
}
