#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <runtime.h>
#include <bluetooth.h>

/*
Unknown Service
494e5445-4c4c-495f-524f-434b535f4857
PRIMARY SERVICE
*/

static int init(struct Module *mod) {
	pak_debug_log(mod, "goveelife init");
	pak_rt_set_tick_interval(mod, 1000 * 1000);
	return 0;
}

static int on_find_connection(struct Module *mod, int job) {
	struct PakBtAdapter adapter;
	pak_bt_get_adapter(mod->bt, &adapter, 0);
	struct PakBtDevice device;
	int rc = pak_bt_get_device(mod->bt, &adapter, &device, 1, PAK_FILTER_BONDED);
	if (rc) return PAK_ERR_NO_CONNECTION;

	pak_debug_log(mod, "%s", device.name);

	struct PakGattService service;
	for (int i = 0; pak_bt_get_gatt_service(mod->bt, &device, &service, i) == 0; i++) {
		pak_debug_log(mod, "dev: %s", service.uuid);
		pak_bt_unref_gatt_service(mod->bt, &service);
	}

	pak_bt_unref_device(mod->bt, &device);

	return PAK_ERR_NO_CONNECTION;
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

int get_module_goveelife(struct Module *mod) {
	mod->init = init;
	mod->on_find_connection = on_find_connection;
	mod->on_idle_tick = on_idle_tick;
	mod->on_disconnect = on_disconnect;
	mod->on_switch_screen = on_switch_screen;
	return 0;
}

