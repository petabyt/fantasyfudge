OUT_DIR := ../app/src/main/assets

compile_libfuji:
	cmake -DCMAKE_TOOLCHAIN_FILE=../wasm.cmake -G Ninja -B libfuji/build libfuji
	cmake --build libfuji/build
install_libfuji: compile_libfuji
	cp flibfuji.json $(OUT_DIR)/
