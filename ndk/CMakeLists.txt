cmake_minimum_required(VERSION 3.10)
set(CMAKE_EXPORT_COMPILE_COMMANDS 1)
project(pakit)

option(PAK_INCLUDE_CLI "cli program" OFF)
option(PAK_INCLUDE_WIFI "WiFi APIs" ON)
option(PAK_INCLUDE_BT "Bluetooth APIs" ON)
add_subdirectory(${CMAKE_CURRENT_SOURCE_DIR}/../libpak ${CMAKE_CURRENT_BINARY_DIR}/libpak)

# libjpeg-turbo
add_subdirectory(${CMAKE_CURRENT_SOURCE_DIR}/../third_party/libjpeg-turbo ${CMAKE_CURRENT_BINARY_DIR}/libjpeg)

# quickjs
option(QJS_BUILD_LIBC "Build standard library modules as part of the library" ON)
add_subdirectory(${CMAKE_CURRENT_SOURCE_DIR}/../third_party/quickjs ${CMAKE_CURRENT_BINARY_DIR}/quickjs)

add_library(${CMAKE_PROJECT_NAME} SHARED
    main.c
)
target_link_libraries(${CMAKE_PROJECT_NAME} PRIVATE pak qjs libjpeg-turbo)

target_link_options(pakit PRIVATE "-Wl,-z,max-page-size=16384")
target_link_options(pak PRIVATE "-Wl,-z,max-page-size=16384")
target_link_options(qjs PRIVATE "-Wl,-z,max-page-size=16384")
target_link_options(libjpeg-turbo PRIVATE "-Wl,-z,max-page-size=16384")