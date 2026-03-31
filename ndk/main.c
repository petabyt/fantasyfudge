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

int get_module_dummy(struct Module *mod);
int setup_quickjs_module(struct Module **mod, const char *filename);
int get_module_libfuji(struct Module *mod);

int pak_ndk_create_module(JNIEnv *env, jobject o_mod, int (*get_fn)(struct Module *mod), jobject manifest) {
	struct Module *mod = calloc(1, sizeof(struct Module));
	mod->rt = malloc(sizeof(struct RuntimePriv));

	jclass class = (*env)->FindClass(env, "dev/danielc/common/NativeModule");
	mod->rt->obj = (*env)->NewGlobalRef(env, o_mod);

	get_fn(mod);

	int rc = 0;
	if (mod->init != NULL) rc = mod->init(mod);

	jbyteArray struct_ = (*env)->NewByteArray(env, sizeof(struct Module));
	(*env)->SetByteArrayRegion(env, struct_, 0, sizeof(struct Module), (const jbyte *)mod);

	jfieldID struct_field = (*env)->GetFieldID(env, class, "struct", "[B");
	(*env)->SetObjectField(env, o_mod, struct_field, struct_);

	return rc;
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

void pak_debug_log(struct Module *mod, const char *fmt, ...) {
	char buffer[512] = {0};
	va_list args;
	va_start(args, fmt);
	vsnprintf(buffer, sizeof(buffer), fmt, args);
	va_end(args);

	JNIEnv *env = get_jni_env();
	(*env)->PushLocalFrame(env, 10);
	jstring buffer_s = (*env)->NewStringUTF(env, buffer);
	jclass module_c = (*env)->FindClass(env, "dev/danielc/common/NativeModule");
	jmethodID debug_log_m = (*env)->GetMethodID(env, module_c, "debugLog", "(Ljava/lang/String;)V");
	(*env)->CallVoidMethod(env, mod->rt->obj, debug_log_m, buffer_s);
	(*env)->PopLocalFrame(env, NULL);
}

int pak_rt_set_screen_supported(struct Module *mod, int screen, int v) {
	JNIEnv *env = get_jni_env();
	jclass module_c = (*env)->FindClass(env, "dev/danielc/common/NativeModule");
	jmethodID set_screen_supported = (*env)->GetMethodID(env, module_c, "setScreenSupported", "(IZ)V");
	(*env)->CallVoidMethod(env, mod->rt->obj, set_screen_supported, screen, (jboolean)v);
	return 0;
}

int pak_rt_set_session_property(struct Module *mod, const char *key, const char *value) {
	JNIEnv *env = get_jni_env();
	(*env)->PushLocalFrame(env, 10);
	jclass module_c = (*env)->FindClass(env, "dev/danielc/common/NativeModule");
	jmethodID set_screen_supported = (*env)->GetMethodID(env, module_c, "setProperty", "(Ljava/lang/String;Ljava/lang/String;)V");
	jstring key_s = (*env)->NewStringUTF(env, key);
	jstring value_s = (*env)->NewStringUTF(env, value);
	(*env)->CallVoidMethod(env, mod->rt->obj, set_screen_supported, key_s, value_s);
	(*env)->PopLocalFrame(env, NULL);
	return 0;
}

int pak_rt_add_user_setting(struct Module *mod, const struct PakUserSetting *s) {
	JNIEnv *env = get_jni_env();
	(*env)->PushLocalFrame(env, 10);
	jclass setting_c = (*env)->FindClass(env, "dev/danielc/common/UserSetting");
	jmethodID constructor = (*env)->GetMethodID(env, setting_c, "<init>", "(Ljava/lang/String;Ljava/lang/Boolean;Ljava/lang/Integer;Ljava/lang/String;Ljava/lang/Integer;Ljava/lang/Integer;Ljava/util/List;)V");
	jstring name_s = (*env)->NewStringUTF(env, s->name);
	jobject boolv = NULL;
	jobject intv = NULL;
	jobject stringv = NULL;
	jobject min_o = NULL;
	jobject max_o = NULL;
	jobject dropdownlist_o = NULL;
	if (s->type == PAK_BOOLEAN) {
		jclass clazz = (*env)->FindClass(env, "java/lang/Boolean");
		boolv = (*env)->NewObject(env, clazz, (*env)->GetMethodID(env, clazz, "<init>", "(Z)V"), s->u.boolv.v);
	}
	jobject setting_o = (*env)->NewObject(env, setting_c, constructor, name_s, boolv, intv, stringv, min_o, max_o, dropdownlist_o);

	jclass module_c = (*env)->FindClass(env, "dev/danielc/common/NativeModule");
	jmethodID add_setting = (*env)->GetMethodID(env, module_c, "addUserSetting", "(Ldev/danielc/common/UserSetting;)V");
	(*env)->CallVoidMethod(env, mod->rt->obj, add_setting, setting_o);

	(*env)->PopLocalFrame(env, NULL);
	return 0;
}

int pak_rt_test_module(struct Module *mod) {
	return 0;
}

JNIEXPORT int JNICALL
Java_dev_danielc_common_NativeRuntime_setupLibFujiModule(JNIEnv *env, jclass clazz, jobject mod_o, jobject manifest) {
	set_jni_env_ctx(env, clazz);
	return pak_ndk_create_module(env, mod_o, get_module_libfuji, manifest);
}

JNIEXPORT int JNICALL
Java_dev_danielc_common_NativeRuntime_setupDummyNativeModule(JNIEnv *env, jclass clazz, jobject mod_o, jobject manifest) {
	set_jni_env_ctx(env, clazz);
	return pak_ndk_create_module(env, mod_o, get_module_dummy, manifest);
}

JNIEXPORT int JNICALL
Java_dev_danielc_common_NativeRuntime_setupWebassemblyModule(JNIEnv *env, jclass clazz, jobject mod_o, jobject manifest, jstring path) {
	abort();
	return -1;
}

JNIEXPORT int JNICALL
Java_dev_danielc_common_NativeRuntime_setupJavascriptModule(JNIEnv *env, jclass clazz, jobject mod_o, jobject manifest, jstring jsPath) {
	set_jni_env_ctx(env, clazz);

	const char *js_path = (*env)->GetStringUTFChars(env, jsPath, 0);

	struct Module *mod;
	int rc = setup_quickjs_module(&mod, js_path);
	if (rc) return rc;
	mod->rt = malloc(sizeof(struct RuntimePriv));
	mod->rt->obj = mod_o;

	jbyteArray struct_ = (*env)->NewByteArray(env, sizeof(struct Module));
	(*env)->SetByteArrayRegion(env, struct_, 0, sizeof(struct Module), (const jbyte *)mod);

	jfieldID struct_field = (*env)->GetFieldID(env, (*env)->FindClass(env, "dev/danielc/common/NativeModule"), "struct", "[B");
	(*env)->SetObjectField(env, mod_o, struct_field, struct_);

	if (mod->init != NULL) rc = mod->init(mod);
	if (rc) return rc;

	return 0;
}
