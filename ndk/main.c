#include <stdlib.h>
#include <jni.h>
#include <stdarg.h>
#include <stdio.h>
#include <pak.h>
#include <runtime.h>
#include <runtime_ext.h>
#include "thread.h"
#include "main.h"

int get_module_dummy(struct Module *mod);
int get_module_libfuji(struct Module *mod);
int get_module_cmfnothingaudio(struct Module *mod);
int get_module_goveelife(struct Module *mod);

static struct Module *create_module(JNIEnv *env, jobject o_mod) {
	struct Module *mod = calloc(1, sizeof(struct Module));
	mod->rt = malloc(sizeof(struct RuntimePriv));

	mod->rt->obj = (*env)->NewGlobalRef(env, o_mod);
	jstring str = (*env)->CallObjectMethod(env, o_mod, (*env)->GetMethodID(env, (*env)->FindClass(env, "dev/danielc/common/ModuleInstance"), "getSetupOptionName", "()Ljava/lang/String;"));
	if (str == NULL) {
		mod->rt->setup_option = NULL;
	} else {
		const char *setup_option = (*env)->GetStringUTFChars(env, str, 0);
		mod->rt->setup_option = strdup(setup_option);
		(*env)->ReleaseStringUTFChars(env, str, setup_option);
	}
	return mod;
}
static int finalize_module(JNIEnv *env, struct Module *mod) {
	int rc = 0;
	if (mod->init != NULL) rc = mod->init(mod);

	mod->bt = pak_bt_get_context();
	mod->net = pak_net_get_context();

	struct ModuleJavaStruct temp = { .ptr = mod, };
	jbyteArray struct_o = (*env)->NewByteArray(env, sizeof(struct ModuleJavaStruct));
	(*env)->SetByteArrayRegion(env, struct_o, 0, sizeof(struct ModuleJavaStruct), (const jbyte *)&temp);

	jfieldID struct_field = (*env)->GetFieldID(env, (*env)->FindClass(env, "dev/danielc/fudge/NativeModule"), "struct", "[B");
	(*env)->SetObjectField(env, mod->rt->obj, struct_field, struct_o);
	return rc;
}

JNIEXPORT void JNICALL
Java_dev_danielc_fudge_AndroidRuntime_init(JNIEnv *env, jclass clazz) {
	set_jni_env_ctx(env, clazz);
	pak_global_log("AndroidRuntime init");
}

static uint8_t *file_add(void *arg, uint8_t *buffer, unsigned int new_len, unsigned int old_len) {
	uint8_t *new = realloc(buffer, new_len);
	fread(new + old_len, 1, new_len - old_len, (FILE *)arg);
	return new;
}

JNIEXPORT jbyteArray JNICALL
Java_dev_danielc_libpak_Exif_getExifThumbnail(JNIEnv *env, jclass clazz, jstring filepath) {
	const char *cfilepath = (*env)->GetStringUTFChars(env, filepath, 0);
	FILE *f = fopen(cfilepath, "rb");
	if (f == NULL) {
		abort();
	}

	uint8_t *buffer = malloc(5000);
	fread(buffer, 1, 5000, f);

	struct ExifParser c = {0};
	int rc = exif_start_raw(&c, buffer, 5000, file_add, f);
	if (rc < 0) {
		abort();
	}

	if (c.thumb_of == 0 || c.thumb_size == 0) {
		return NULL;
	}

	jbyteArray result = (*env)->NewByteArray(env, (jsize)c.thumb_size);
	(*env)->SetByteArrayRegion(env, result, 0, (jsize)c.thumb_size, (jbyte *)(c.buf + c.thumb_of));

	free(c.buf);
	fclose(f);
	(*env)->ReleaseStringUTFChars(env, filepath, cfilepath);

	return result;
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
	jclass backend_c = (*env)->FindClass(env, "dev/danielc/fudge/AndroidRuntime");
	jmethodID log_global_m = (*env)->GetStaticMethodID(env, backend_c, "logGlobalLine", "(Ljava/lang/String;)V");
	(*env)->CallStaticVoidMethod(env, backend_c, log_global_m, buffer_s);
	(*env)->PopLocalFrame(env, NULL);
}

