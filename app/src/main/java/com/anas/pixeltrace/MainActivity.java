package com.anas.pixeltrace;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.opengl.GLSurfaceView; // Import GLSurfaceView
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import com.anas.pixeltrace.gl.MyGLRenderer; // Import your renderer
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private GLSurfaceView glSurfaceView;
    private MyGLRenderer renderer;
    private TextView fpsTextView;
    private Button toggleButton;
    private int frameCount = 0;
    private long lastFpsTimestamp = 0;
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fpsTextView = findViewById(R.id.fps_text_view);

        glSurfaceView = findViewById(R.id.glSurfaceView);

        toggleButton = findViewById(R.id.toggle_button);
        toggleButton.setOnClickListener(v -> {
            // Call the native toggle method when the button is clicked
            NativeBridge.toggleFilter();
        });

        // Configure GLSurfaceView for OpenGL ES 2.0
        glSurfaceView.setEGLContextClientVersion(2);
        renderer = new MyGLRenderer();
        glSurfaceView.setRenderer(renderer);

        // Render only when new data is available
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        // Check for camera permission
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), imageProxy -> {

                    // --- ADD FPS CALCULATION LOGIC ---
                    frameCount++;
                    long currentTime = System.currentTimeMillis();
                    if (lastFpsTimestamp == 0) {
                        lastFpsTimestamp = currentTime;
                    }
                    if (currentTime - lastFpsTimestamp >= 1000) { // Update every second
                        final int fps = frameCount;
                        runOnUiThread(() -> fpsTextView.setText("FPS: " + fps));
                        frameCount = 0;
                        lastFpsTimestamp = currentTime;
                    }

                    java.nio.ByteBuffer yBuffer = imageProxy.getPlanes()[0].getBuffer();
                    byte[] yBytes = new byte[yBuffer.remaining()];
                    yBuffer.get(yBytes);

                    // Call the new, simpler processFrame method
                    NativeBridge.processFrame(yBytes, imageProxy.getWidth(), imageProxy.getHeight());

                    // Request a redraw (this is still important)
                    glSurfaceView.requestRender();

                    imageProxy.close();
                });

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();

                // IMPORTANT: Only bind the ImageAnalysis use case
                cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Error starting camera", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (glSurfaceView != null) {
            glSurfaceView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (glSurfaceView != null) {
            glSurfaceView.onPause();
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(
                    getBaseContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}