compile_libfuji:
	cmake -D_WASI_EMULATED_GETPID=yes -DCMAKE_TOOLCHAIN_FILE=/home/daniel/Downloads/wasi-sdk-29.0-x86_64-linux/share/cmake/wasi-sdk-pthread.cmake -G Ninja -B libfuji/build libfuji
	cmake --build libfuji/build
