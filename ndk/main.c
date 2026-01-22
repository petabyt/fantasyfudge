#include <quickjs.h>
#include <quickjs-libc.h>
#include <jni.h>

JNIEXPORT void JNICALL
Java_dev_danielc_common_NativeBackend_Foo(JNIEnv *env, jclass clazz) {
	JSRuntime *rt = JS_NewRuntime();

	JSContext *ctx = JS_NewContext(rt);
	js_std_add_helpers(ctx, 0, NULL);

	js_init_module_std(ctx, "qjs:std");
	//JS_SetModuleLoaderFunc(rt, NULL, js_module_loader, NULL);
	js_std_init_handlers(rt);

	const char buffer[] = "console.log('asd');";

	JSValue val = JS_Eval(ctx, buffer, sizeof(buffer), "main.js", JS_EVAL_TYPE_MODULE);
	if (JS_IsException(val)) {
		const char *str = JS_ToCString(ctx, val);
		printf("JS error: %s\n", str);
		JS_FreeCString(ctx, str);
		abort();
	}

	printf("Return value: %d\n", JS_VALUE_GET_TAG(val));

	JS_FreeValue(ctx, val);

	js_std_free_handlers(rt);
	JS_FreeContext(ctx);
	JS_FreeRuntime(rt);
}