#include <jni.h>

jstring Java_com_dc_gm_RandomJNI_ranluxppFactoryTesting(JNIEnv *env, jclass clazz) {
    return (*env)->NewStringUTF(env, "hello from c++");
}

JNINativeMethod gMethods[] = {
        {"stringFromJNI", "()Ljava/lang/String;", (void *)Java_com_dc_gm_RandomJNI_ranluxppFactoryTesting}
};

jstring gClassName = "com/test/jnidemo/MainActivity";

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env = NULL;
    if (((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_6) != JNI_OK)) return -1;
    jclass nativeClass = (*env)->FindClass(env, gClassName);
    if (nativeClass == NULL) return -1;
    jint count = sizeof(gMethods)/ sizeof(gMethods[0]);
    if (((*env)->RegisterNatives(env, nativeClass, gMethods, count) < 0)) return -1;
    return JNI_VERSION_1_6;
}