int pak_rt_set_tick_interval(struct Module *mod, unsigned int us) {
	JNIEnv *env = get_jni_env();
	(*env)->PushLocalFrame(env, 10);
	jclass module_c = (*env)->FindClass(env, "dev/danielc/common/ModuleInstance");
	jmethodID method = (*env)->GetMethodID(env, module_c, "setTickRate", "(I)V");
	(*env)->CallVoidMethod(env, mod->rt->obj, method, (int)us);
	(*env)->PopLocalFrame(env, NULL);
	return 0;
}

void pak_debug_log(struct Module *mod, const char *fmt, ...) {
	char buffer[512] = {0};
	va_list args;
	va_start(args, fmt);
	vsnprintf(buffer, sizeof(buffer), fmt, args);
	va_end(args);

	JNIEnv *env = get_jni_env();
	pak_global_log("%p %p", env, mod->rt);
	(*env)->PushLocalFrame(env, 10);
	jstring buffer_s = (*env)->NewStringUTF(env, buffer);
	jclass module_c = (*env)->FindClass(env, "dev/danielc/common/ModuleInstance");
	jmethodID debug_log_m = (*env)->GetMethodID(env, module_c, "debugLog", "(Ljava/lang/String;)V");
	(*env)->CallVoidMethod(env, mod->rt->obj, debug_log_m, buffer_s);
	(*env)->PopLocalFrame(env, NULL);
}

void pak_rt_fatal_error(struct Module *mod, const char *fmt, ...) {
	char buffer[512] = {0};
	va_list args;
	va_start(args, fmt);
	vsnprintf(buffer, sizeof(buffer), fmt, args);
	va_end(args);

	JNIEnv *env = get_jni_env();
	(*env)->PushLocalFrame(env, 10);
	jstring buffer_s = (*env)->NewStringUTF(env, buffer);
	jclass module_c = (*env)->FindClass(env, "dev/danielc/common/ModuleInstance");
	jmethodID debug_log_m = (*env)->GetMethodID(env, module_c, "forceDisconnect", "(Ljava/lang/String;I)V");
	(*env)->CallVoidMethod(env, mod->rt->obj, debug_log_m, buffer_s, 0);
	(*env)->PopLocalFrame(env, NULL);
}

void pak_error(const char *fmt, ...) {
	char buffer[512] = {0};
	va_list args;
	va_start(args, fmt);
	vsnprintf(buffer, sizeof(buffer), fmt, args);
	va_end(args);

	__android_log_write(ANDROID_LOG_ERROR, "pak_error", buffer);
}

void pak_abort(const char *fmt, ...) {
	char buffer[512];
	va_list args;
	va_start(args, fmt);
	vsnprintf(buffer, sizeof(buffer), fmt, args);
	va_end(args);

	__android_log_write(ANDROID_LOG_ERROR, "pak_abort", buffer);
	abort();
}

static jobject create_filehandle(JNIEnv *env, const struct FileHandle *file) {
	if (file == NULL) return NULL;
	(*env)->PushLocalFrame(env, 10);
	jclass filehandle_c = (*env)->FindClass(env, "dev/danielc/common/FileHandle");
	jmethodID constructor = (*env)->GetMethodID(env, filehandle_c, "<init>", "(ILjava/lang/String;)V");
	jobject handle_o = (*env)->NewObject(env, filehandle_c, constructor,
		file->index_in_view,
		(*env)->NewStringUTF(env, file->storage_name)
	);
	return (*env)->PopLocalFrame(env, handle_o);
}

static jobject create_filemetadata(JNIEnv *env, const struct FileMetadata *meta) {
	if (meta == NULL) return NULL;
	(*env)->PushLocalFrame(env, 10);
	jclass metadata_c = (*env)->FindClass(env, "dev/danielc/common/FileMetadata");
	jmethodID constructor = (*env)->GetMethodID(env, metadata_c, "<init>", "(Ljava/lang/String;Ljava/lang/String;III)V");
	jobject handle_o = (*env)->NewObject(env, metadata_c, constructor,
		(*env)->NewStringUTF(env, meta->filename),
		(*env)->NewStringUTF(env, meta->mime_type),
		meta->image_width,
		meta->image_height,
		meta->file_size
	);
	return (*env)->PopLocalFrame(env, handle_o);
}

