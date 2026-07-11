/// Implements module bindings
#include <stdlib.h>
#include <stdio.h>
#include <jni.h>
#include <pak.h>
#include <runtime.h>
#include "thread.h"
#include "main.h"

struct PakBtDevice *pak_bt_device_from_jobject(JNIEnv *env, struct PakBt *ctx, jobject dev_o);

struct PakWiFiAdapter *pak_wifi_adapter_from_jobject(JNIEnv *env, jobject wifi_adapter);

struct TempStruct {
	jobject byte_array;
	struct ModuleJavaStruct *data;
};
struct PakModule *get_mod(JNIEnv *env, jobject thiz, struct TempStruct *info) {
	set_jni_env_ctx(env, NULL);
	jclass thiz_c = (*env)->GetObjectClass(env, thiz);
	jfieldID struct_id = (*env)->GetFieldID(env, thiz_c, "struct", "[B");
	if (struct_id == NULL) abort();

	info->byte_array = (*env)->GetObjectField(env, thiz, struct_id);
	info->data = (struct ModuleJavaStruct *)(*env)->GetByteArrayElements(env, info->byte_array, NULL);
	return info->data->ptr;
}
void release_mod(JNIEnv *env, struct TempStruct *info) {
	(*env)->ReleaseByteArrayElements(env, info->byte_array, (jbyte *)info->data, 0);
}

JNIEXPORT void JNICALL
Java_dev_danielc_fudge_NativeModule_free(JNIEnv *env, jobject thiz) {
	struct TempStruct info;
	struct PakModule *mod = get_mod(env, thiz, &info);
	if (mod->free) mod->free(mod);
	pak_bt_unref_context(mod->bt);
	pak_net_unref_context(mod->net);
	free(mod->rt);
	release_mod(env, &info);
	free(mod);
}

JNIEXPORT jint JNICALL
Java_dev_danielc_fudge_NativeModule_onFindConnection(JNIEnv *env, jobject thiz, jint job) {
	struct TempStruct info;
	struct PakModule *mod = get_mod(env, thiz, &info);

	int rc = PAK_ERR_UNIMPLEMENTED;
	if (mod->on_find_connection) rc = mod->on_find_connection(mod, job);

	release_mod(env, &info);
	return rc;
}

JNIEXPORT jint JNICALL
Java_dev_danielc_fudge_NativeModule_onTryConnectWiFi(JNIEnv *env, jobject thiz, jobject adapter_o,
													  jint job) {
	struct TempStruct info;
	struct PakModule *mod = get_mod(env, thiz, &info);

	struct PakWiFiAdapter *adapter = pak_wifi_adapter_from_jobject(env, adapter_o);

	int rc = PAK_ERR_UNIMPLEMENTED;
	if (mod->on_try_connect_wifi) rc = mod->on_try_connect_wifi(mod, adapter, NULL, job);

	release_mod(env, &info);
	return rc;
}

int pak_saved_info_from_jobject(JNIEnv *env, jobject saved_o, struct PakSavedConnection *saved) {
	(*env)->PushLocalFrame(env, 10);

	jclass saved_c = (*env)->FindClass(env, "dev/danielc/common/SavedDeviceInfo");

	jobject name_o = (*env)->GetObjectField(env, saved_o, (*env)->GetFieldID(env, saved_c, "name", "Ljava/lang/String;"));
	const char *name_s = (*env)->GetStringUTFChars(env, name_o, NULL);
	saved->name = strdup(name_s);
	(*env)->ReleaseStringUTFChars(env, name_o, name_s);

	jobject id_o = (*env)->GetObjectField(env, saved_o, (*env)->GetFieldID(env, saved_c, "uniqueIdentifier", "Ljava/lang/String;"));
	const char *id_s = (*env)->GetStringUTFChars(env, id_o, NULL);
	saved->unique_id = strdup(id_s);
	(*env)->ReleaseStringUTFChars(env, id_o, id_s);

	jobject data_o = (*env)->GetObjectField(env, saved_o, (*env)->GetFieldID(env, saved_c, "privateData", "[B"));
	jsize len = (*env)->GetArrayLength(env, data_o);
	uint8_t *data = malloc((size_t)len);
	(*env)->GetByteArrayRegion(env, data_o, 0, len, (jbyte *)data);

	saved->aux_data_length = (unsigned int)len;
	saved->aux_data = data;

	(*env)->PopLocalFrame(env, NULL);
	return 0;
}

