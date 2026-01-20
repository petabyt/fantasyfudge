__thread struct AndroidLocal local = {0, 0};

void set_jni_env_ctx(JNIEnv *env, jobject ctx) {
	local.env = env;
	local.ctx = ctx;
}

struct AndroidLocal push_jni_env_ctx(JNIEnv *env, jobject ctx) {
	struct AndroidLocal l;
	l.env = local.env;
	l.ctx = local.ctx;
	local.env = env;
	local.ctx = ctx;
	return l;
}

void pop_jni_env_ctx(struct AndroidLocal l) {
	set_jni_env_ctx(l.env, l.ctx);
}

JNIEnv *get_jni_env(void) {
	if (local.env == NULL) {
		plat_dbg("JNIEnv not set for this thread");
		abort();
	}

	return local.env;
}

jobject get_jni_ctx(void) {
	if (local.ctx == NULL) {
		plat_dbg("ctx not set for this thread");
		abort();
	}

	return local.ctx;
}
