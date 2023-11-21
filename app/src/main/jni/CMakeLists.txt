cmake_minimum_required(VERSION 3.18.1)

project("rtmp_play")

#add h files
include_directories(${CMAKE_SOURCE_DIR}/head)
include_directories(${CMAKE_SOURCE_DIR}/head/x264)
message(WARNING CMAKE_SOURCE_DIR---->${CMAKE_SOURCE_DIR})

#sub directory must include cmake list txt files
#add_subdirectory(./cpp/x264)
file(GLOB source_files ${CMAKE_SOURCE_DIR}/cpp/*.cpp)
file(GLOB source_files_1 ${CMAKE_SOURCE_DIR}/cpp/x264/*.cpp)
message(WARNING ${source_files})

# local files compile to so lib
add_library(
        rtmp_play
        SHARED
        ${source_files}
        ${source_files_1} )

# add system lib
find_library(
        log-lib
        log)

# add third libs
add_library(x264
        SHARED
        IMPORTED)
# load third lib by abs path
set_target_properties(x264
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/libs/x264/${ANDROID_ABI}/libx264.so)

# link local , system ,third libs
target_link_libraries(
        rtmp_play
        x264
        ${log-lib})