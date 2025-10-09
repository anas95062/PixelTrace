package com.anas.pixeltrace;

public class NativeBridge {
    static {
        System.loadLibrary("pixeltrace");
    }

    // Processes the frame and saves the result internally
    public static native void processFrame(byte[] data, int width, int height);

    // Uploads the last processed frame to the texture
    public static native void updateTexture(int textureId);

    // --- ADD THIS NEW METHOD ---
    // Tells the C++ code to toggle the filter on or off
    public static native void toggleFilter();
    // ---------------------------
}