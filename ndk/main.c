#include <stdlib.h>
#include <jni.h>
#include <stdarg.h>
#include <stdio.h>
#include <pak.h>
#include <runtime.h>
#include "thread.h"
#include "main.h"
#include <runtime_ext.h>

struct RuntimePriv {
	jobject obj;
};

int get_module_dummy(struct Module *mod);
int setup_quickjs_module(struct Module **mod, const char *filename);
int get_module_libfuji(struct Module *mod);
int get_module_cmfnothingaudio(struct Module *mod);

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

	mod->bt = pak_bt_get_context();
	mod->net = pak_net_get_context();

	return rc;
}

JNIEXPORT void JNICALL
Java_dev_danielc_common_NativeRuntime_init(JNIEnv *env, jclass clazz) {
	set_jni_env_ctx(env, clazz);
	pak_global_log("NativeRuntime init");
}

static uint8_t *file_add(void *arg, const uint8_t *buffer, unsigned int new_len, unsigned int old_len) {
	uint8_t *new = realloc(buffer, new_len);
	fread(new + old_len, 1, new_len - old_len, (FILE *)arg);
	return new;
}

JNIEXPORT jbyteArray JNICALL
Java_dev_danielc_common_Exif_getExifThumbnail(JNIEnv *env, jclass clazz, jstring filepath) {
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

	jbyteArray result = (*env)->NewByteArray(env, (int)c.thumb_size);
	(*env)->SetByteArrayRegion(env, result, 0, (int)c.thumb_size, (jbyte *)(c.buf + c.thumb_of));

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
	jclass backend_c = (*env)->FindClass(env, "dev/danielc/common/NativeRuntime");
	jmethodID log_global_m = (*env)->GetStaticMethodID(env, backend_c, "logGlobalLine", "(Ljava/lang/String;)V");
	(*env)->CallStaticVoidMethod(env, backend_c, log_global_m, buffer_s);
	(*env)->PopLocalFrame(env, NULL);
}

int pak_rt_set_tick_interval(struct Module *mod, unsigned int us) {
	JNIEnv *env = get_jni_env();
	(*env)->PushLocalFrame(env, 10);
	jclass module_c = (*env)->FindClass(env, "dev/danielc/common/NativeModule");
	jmethodID method = (*env)->GetMethodID(env, module_c, "setCurrentTickIntervalUs", "(I)V");
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
	(*env)->PushLocalFrame(env, 10);
	jstring buffer_s = (*env)->NewStringUTF(env, buffer);
	jclass module_c = (*env)->FindClass(env, "dev/danielc/common/NativeModule");
	jmethodID debug_log_m = (*env)->GetMethodID(env, module_c, "debugLog", "(Ljava/lang/String;)V");
	(*env)->CallVoidMethod(env, mod->rt->obj, debug_log_m, buffer_s);
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
	(*env)->PushLocalFrame(env, 10);
	jclass metadata_c = (*env)->FindClass(env, "dev/danielc/common/FileMetadata");
	jmethodID constructor = (*env)->GetMethodID(env, metadata_c, "<init>", "(Ljava/lang/String;Ljava/lang/String;II)V");
	jobject handle_o = (*env)->NewObject(env, metadata_c, constructor,
		(*env)->NewStringUTF(env, meta->filename),
		(*env)->NewStringUTF(env, meta->mime_type),
		0,
		0
	);
	return (*env)->PopLocalFrame(env, handle_o);
}



int pak_rt_set_screen_supported(struct Module *mod, int screen, int v) {
	JNIEnv *env = get_jni_env();
	jclass module_c = (*env)->FindClass(env, "dev/danielc/common/NativeModule");
	jmethodID set_screen_supported = (*env)->GetMethodID(env, module_c, "setScreenSupported", "(IZ)V");
	(*env)->CallVoidMethod(env, mod->rt->obj, set_screen_supported, screen, (jboolean)v);
	return 0;
}

int pak_rt_is_job_cancelled(struct Module *mod, int job) {
	JNIEnv *env = get_jni_env();
	jclass module_c = (*env)->FindClass(env, "dev/danielc/common/NativeModule");
	jmethodID is_job_cancelled = (*env)->GetMethodID(env, module_c, "isJobCancelled", "(I)Z");
	return (*env)->CallBooleanMethod(env, mod->rt->obj, is_job_cancelled, job);
}