void free_saved_info(struct PakSavedConnection *saved) {
	free((char *)saved->name);
	free((char *)saved->unique_id);
	free((char *)saved->aux_data);
}

JNIEXPORT jint JNICALL
Java_dev_danielc_fudge_NativeModule_onTryConnectBluetooth(JNIEnv *env, jobject thiz, jobject device_o, jobject saved_o,
													  jint job) {
	struct TempStruct info;
	struct PakModule *mod = get_mod(env, thiz, &info);

	struct PakSavedConnection saved;
	struct PakSavedConnection *saved_ptr = NULL;
	if (saved_o != NULL) {
		pak_saved_info_from_jobject(env, saved_o, &saved);
		saved_ptr = &saved;
	}

	struct PakBtDevice *device = pak_bt_device_from_jobject(env, mod->bt, device_o);

	int rc = PAK_ERR_UNIMPLEMENTED;
	if (mod->on_try_connect_bluetooth) rc = mod->on_try_connect_bluetooth(mod, device, saved_ptr, job);

	if (saved_ptr != NULL) free_saved_info(saved_ptr);

	release_mod(env, &info);
	return rc;
}

JNIEXPORT jint JNICALL
Java_dev_danielc_fudge_NativeModule_onIdleTick(JNIEnv *env, jobject thiz, jint us_since_last_tick) {
	struct TempStruct info;
	struct PakModule *mod = get_mod(env, thiz, &info);

	int rc = PAK_ERR_UNIMPLEMENTED;
	if (mod->on_idle_tick) rc = mod->on_idle_tick(mod, (unsigned int)us_since_last_tick);

	release_mod(env, &info);
	return rc;
}

JNIEXPORT jint JNICALL
Java_dev_danielc_fudge_NativeModule_onDisconnect(JNIEnv *env, jobject thiz) {
	struct TempStruct info;
	struct PakModule *mod = get_mod(env, thiz, &info);

	int rc = PAK_ERR_UNIMPLEMENTED;
	if (mod->on_switch_screen) rc = mod->on_disconnect(mod);

	release_mod(env, &info);
	return rc;
}

JNIEXPORT jint JNICALL
Java_dev_danielc_fudge_NativeModule_onSwitchScreen(JNIEnv *env, jobject thiz, jint old_screen,
													jint new_screen, jint job) {
	struct TempStruct info;
	struct PakModule *mod = get_mod(env, thiz, &info);

	int rc = PAK_ERR_UNIMPLEMENTED;
	if (mod->on_switch_screen) rc = mod->on_switch_screen(mod, old_screen, new_screen, job);

	release_mod(env, &info);
	return rc;
}

