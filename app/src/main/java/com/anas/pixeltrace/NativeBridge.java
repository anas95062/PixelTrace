package com.anas.pixeltrace;

public class NativeBridge {
    static {
        // "pixeltrace" aapke native library ka naam hai
        System.loadLibrary("pixeltrace");
    }

    /**
     * Yeh native method C++ mein implement kiya jaayega.
     */
    public static native void processFrame(byte[] data, int width, int height);
}