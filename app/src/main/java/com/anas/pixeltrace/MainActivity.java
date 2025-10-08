package com.anas.pixeltrace;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    // Views aur variables define kar rahe hain
    private PreviewView viewFinder;
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewFinder = findViewById(R.id.viewFinder);

        // Check kar rahe hain ki camera permission hai ya nahi
        if (allPermissionsGranted()) {
            startCamera(); // Agar permission hai to camera shuru karo
        } else {
            // Agar nahi hai, to user se permission maango
            ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    // Camera shuru karne ka function
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // 1. Preview use case setup
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                // 2. ImageAnalysis use case setup

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), imageProxy -> {
                    // Yahaan par har frame ka data milega
                    // YUV_420_888 format se Y plane (grayscale data) ka byte array nikal rahe hain
                    java.nio.ByteBuffer yBuffer = imageProxy.getPlanes()[0].getBuffer();
                    int ySize = yBuffer.remaining();
                    byte[] yBytes = new byte[ySize];
                    yBuffer.get(yBytes);

// Native method ko call kar rahe hain
                    NativeBridge.processFrame(yBytes, imageProxy.getWidth(), imageProxy.getHeight());

                    imageProxy.close();
                });

                // Peeche wala camera select kar rahe hain
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                // Sab kuch unbind karke dobara bind kar rahe hain
                cameraProvider.unbindAll();

                // Dono (preview aur imageAnalysis) ko ek saath bind karein
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Error starting camera", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // Yeh function check karta hai ki permission di gayi hai ya nahi
    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(
                    getBaseContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    // Yeh function tab call hota hai jab user permission allow ya deny karta hai
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera(); // Agar permission mil gayi to camera shuru karo
            } else {
                // Agar nahi mili to message dikhao aur app band kar do
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}