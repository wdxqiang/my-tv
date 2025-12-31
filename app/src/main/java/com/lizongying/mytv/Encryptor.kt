package com.lizongying.mytv

import android.content.Context

class Encryptor {
    external fun init(context: Context)

    external fun encrypt(t: String, e: String, r: String, n: String, i: String): String

    external fun hash(data: ByteArray): ByteArray?

    external fun hash2(data: ByteArray): ByteArray?

    companion object {
        var isLibraryLoaded = false
        init {
            try {
                System.loadLibrary("native")
                isLibraryLoaded = true
            } catch (e: UnsatisfiedLinkError) {
                // Library loading failed, but we'll handle this gracefully
                isLibraryLoaded = false
            }
        }
    }
}