/// Implements module bindings
#include <stdlib.h>
#include <stdio.h>
#include <jni.h>
#include <pak.h>
#include <runtime.h>
#include "thread.h"

struct TempStruct {
	jobject byte_array;
	jbyte *data;
};
struct Module *get_mod(JNIEnv *env, jobject thiz, struct TempStruct *info) {
	set_jni_env_ctx(env, NULL);
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

	int rc = PAK_ERR_UNIMPLEMENTED;
	if (mod->on_find_connection) rc = mod->on_find_connection(mod, job);

	release_mod(env, &info);
	return rc;
}

int pak_wifi_adapter_from_jobject(JNIEnv *env, struct PakWiFiAdapter *adapter, jobject wifi_adapter);

JNIEXPORT jint JNICALL
Java_dev_danielc_common_NativeModule_onTryConnectWiFi(JNIEnv *env, jobject thiz, jobject adapter_o,
													  jint job) {
	struct TempStruct info;
	struct Module *mod = get_mod(env, thiz, &info);

	struct PakWiFiAdapter adapter;

	pak_wifi_adapter_from_jobject(env, &adapter, adapter_o);

	int rc = PAK_ERR_UNIMPLEMENTED;
	if (mod->on_try_connect_wifi) rc = mod->on_try_connect_wifi(mod, &adapter, job);

	release_mod(env, &info);
	return rc;
}

JNIEXPORT jint JNICALL
Java_dev_danielc_common_NativeModule_onIdleTick(JNIEnv *env, jobject thiz, jint us_since_last_tick) {
	struct TempStruct info;
	struct Module *mod = get_mod(env, thiz, &info);

	int rc = PAK_ERR_UNIMPLEMENTED;
	if (mod->on_idle_tick) rc = mod->on_idle_tick(mod, (unsigned int)us_since_last_tick);

	release_mod(env, &info);
	return rc;
}

JNIEXPORT jint JNICALL
Java_dev_danielc_common_NativeModule_onDisconnect(JNIEnv *env, jobject thiz) {
	struct TempStruct info;
	struct Module *mod = get_mod(env, thiz, &info);

	int rc = PAK_ERR_UNIMPLEMENTED;
	if (mod->on_switch_screen) rc = mod->on_disconnect(mod);

	release_mod(env, &info);
	return rc;
}

JNIEXPORT jint JNICALL
Java_dev_danielc_common_NativeModule_onSwitchScreen(JNIEnv *env, jobject thiz, jint old_screen,
													jint new_screen, jint job) {
	struct TempStruct info;
	struct Module *mod = get_mod(env, thiz, &info);

	int rc = PAK_ERR_UNIMPLEMENTED;
	if (mod->on_switch_screen) rc = mod->on_switch_screen(mod, old_screen, new_screen, job);

	release_mod(env, &info);
	return rc;
}

struct FileHandleWrapper {
	jobject storagename_o;
	struct FileHandle handle;
};

static struct FileHandleWrapper get_filehandle(JNIEnv *env, jobject file) {
	jclass filehandle_class = (*env)->FindClass(env, "dev/danielc/common/FileHandle");
	jfieldID storagename_f = (*env)->GetFieldID(env, filehandle_class, "storageName", "Ljava/lang/String;");
	jfieldID index_f = (*env)->GetFieldID(env, filehandle_class, "index", "I");
	jstring storagename_o = (*env)->GetObjectField(env, file, storagename_f);
	const char *storagename_s = NULL;
	if (storagename_o != NULL) storagename_s = (*env)->GetStringUTFChars(env, storagename_o, NULL);

	return (struct FileHandleWrapper){
		storagename_o,
		{
			.index_in_view = (*env)->GetIntField(env, file, index_f),
			.storage_name = storagename_s,
		},
	};
}

static void free_wrapper(JNIEnv *env, struct FileHandleWrapper *wrapper) {
	if (wrapper->storagename_o != NULL) (*env)->ReleaseStringUTFChars(env, wrapper->storagename_o, wrapper->handle.storage_name);
}

JNIEXPORT jint JNICALL
Java_dev_danielc_common_NativeModule_onRequestFileThumbnail(JNIEnv *env, jobject thiz, jint job,
															jobject file) {
	struct TempStruct info;
	struct Module *mod = get_mod(env, thiz, &info);

	(*env)->PushLocalFrame(env, 10);

	struct FileHandleWrapper wrapper = get_filehandle(env, file);

	int rc = 0;
	if (mod->on_request_file_thumbnail) rc = mod->on_request_file_thumbnail(mod, job, &wrapper.handle);

	free_wrapper(env, &wrapper);
	(*env)->PopLocalFrame(env, NULL);

	release_mod(env, &info);
	return rc;
}

JNIEXPORT jint JNICALL
Java_dev_danielc_common_NativeModule_onRequestFileContents(JNIEnv *env, jobject thiz, jint job,
														   jobject file) {
	struct TempStruct info;
	struct Module *mod = get_mod(env, thiz, &info);

	(*env)->PushLocalFrame(env, 10);

	struct FileHandleWrapper wrapper = get_filehandle(env, file);

	int rc = 0;
	if (mod->on_request_file_contents) rc = mod->on_request_file_contents(mod, job, &wrapper.handle);

	free_wrapper(env, &wrapper);
	(*env)->PopLocalFrame(env, NULL);

	release_mod(env, &info);
	return rc;
}

JNIEXPORT jint JNICALL
Java_dev_danielc_common_NativeModule_onRequestFileMetadata(JNIEnv *env, jobject thiz, jint job,
														   jobject file) {
	struct TempStruct info;
	struct Module *mod = get_mod(env, thiz, &info);

	(*env)->PushLocalFrame(env, 10);

	struct FileHandleWrapper wrapper = get_filehandle(env, file);

	int rc = 0;
	if (mod->on_request_file_metadata) rc = mod->on_request_file_metadata(mod, job, &wrapper.handle);

	free_wrapper(env, &wrapper);
	(*env)->PopLocalFrame(env, NULL);

	release_mod(env, &info);
	return rc;
}