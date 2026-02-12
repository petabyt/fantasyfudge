/// Implements module bindings
#include <stdlib.h>
#include <stdio.h>
#include <jni.h>
#include <pak.h>
#include <runtime.h>

struct TempStruct {
	jobject byte_array;
	jbyte *data;
};
struct Module *get_mod(JNIEnv *env, jobject thiz, struct TempStruct *info) {
	jclass thiz_c = (*env)->GetObjectClass(env, thiz);
	jfieldID struct_id = (*env)->GetFieldID(env, thiz_c, "struct", "[B");
	if (struct_id == NULL) abort();

	info->byte_array = (*env)->GetObjectField(env, thiz, struct_id);
	info->data = (*env)->GetByteArrayElements(env, info->byte_array, NULL);
	return (struct Module *)info->data;
}
void release_mod(JNIEnv *env, struct TempStruct *info) {
	(*env)->ReleaseByteArrayElements(env, info->byte_array, info->data, 0);
}

JNIEXPORT void JNICALL
Java_dev_danielc_common_NativeModule_free(JNIEnv *env, jobject thiz) {
	struct TempStruct info;
	struct Module *mod = get_mod(env, thiz, &info);

	free(mod->rt);

	release_mod(env, &info);
}

JNIEXPORT jint JNICALL
Java_dev_danielc_common_NativeModule_onFindConnection(JNIEnv *env, jobject thiz, jint job) {
	struct TempStruct info;
	struct Module *mod = get_mod(env, thiz, &info);

	int rc = 0;
	if (mod->on_find_connection) rc = mod->on_find_connection(mod, job);

	release_mod(env, &info);
	return rc;
}

JNIEXPORT jint JNICALL
Java_dev_danielc_common_NativeModule_onTryConnectWiFi(JNIEnv *env, jobject thiz, jobject adapter,
													  jint job) {
	// TODO: implement onTryConnectWiFi()
}

JNIEXPORT jint JNICALL
Java_dev_danielc_common_NativeModule_onIdleTick(JNIEnv *env, jobject thiz,
												jint us_since_last_tick) {
	// TODO: implement onIdleTick()
}

JNIEXPORT jint JNICALL
Java_dev_danielc_common_NativeModule_onDisconnect(JNIEnv *env, jobject thiz) {
	// TODO: implement onDisconnect()
}

JNIEXPORT jint JNICALL
Java_dev_danielc_common_NativeModule_onSwitchScreen(JNIEnv *env, jobject thiz, jint old_screen,
													jint new_screen, jint job) {
	struct TempStruct info;
	struct Module *mod = get_mod(env, thiz, &info);

	int rc = 0;
	if (mod->on_switch_screen) rc = mod->on_switch_screen(mod, old_screen, new_screen, job);

	release_mod(env, &info);
	return rc;
}