const char *pak_rt_get_client_name(void) {
	JNIEnv *env = get_jni_env();
	static const char *cstr = NULL;
	if (cstr == NULL) {
		(*env)->PushLocalFrame(env, 10);
		jclass module_c = (*env)->FindClass(env, "dev/danielc/fudge/AndroidRuntime");
		jmethodID is_job_cancelled = (*env)->GetStaticMethodID(env, module_c, "getDeviceFriendlyName", "()Ljava/lang/String;");
		jstring s = (*env)->CallStaticObjectMethod(env, module_c, is_job_cancelled);
		cstr = (*env)->GetStringUTFChars(env, s, NULL);
		(*env)->PopLocalFrame(env, NULL);
	}
	return cstr;
}

int pak_rt_set_screen_supported(struct Module *mod, int screen, int v) {
	JNIEnv *env = get_jni_env();
	jclass module_c = (*env)->FindClass(env, "dev/danielc/common/ModuleInstance");
	jmethodID set_screen_supported = (*env)->GetMethodID(env, module_c, "setScreenSupported", "(IZ)V");
	(*env)->CallVoidMethod(env, mod->rt->obj, set_screen_supported, screen, (jboolean)v);
	return 0;
}

int pak_rt_is_job_cancelled(struct Module *mod, int job) {
	JNIEnv *env = get_jni_env();
	jclass module_c = (*env)->FindClass(env, "dev/danielc/common/ModuleInstance");
	jmethodID is_job_cancelled = (*env)->GetMethodID(env, module_c, "isJobCancelled", "(I)Z");
	return (*env)->CallBooleanMethod(env, mod->rt->obj, is_job_cancelled, job);
}

int pak_rt_set_progress_bar(struct Module *mod, int job, int percent) {
	JNIEnv *env = get_jni_env();
	(*env)->PushLocalFrame(env, 10);
	jclass module_c = (*env)->FindClass(env, "dev/danielc/common/ModuleInstance");
	jmethodID set_progress_bar = (*env)->GetMethodID(env, module_c, "setProgressBar", "(II)V");
	(*env)->CallVoidMethod(env, mod->rt->obj, set_progress_bar, job, percent);
	(*env)->PopLocalFrame(env, NULL);
	return 0;
}

int pak_rt_set_storage_info(struct Module *mod, const char *storage_name, unsigned int n_items, enum SortedBy sorted_by) {
	JNIEnv *env = get_jni_env();
	(*env)->PushLocalFrame(env, 10);
	jclass module_c = (*env)->FindClass(env, "dev/danielc/common/ModuleInstance");
	jmethodID set_storage_info = (*env)->GetMethodID(env, module_c, "setStorageInfo", "(ILjava/lang/String;I)V");
	jstring name_s = (*env)->NewStringUTF(env, storage_name);
	(*env)->CallVoidMethod(env, mod->rt->obj, set_storage_info, (int)n_items, name_s, (int)sorted_by);
	(*env)->PopLocalFrame(env, NULL);
	return 0;
}

int pak_rt_add_file_contents(struct Module *mod, struct FileHandle *file, void *image_data, unsigned int length, int is_partial) {
	JNIEnv *env = get_jni_env();
	(*env)->PushLocalFrame(env, 10);
	jobject handle_o = create_filehandle(env, file);
	jclass module_c = (*env)->FindClass(env, "dev/danielc/common/ModuleInstance");
	jmethodID set_storage_info = (*env)->GetMethodID(env, module_c, "setFileContents", "(Ldev/danielc/common/FileHandle;[BZ)V");

	jbyteArray image_data_o = (*env)->NewByteArray(env, (jsize)length);
	(*env)->SetByteArrayRegion(env, image_data_o, 0, (jsize)length, (const jbyte *)image_data);

	(*env)->CallVoidMethod(env, mod->rt->obj, set_storage_info, handle_o, image_data_o, (jboolean)is_partial);
	(*env)->PopLocalFrame(env, NULL);
	return 0;
}

