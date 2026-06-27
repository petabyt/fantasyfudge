#pragma once
#include <jni.h>
#include <pak.h>
#include <runtime.h>

struct RuntimePriv {
	jobject obj;
	char *setup_option;
};

struct ModuleJavaStruct {
	struct Module *ptr;
};