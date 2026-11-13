#include <jni.h>

// 由 NDK 在编译期按目标 ABI 定义，报告这份 .so 自身所属的架构。
// 与 Build.SUPPORTED_ABIS 的区别：后者是设备支持的架构，这里是包管理器实际解出来的那一份。
static const char *self_abi(void) {
#if defined(__aarch64__)
    return "arm64-v8a";
#elif defined(__arm__)
    return "armeabi-v7a";
#elif defined(__x86_64__)
    return "x86_64";
#elif defined(__i386__)
    return "x86";
#else
    return "unknown";
#endif
}

JNIEXPORT jstring JNICALL
Java_com_host1syan_claudecn_NativeAbi_nativeAbi(JNIEnv *env, jclass type) {
    (void) type;
    return (*env)->NewStringUTF(env, self_abi());
}