int pak_rt_add_file_thumbnail(struct Module *mod, struct FileHandle *file, void *image_data, unsigned int length) {
	JNIEnv *env = get_jni_env();
	(*env)->PushLocalFrame(env, 10);
	jobject handle_o = create_filehandle(env, file);
	jclass module_c = (*env)->FindClass(env, "dev/danielc/common/ModuleInstance");
	jmethodID set_storage_info = (*env)->GetMethodID(env, module_c, "addFileThumbnail", "(Ldev/danielc/common/FileHandle;[B)V");

	jbyteArray image_data_o = (*env)->NewByteArray(env, (jsize)length);
	(*env)->SetByteArrayRegion(env, image_data_o, 0, (jsize)length, (const jbyte *)image_data);

	(*env)->CallVoidMethod(env, mod->rt->obj, set_storage_info, handle_o, image_data_o);
	(*env)->PopLocalFrame(env, NULL);
	return 0;
}

int pak_rt_set_session_property(struct Module *mod, const char *key, const char *value) {
	JNIEnv *env = get_jni_env();
	(*env)->PushLocalFrame(env, 10);
	jclass module_c = (*env)->FindClass(env, "dev/danielc/common/ModuleInstance");
	jmethodID set_screen_supported = (*env)->GetMethodID(env, module_c, "setProperty", "(Ljava/lang/String;Ljava/lang/String;)V");
	jstring key_s = (*env)->NewStringUTF(env, key);
	jstring value_s = (*env)->NewStringUTF(env, value);
	if (value_s == NULL && (*env)->ExceptionCheck(env)) {
        (*env)->ExceptionClear(env);
		return -1;
    }
	(*env)->CallVoidMethod(env, mod->rt->obj, set_screen_supported, key_s, value_s);
	(*env)->PopLocalFrame(env, NULL);
	return 0;
}

int pak_rt_set_session_property_int(struct Module *mod, const char *key, int value) {
	JNIEnv *env = get_jni_env();
	(*env)->PushLocalFrame(env, 10);
	jclass module_c = (*env)->FindClass(env, "dev/danielc/common/ModuleInstance");
	jmethodID set_screen_supported = (*env)->GetMethodID(env, module_c, "setProperty", "(Ljava/lang/String;I)V");
	jstring key_s = (*env)->NewStringUTF(env, key);
	(*env)->CallVoidMethod(env, mod->rt->obj, set_screen_supported, key_s, value);
	(*env)->PopLocalFrame(env, NULL);
	return 0;
}

int pak_rt_set_dashboard_pane(struct Module *mod, const struct PakUserSetting *s) {
	JNIEnv *env = get_jni_env();
	(*env)->PushLocalFrame(env, 10);
	jclass properties_c = (*env)->FindClass(env, "dev/danielc/common/DashboardPane$Properties");
	jstring name_s = (*env)->NewStringUTF(env, s->name);
	jstring title_s = (*env)->NewStringUTF(env, s->title);
	jobject properties_o = (*env)->NewObject(env, properties_c, (*env)->GetMethodID(env, properties_c, "<init>", "(Ljava/lang/String;Ljava/lang/String;)V"), name_s, title_s);

	jobject pane_o = NULL;
	if (s->type == PAK_BOOLEAN) {
		jclass booleansetting_c = (*env)->FindClass(env, "dev/danielc/common/DashboardPane$BooleanSetting");
		pane_o = (*env)->NewObject(env, booleansetting_c, (*env)->GetMethodID(env, booleansetting_c, "<init>", "(Ldev/danielc/common/DashboardPane$Properties;Z)V"), properties_o, (jboolean)s->u.boolv.v);
	} else if (s->type == PAK_GRAPH) {
		jclass booleansetting_c = (*env)->FindClass(env, "dev/danielc/common/DashboardPane$Graph");
		jintArray arr = (*env)->NewIntArray(env, (jsize)s->u.graphv.n_points);
		(*env)->SetIntArrayRegion(env, arr, 0, (jsize)s->u.graphv.n_points, (const jint *)s->u.graphv.points);
		pane_o = (*env)->NewObject(env, booleansetting_c, (*env)->GetMethodID(env, booleansetting_c, "<init>", "(Ldev/danielc/common/DashboardPane$Properties;[I)V"), properties_o, arr);
	}
	if (pane_o != NULL) {
		jclass module_c = (*env)->FindClass(env, "dev/danielc/common/ModuleInstance");
		jmethodID add_setting = (*env)->GetMethodID(env, module_c, "setDashboardPane", "(Ldev/danielc/common/DashboardPane;)V");
		(*env)->CallVoidMethod(env, mod->rt->obj, add_setting, pane_o);
	}

	(*env)->PopLocalFrame(env, NULL);
	return 0;
}

