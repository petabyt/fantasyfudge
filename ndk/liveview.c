#include <android/log.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <inttypes.h>
#include <stdlib.h>
#include <stdio.h>
//#define JPEG_LIB_VERSION 80
//typedef unsigned char JSAMPLE;
#include <jinclude.h>
#include <jpeglib.h>
#include <jerror.h>

METHODDEF(void)my_error_exit(j_common_ptr cinfo) {
	struct jpeg_error_mgr *err = cinfo->err;
	char buffer[1024];
	err->format_message(cinfo, buffer);
	__android_log_write(ANDROID_LOG_ERROR, "libjpeg", buffer);
}

int render_liveview_jpeg(const unsigned char *input, size_t size, uint8_t *framebuffer, unsigned int screen_width, unsigned int screen_height) {
	struct jpeg_decompress_struct cinfo;
	struct jpeg_error_mgr jerr;

	cinfo.err = jpeg_std_error(&jerr);
	jerr.error_exit = my_error_exit;
	jpeg_create_decompress(&cinfo);

	jpeg_mem_src(&cinfo, input, size);

	// Note: not necessary on every frame
	jpeg_read_header(&cinfo, TRUE);

	cinfo.out_color_space = JCS_EXT_RGBA;

	unsigned int y = 0;
	unsigned int x = 0;
	if (screen_width < screen_height) {
		// Scale image height to aspect ratio (hacky math, shouldn't overflow though)
		//y = (screen_height / 2) - (cinfo.image_height * screen_width / cinfo.image_width / 2);
		cinfo.scale_num = screen_height;
		cinfo.scale_denom = cinfo.image_height;
	} else {
		cinfo.scale_num = screen_width;
		cinfo.scale_denom = cinfo.image_width;

		//x = (screen_width / 2) - (cinfo.image_width);
	}

	jpeg_start_decompress(&cinfo);

	while (cinfo.output_scanline < cinfo.output_height) {
		// Read scanlines directly into framebuffer
		unsigned char *buffer_array[1] = {
			framebuffer + (y * (screen_width * 4)) + (x * 4)
		};
		jpeg_read_scanlines(&cinfo, (JSAMPARRAY)buffer_array, 1);
		if (y <= (screen_height - 2)) y++;
	}

	jpeg_finish_decompress(&cinfo);
	jpeg_destroy_decompress(&cinfo);

	return 0;
}

int liveview_render_frame(JNIEnv *env, jobject surface_handle, void *buffer, unsigned int size) {
	if (surface_handle == NULL) {
		__android_log_write(ANDROID_LOG_ERROR, "liveview", "surface handle is null");
		abort();
	}
	ANativeWindow *window = ANativeWindow_fromSurface(env, surface_handle);
	if (window == NULL) {
		__android_log_write(ANDROID_LOG_ERROR, "liveview", "ANativeWindow_fromSurface");
		abort();
	}
	ANativeWindow_acquire(window);

	ANativeWindow_setBuffersGeometry(window, 0, 0, AHARDWAREBUFFER_FORMAT_R8G8B8A8_UNORM);

	ANativeWindow_Buffer winbuf;
	ARect bounds = {0, 0, 1, 1};
	if (ANativeWindow_lock(window, &winbuf, &bounds)) {
		__android_log_write(ANDROID_LOG_ERROR, "liveview", "failed to lock window");
		abort();
	}

	// Assume buffer is jpeg
	render_liveview_jpeg(buffer, size, winbuf.bits, winbuf.stride, winbuf.height);

	if (ANativeWindow_unlockAndPost(window)) {
		__android_log_write(ANDROID_LOG_ERROR, "liveview", "failed to unlock window");
		abort();
	}
	return 0;
}