package com.anas.pixeltrace;

public class NativeBridge {
    static {
        System.loadLibrary("pixeltrace");
    }

    // 1. Processes the frame and saves the result internally
    public static native void processFrame(byte[] data, int width, int height);

    // 2. Uploads the last processed frame to the texture
    public static native void updateTexture(int textureId);
}