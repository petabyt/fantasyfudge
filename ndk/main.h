#pragma once
#include <jni.h>
#include <pak.h>
#include <runtime.h>

struct RuntimePriv {
	jobject obj;
	char *setup_option;
	// dlopen handle
	void *lib;

	unsigned int log_len;
	unsigned int log_pos;
	char *log_buf;
};

struct ModuleJavaStruct {
	struct PakModule *ptr;
};

jobject pak_wifi_ap_filter_to_jobject(JNIEnv *env, struct PakWiFiApFilter *filter);

struct PakBtDevice *pak_bt_device_from_jobject(JNIEnv *env, struct PakBt *ctx, jobject dev_o);

struct PakWiFiAdapter *pak_wifi_adapter_from_jobject(JNIEnv *env, jobject wifi_adapter);