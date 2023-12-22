# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html.
# For more examples on how to use CMake, see https://github.com/android/ndk-samples.

# Sets the minimum CMake version required for this project.
cmake_minimum_required(VERSION 3.22.1)
project("jnidemo")
add_compile_options(-fvisibility=hidden) # 隐藏符号
add_library(${CMAKE_PROJECT_NAME} SHARED
        native-lib.c)
#target_link_libraries(${CMAKE_PROJECT_NAME})
target_link_libraries(${CMAKE_PROJECT_NAME} PRIVATE android log)
target_link_libraries(${CMAKE_PROJECT_NAME} PRIVATE -Wl,--exclude-libs,ALL)