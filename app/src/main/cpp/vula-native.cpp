#include <jni.h>
#include <string>
#include "sha256.h"

extern "C" JNIEXPORT jstring JNICALL
Java_com_vula_app_core_util_HashUtils_hashNetworkId(
        JNIEnv* env,
        jobject /* this */,
        jstring input) {
        
    // Convert jstring to std::string
    const char *nativeString = env->GetStringUTFChars(input, 0);
    std::string inputStr = std::string(nativeString);
    env->ReleaseStringUTFChars(input, nativeString);

    // Compute SHA-256 hash using our lightweight implementation
    std::string hashedStr = sha256(inputStr);

    // Return the hashed string to Kotlin
    return env->NewStringUTF(hashedStr.c_str());
}
