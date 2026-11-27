cmake_minimum_required(VERSION 3.22.1)

project("horizonplayer_native" C CXX)

option(HORIZONPLAYER_ENABLE_YTDLP "Enable yt-dlp native bindings" OFF)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

add_link_options("-Wl,-z,max-page-size=16384")

include(FetchContent)
FetchContent_Declare(
    oboe
    GIT_REPOSITORY https://github.com/google/oboe.git
    GIT_TAG 1.10.0
    GIT_SHALLOW TRUE
)
FetchContent_MakeAvailable(oboe)

add_library(
    horizonplayer_audio
    SHARED
    DirectOutputBridge.cpp
    DirectOutputStream.cpp
    ParametricEq.cpp
    ParametricEqBridge.cpp
)

target_include_directories(
    horizonplayer_audio
    PRIVATE
    "${CMAKE_CURRENT_SOURCE_DIR}"
)

target_compile_options(
    horizonplayer_audio
    PRIVATE
    $<$<CONFIG:Debug>:-O2>
    -Wall
    -Wextra
)

target_link_libraries(
    horizonplayer_audio
    PRIVATE
    oboe
    android
    log
)