int pak_rt_add_file_metadata(struct Module *mod, struct FileHandle *file, const struct FileMetadata *metadata) {
	JNIEnv *env = get_jni_env();
	(*env)->PushLocalFrame(env, 10);

	jobject handle_o = create_filehandle(env, file);
	jobject metadata_o = create_filemetadata(env, metadata);

	jclass module_c = (*env)->FindClass(env, "dev/danielc/common/ModuleInstance");
	jmethodID method = (*env)->GetMethodID(env, module_c, "addFileMetadata", "(Ldev/danielc/common/FileHandle;Ldev/danielc/common/FileMetadata;)V");
	(*env)->CallVoidMethod(env, mod->rt->obj, method, handle_o, metadata_o);

	(*env)->PopLocalFrame(env, NULL);
	return 0;
}

int pak_rt_save_session_signature(struct Module *mod, struct PakSavedConnection *info) {
	JNIEnv *env = get_jni_env();
	(*env)->PushLocalFrame(env, 10);

	jbyteArray data = NULL;
	if (info->aux_data != NULL) {
		data = (*env)->NewByteArray(env, (jsize)info->aux_data_length);
		(*env)->SetByteArrayRegion(env, data, 0, (jsize)info->aux_data_length, (const jbyte *)info->aux_data);
	}

	jclass saveddeviceinfo_c = (*env)->FindClass(env, "dev/danielc/common/SavedDeviceInfo");
	jobject obj = (*env)->NewObject(env, saveddeviceinfo_c, (*env)->GetMethodID(env, saveddeviceinfo_c, "<init>", "(Ljava/lang/String;Ljava/lang/String;[B)V"),
		(*env)->NewStringUTF(env, info->unique_id),
		(*env)->NewStringUTF(env, info->name),
		data
	);

	jclass module_c = (*env)->FindClass(env, "dev/danielc/common/ModuleInstance");
	jmethodID method = (*env)->GetMethodID(env, module_c, "saveDeviceSignature", "(Ldev/danielc/common/SavedDeviceInfo;)V");
	(*env)->CallVoidMethod(env, mod->rt->obj, method, obj);

	(*env)->PopLocalFrame(env, NULL);
	return 0;
}

int pak_rt_test_module(struct Module *mod) {
	return 0;
}

static char *strdup_jstring(JNIEnv *env, jstring s_o) {
	const char *s = (*env)->GetStringUTFChars(env, s_o, 0);
	char *s2 = strdup(s);
	(*env)->ReleaseStringUTFChars(env, s_o, s);
	return s2;
}

const char *pak_rt_get_setup_option(struct Module *mod) {
	return mod->rt->setup_option;
}

