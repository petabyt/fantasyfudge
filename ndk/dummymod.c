#include <stdlib.h>
#include <runtime.h>
#include <wifi.h>

struct ModulePriv {
	int x;
};

static int on_find_connection(struct Module *mod, int job) {
	return 0;
}

static int init(struct Module *mod) {
	mod->priv = (struct ModulePriv *)malloc(sizeof(struct ModulePriv));
	pak_global_log("Hello from module");
	return 0;
}

static int on_try_connect_wifi(struct Module *mod, struct PakWiFiAdapter *handle, int job) {
	return 0;
}

static int on_idle_tick(struct Module *mod, unsigned int us_since_last_tick) {
	return 0;
}

static int on_disconnect(struct Module *mod) {
	return 0;
}

static int on_switch_screen(struct Module *mod, int old_screen, int new_screen, int job) {
	return 0;
}

static int on_request_file_contents(struct Module *mod, int screen, int job, struct FileHandle *file) {
	return 0;
}

static int on_request_thumbnail(struct Module *mod, int screen, int job, struct FileHandle *file) {
	return 0;
}

static int on_request_file_metadata(struct Module *mod, int screen, int job, struct FileHandle *file) {
	return 0;
}

static int on_run_test(struct Module *mod, int screen, int job) {
	return 0;
}

static int on_custom_command(struct Module *mod, const char *request) {
	return 0;
}

int get_module_dummy(struct Module *mod) {
	mod->init = init;
	mod->on_try_connect_wifi = on_try_connect_wifi;
	mod->on_idle_tick = on_idle_tick;
	mod->on_disconnect = on_disconnect;
	mod->on_switch_screen = on_switch_screen;
	return 0;
}