struct FileHandleWrapper {
	jobject storagename_o;
	struct PakFileHandle handle;
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
Java_dev_danielc_fudge_NativeModule_onRequestFileThumbnail(JNIEnv *env, jobject thiz, jint job,
															jobject file) {
	struct TempStruct info;
	struct PakModule *mod = get_mod(env, thiz, &info);

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
Java_dev_danielc_fudge_NativeModule_onRequestFileContents(JNIEnv *env, jobject thiz, jint job,
														   jobject file) {
	struct TempStruct info;
	struct PakModule *mod = get_mod(env, thiz, &info);

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
Java_dev_danielc_fudge_NativeModule_onRequestFileMetadata(JNIEnv *env, jobject thiz, jint job,
														   jobject file) {
	struct TempStruct info;
	struct PakModule *mod = get_mod(env, thiz, &info);

	(*env)->PushLocalFrame(env, 10);

	struct FileHandleWrapper wrapper = get_filehandle(env, file);

	int rc = 0;
	if (mod->on_request_file_metadata) rc = mod->on_request_file_metadata(mod, job, &wrapper.handle);

	free_wrapper(env, &wrapper);
	(*env)->PopLocalFrame(env, NULL);

	release_mod(env, &info);
	return rc;
}

JNIEXPORT jint JNICALL Java_dev_danielc_fudge_NativeModule_onRunCommand(JNIEnv *env, jobject thiz, jint job, jstring arg0, jstring arg1, jstring arg2, jstring arg3) {
	struct TempStruct info;
	struct PakModule *mod = get_mod(env, thiz, &info);

	(*env)->PushLocalFrame(env, 10);

	const char *list[4];
	int argc = 0;
	if (arg0 != NULL) list[argc++] = (*env)->GetStringUTFChars(env, arg0, NULL);
	if (arg1 != NULL) list[argc++] = (*env)->GetStringUTFChars(env, arg1, NULL);
	if (arg2 != NULL) list[argc++] = (*env)->GetStringUTFChars(env, arg2, NULL);
	if (arg3 != NULL) list[argc++] = (*env)->GetStringUTFChars(env, arg3, NULL);

	int rc = 0;
	if (mod->on_custom_command) rc = mod->on_custom_command(mod, job, argc, list);

	argc = 0;
	if (arg0 != NULL) (*env)->ReleaseStringUTFChars(env, arg0, list[argc++]);
	if (arg1 != NULL) (*env)->ReleaseStringUTFChars(env, arg1, list[argc++]);
	if (arg2 != NULL) (*env)->ReleaseStringUTFChars(env, arg2, list[argc++]);
	if (arg3 != NULL) (*env)->ReleaseStringUTFChars(env, arg3, list[argc++]);

	(*env)->PopLocalFrame(env, NULL);

	release_mod(env, &info);
	return rc;
}

JNIEXPORT jint JNICALL Java_dev_danielc_fudge_NativeModule_onPropChanged(JNIEnv *env, jobject thiz, jint job, jobject pane) {
	struct TempStruct info;
	struct PakModule *mod = get_mod(env, thiz, &info);

	struct PakWidget setting;
	setting.title = "";

	(*env)->PushLocalFrame(env, 10);

	jobject args = (*env)->CallObjectMethod(env, pane, (*env)->GetMethodID(env, (*env)->FindClass(env, "dev/danielc/common/DashboardPane"), "getArgs", "()Ldev/danielc/common/DashboardPane$Properties;"));

	jclass properties_c = (*env)->FindClass(env, "dev/danielc/common/DashboardPane$Properties");
	jfieldID name_f = (*env)->GetFieldID(env, properties_c, "name", "Ljava/lang/String;");
	jstring name_s = (*env)->GetObjectField(env, args, name_f);
	const char *name = (*env)->GetStringUTFChars(env, name_s, NULL);
	setting.name = name;

	if ((*env)->IsInstanceOf(env, pane, (*env)->FindClass(env, "dev/danielc/common/DashboardPane$Button"))) {
		setting.type = PAK_BUTTON;
	} else if ((*env)->IsInstanceOf(env, pane, (*env)->FindClass(env, "dev/danielc/common/DashboardPane$BooleanSetting"))) {
		setting.type = PAK_BOOLEAN;
	}

	int rc = PAK_ERR_UNIMPLEMENTED;
	if (mod->on_setting_changed) rc = mod->on_setting_changed(mod, job, &setting);

	(*env)->ReleaseStringUTFChars(env, name_s, name);

	(*env)->PopLocalFrame(env, NULL);

	release_mod(env, &info);
	return rc;
}