struct FileMetadata *pak_rt_get_metadata(struct Module *mod, struct FileHandle *file) {
	JNIEnv *env = get_jni_env();
	(*env)->PushLocalFrame(env, 10);
	jobject handle_o = create_filehandle(env, file);
	jclass module_c = (*env)->FindClass(env, "dev/danielc/common/ModuleInstance");
	jmethodID method = (*env)->GetMethodID(env, module_c, "getMetadata", "(Ldev/danielc/common/FileHandle;)Ldev/danielc/common/FileMetadata;");
	jobject md_o = (*env)->CallObjectMethod(env, mod->rt->obj, method, handle_o);
	if (md_o == NULL) {
		(*env)->PushLocalFrame(env, 10);
		return NULL;
	}

	struct FileMetadata *md = malloc(sizeof(struct FileMetadata));

	jclass md_c = (*env)->FindClass(env, "dev/danielc/common/FileMetadata");
	jobject filename_o = (*env)->GetObjectField(env, md_o, (*env)->GetFieldID(env, md_c, "filename", "Ljava/lang/String;"));
	md->filename = strdup_jstring(env, filename_o);
	md->file_size = (*env)->GetIntField(env, md_o, (*env)->GetFieldID(env, md_c, "filesize", "I"));

	(*env)->PopLocalFrame(env, NULL);
	return md;
}

void pak_rt_release_metadata(struct Module *mod, struct FileMetadata *md) {
	free((char *)md->filename);
	free(md);
}

JNIEXPORT int JNICALL
Java_dev_danielc_fudge_AndroidRuntime_setupLibFujiModule(JNIEnv *env, jclass clazz, jobject mod_o) {
	set_jni_env_ctx(env, clazz);
	struct Module *mod = create_module(env, mod_o);
	get_module_libfuji(mod);
	return finalize_module(env, mod);
}

JNIEXPORT int JNICALL
Java_dev_danielc_fudge_AndroidRuntime_setupDummyNativeModule(JNIEnv *env, jclass clazz, jobject mod_o) {
	set_jni_env_ctx(env, clazz);
	struct Module *mod = create_module(env, mod_o);
	get_module_dummy(mod);
	return finalize_module(env, mod);
}

JNIEXPORT int JNICALL
Java_dev_danielc_fudge_AndroidRuntime_setupWebassemblyModule(JNIEnv *env, jclass clazz, jobject mod_o, jbyteArray fileContents) {
	set_jni_env_ctx(env, clazz);
	struct Module *mod = create_module(env, mod_o);
	jbyte *buf = (*env)->GetByteArrayElements(env, fileContents, NULL);
	jsize size = (*env)->GetArrayLength(env, fileContents);
	int rc = setup_wasm_module(mod, (char *)buf, (unsigned int)size);
	(*env)->ReleaseByteArrayElements(env, fileContents, buf, 0);
	if (rc) return rc;
	return finalize_module(env, mod);
	return -1;
}

JNIEXPORT int JNICALL
Java_dev_danielc_fudge_AndroidRuntime_setupJavascriptModule(JNIEnv *env, jclass clazz, jobject mod_o, jbyteArray fileContents) {
	set_jni_env_ctx(env, clazz);
	struct Module *mod = create_module(env, mod_o);
	jbyte *buf = (*env)->GetByteArrayElements(env, fileContents, NULL);
	jsize size = (*env)->GetArrayLength(env, fileContents);
	int rc = setup_quickjs_module(mod, (char *)buf, (unsigned int)size);
	(*env)->ReleaseByteArrayElements(env, fileContents, buf, 0);
	if (rc) return rc;
	return finalize_module(env, mod);
}

JNIEXPORT jint JNICALL
Java_dev_danielc_fudge_AndroidRuntime_setupCmfNothingAudioModule(JNIEnv *env, jclass clazz, jobject mod_o) {
	set_jni_env_ctx(env, clazz);
	struct Module *mod = create_module(env, mod_o);
	get_module_cmfnothingaudio(mod);
	return finalize_module(env, mod);
}

JNIEXPORT jint JNICALL
Java_dev_danielc_fudge_AndroidRuntime_setupGoveeLifeModule(JNIEnv *env, jobject clazz, jobject mod_o) {
	set_jni_env_ctx(env, clazz);
	struct Module *mod = create_module(env, mod_o);
	get_module_goveelife(mod);
	return finalize_module(env, mod);
}