int pak_rt_set_progress_bar(struct Module *mod, int job, int percent) {
	JNIEnv *env = get_jni_env();
	jclass module_c = (*env)->FindClass(env, "dev/danielc/common/NativeModule");
	jmethodID set_progress_bar = (*env)->GetMethodID(env, module_c, "setProgressBar", "(II)V");
	(*env)->CallVoidMethod(env, mod->rt->obj, set_progress_bar, job, percent);
	return 0;
}

int pak_rt_set_storage_info(struct Module *mod, const char *storage_name, unsigned int n_items, enum SortedBy sorted_by) {
	JNIEnv *env = get_jni_env();
	(*env)->PushLocalFrame(env, 10);
	jclass module_c = (*env)->FindClass(env, "dev/danielc/common/NativeModule");
	jmethodID set_storage_info = (*env)->GetMethodID(env, module_c, "setStorageInfo", "(ILjava/lang/String;I)V");
	jstring name_s = (*env)->NewStringUTF(env, storage_name);
	(*env)->CallVoidMethod(env, mod->rt->obj, set_storage_info, (int)n_items, name_s, (int)sorted_by);
	(*env)->PopLocalFrame(env, NULL);
	return 0;
}

int pak_rt_add_file_contents(struct Module *mod, struct FileHandle *file, void *image_data, unsigned int length) {
	JNIEnv *env = get_jni_env();
	(*env)->PushLocalFrame(env, 10);
	jobject handle_o = create_filehandle(env, file);
	jclass module_c = (*env)->FindClass(env, "dev/danielc/common/NativeModule");
	jmethodID set_storage_info = (*env)->GetMethodID(env, module_c, "setFileContents", "(Ldev/danielc/common/FileHandle;[B)V");

	jbyteArray image_data_o = (*env)->NewByteArray(env, (jsize)length);
	(*env)->SetByteArrayRegion(env, image_data_o, 0, (jsize)length, (const jbyte *)image_data);

	(*env)->CallVoidMethod(env, mod->rt->obj, set_storage_info, handle_o, image_data_o);
	(*env)->PopLocalFrame(env, NULL);
	return 0;
}

int pak_rt_add_file_thumbnail(struct Module *mod, struct FileHandle *file, void *image_data, unsigned int length) {
	JNIEnv *env = get_jni_env();
	(*env)->PushLocalFrame(env, 10);
	jobject handle_o = create_filehandle(env, file);
	jclass module_c = (*env)->FindClass(env, "dev/danielc/common/NativeModule");
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
	jmethodID constructor = (*env)->GetMethodID(env, setting_c, "<init>",
												"(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Boolean;Ljava/lang/Integer;Ljava/lang/String;Ljava/lang/Integer;Ljava/lang/Integer;Ljava/util/List;)V");
	jstring name_s = (*env)->NewStringUTF(env, s->name);
	jstring title_s = (*env)->NewStringUTF(env, s->title);
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
	jobject setting_o = (*env)->NewObject(env, setting_c, constructor, name_s, title_s, boolv, intv, stringv, min_o, max_o, dropdownlist_o);

	jclass module_c = (*env)->FindClass(env, "dev/danielc/common/NativeModule");
	jmethodID add_setting = (*env)->GetMethodID(env, module_c, "addUserSetting", "(Ldev/danielc/common/UserSetting;)V");
	(*env)->CallVoidMethod(env, mod->rt->obj, add_setting, setting_o);

	(*env)->PopLocalFrame(env, NULL);
	return 0;
}

int pak_rt_add_file_metadata(struct Module *mod, struct FileHandle *file, const struct FileMetadata *metadata) {
	JNIEnv *env = get_jni_env();
	(*env)->PushLocalFrame(env, 10);

	jobject handle_o = create_filehandle(env, file);
	jobject metadata_o = create_filemetadata(env, metadata);

	jclass module_c = (*env)->FindClass(env, "dev/danielc/common/NativeModule");
	jmethodID method = (*env)->GetMethodID(env, module_c, "addFileMetadata", "(Ldev/danielc/common/FileHandle;Ldev/danielc/common/FileMetadata;)V");
	(*env)->CallVoidMethod(env, mod->rt->obj, method, handle_o, metadata_o);

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

JNIEXPORT jint JNICALL
Java_dev_danielc_common_NativeRuntime_setupCmfNothingAudioModule(JNIEnv *env, jclass clazz,
																 jobject mod, jobject manifest) {
	set_jni_env_ctx(env, clazz);
	return pak_ndk_create_module(env, mod, get_module_cmfnothingaudio, manifest);
}