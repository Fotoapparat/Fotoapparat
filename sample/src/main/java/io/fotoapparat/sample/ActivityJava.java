package io.fotoapparat.sample;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import io.fotoapparat.Fotoapparat;
import io.fotoapparat.capability.Capabilities;
import io.fotoapparat.configuration.CameraConfiguration;
import io.fotoapparat.configuration.UpdateConfiguration;
import io.fotoapparat.error.CameraErrorListener;
import io.fotoapparat.exception.camera.CameraException;
import io.fotoapparat.parameter.ScaleType;
import io.fotoapparat.parameter.Zoom;
import io.fotoapparat.preview.Frame;
import io.fotoapparat.preview.FrameProcessor;
import io.fotoapparat.result.BitmapPhoto;
import io.fotoapparat.result.PhotoResult;
import io.fotoapparat.result.WhenDoneListener;
import io.fotoapparat.view.CameraView;
import io.fotoapparat.view.FocusView;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import static io.fotoapparat.log.LoggersKt.fileLogger;
import static io.fotoapparat.log.LoggersKt.logcat;
import static io.fotoapparat.log.LoggersKt.loggers;
import static io.fotoapparat.result.transformer.ResolutionTransformersKt.scaled;
import static io.fotoapparat.selector.AspectRatioSelectorsKt.standardRatio;
import static io.fotoapparat.selector.FlashSelectorsKt.autoFlash;
import static io.fotoapparat.selector.FlashSelectorsKt.autoRedEye;
import static io.fotoapparat.selector.FlashSelectorsKt.off;
import static io.fotoapparat.selector.FlashSelectorsKt.torch;
import static io.fotoapparat.selector.FocusModeSelectorsKt.autoFocus;
import static io.fotoapparat.selector.FocusModeSelectorsKt.continuousFocusPicture;
import static io.fotoapparat.selector.FocusModeSelectorsKt.fixed;
import static io.fotoapparat.selector.LensPositionSelectorsKt.back;
import static io.fotoapparat.selector.LensPositionSelectorsKt.front;
import static io.fotoapparat.selector.PreviewFpsRangeSelectorsKt.highestFps;
import static io.fotoapparat.selector.ResolutionSelectorsKt.highestResolution;
import static io.fotoapparat.selector.SelectorsKt.firstAvailable;
import static io.fotoapparat.selector.SensorSensitivitySelectorsKt.highestSensorSensitivity;

public class ActivityJava extends AppCompatActivity {

    private static final String LOGGING_TAG = "Fotoapparat Example";

    private final PermissionsDelegate permissionsDelegate = new PermissionsDelegate(this);
    private boolean hasCameraPermission;
    private CameraView cameraView;
    private FocusView focusView;
    private TextView zoomLvl;
    private ImageView switchCamera;
    private View capture;

    private Fotoapparat fotoapparat;
    private Zoom.VariableZoom cameraZoom;
    private float curZoom = 0f;

    boolean activeCameraBack = true;

    private CameraConfiguration cameraConfiguration = CameraConfiguration
            .builder()
            .photoResolution(standardRatio(
                    highestResolution()
            ))
            .focusMode(firstAvailable(
                    continuousFocusPicture(),
                    autoFocus(),
                    fixed()
            ))
            .flash(firstAvailable(
                    autoRedEye(),
                    autoFlash(),
                    torch(),
                    off()
            ))
            .previewFpsRange(highestFps())
            .sensorSensitivity(highestSensorSensitivity())
            .frameProcessor(new SampleFrameProcessor())
            .build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraView = findViewById(R.id.cameraView);
        focusView = findViewById(R.id.focusView);
        capture = findViewById(R.id.capture);
        zoomLvl = findViewById(R.id.zoomLvl);
        switchCamera = findViewById(R.id.switchCamera);
        hasCameraPermission = permissionsDelegate.hasCameraPermission();

        if (hasCameraPermission) {
            cameraView.setVisibility(View.VISIBLE);
        } else {
            permissionsDelegate.requestCameraPermission();
        }

        fotoapparat = createFotoapparat();

        takePictureOnClick();
        switchCameraOnClick();
        toggleTorchOnSwitch();
    }

    private Fotoapparat createFotoapparat() {
        return Fotoapparat
                .with(this)
                .into(cameraView)
                .focusView(focusView)
                .previewScaleType(ScaleType.CenterCrop)
                .lensPosition(back())
                .frameProcessor(new SampleFrameProcessor())
                .logger(loggers(
                        logcat(),
                        fileLogger(this)
                ))
                .cameraErrorCallback(new CameraErrorListener() {
                    @Override
                    public void onError(@NotNull CameraException e) {
                        Toast.makeText(ActivityJava.this, e.toString(), Toast.LENGTH_LONG).show();
                    }
                })
                .build();
    }

    private void adjustViewsVisibility() {
        fotoapparat.getCapabilities().whenAvailable(new Function1<Capabilities, Unit>() {
            @Override
            public Unit invoke(Capabilities capabilities) {
                Zoom zoom = capabilities != null ? capabilities.getZoom() : null;
                if(zoom instanceof Zoom.VariableZoom){
                    cameraZoom = (Zoom.VariableZoom) zoom;
                    focusView.setScaleListener(new Function1<Float, Unit>() {
                        @Override
                        public Unit invoke(Float aFloat) {
                            scaleZoom(aFloat);
                            return null;
                        }
                    });
                    focusView.setPtrListener(new Function1<Integer, Unit>() {
                        @Override
                        public Unit invoke(Integer integer) {
                            pointerChanged(integer);
                            return null;
                        }
                    });
                } else {
                    zoomLvl.setVisibility(View.GONE);
                    focusView.setScaleListener(null);
                    focusView.setPtrListener(null);
                }
                return null;
            }
        });
        if (fotoapparat.isAvailable(front())){
            switchCamera.setVisibility(View.VISIBLE);
        } else {
            switchCamera.setVisibility(View.GONE);
        }
    }

    private void scaleZoom(float scaleFactor){
        float plusZoom = 0;
        if (scaleFactor < 1) plusZoom = -1 * (1 - scaleFactor);
        else plusZoom = scaleFactor - 1;

        float newZoom = curZoom + plusZoom;
        if (newZoom < 0 || newZoom > 1) return;

        curZoom = newZoom;
        fotoapparat.setZoom(curZoom);

        int progress = Math.round (cameraZoom.getMaxZoom() * curZoom);
        int value = cameraZoom.getZoomRatios().get(progress);

        float roundedValue = (float)(Math.round(((float)value) / 10f)) / 10f;

        zoomLvl.setVisibility(View.VISIBLE);
        zoomLvl.setText(String.format(Locale.getDefault(), "%.1f×", roundedValue));
    }

    private void pointerChanged(int fingerCount){
        if(fingerCount == 0) {
            zoomLvl.setVisibility(View.GONE);
        }
    }

    private void switchCameraOnClick() {
        View switchCameraButton = findViewById(R.id.switchCamera);

        boolean hasFrontCamera = fotoapparat.isAvailable(front());

        switchCameraButton.setVisibility(
                hasFrontCamera ? View.VISIBLE : View.GONE
        );

        if (hasFrontCamera) {
            switchCameraOnClick(switchCameraButton);
        }
    }

    private void toggleTorchOnSwitch() {
        SwitchCompat torchSwitch = findViewById(R.id.torchSwitch);

        torchSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                fotoapparat.updateConfiguration(
                        UpdateConfiguration.builder()
                                .flash(
                                        isChecked ? torch() : off()
                                )
                                .build()
                );
            }
        });
    }

    private void switchCameraOnClick(View view) {
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activeCameraBack = !activeCameraBack;
                fotoapparat.switchTo(
                        activeCameraBack ? back() : front(),
                        cameraConfiguration
                );
                adjustViewsVisibility();
            }
        });
    }

    private void takePictureOnClick() {
        capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();
            }
        });
    }

    private void takePicture() {
        PhotoResult photoResult = fotoapparat.takePicture();

        photoResult.saveToFile(new File(
                getExternalFilesDir("photos"),
                "photo.jpg"
        ));

        photoResult
                .toBitmap(scaled(0.25f))
                .whenDone(new WhenDoneListener<BitmapPhoto>() {
                    @Override
                    public void whenDone(@Nullable BitmapPhoto bitmapPhoto) {
                        if (bitmapPhoto == null) {
                            Log.e(LOGGING_TAG, "Couldn't capture photo.");
                            return;
                        }
                        ImageView imageView = findViewById(R.id.result);

                        imageView.setImageBitmap(bitmapPhoto.bitmap);
                        imageView.setRotation(-bitmapPhoto.rotationDegrees);
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (hasCameraPermission) {
            fotoapparat.start();
            adjustViewsVisibility();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (hasCameraPermission) {
            fotoapparat.stop();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (permissionsDelegate.resultGranted(requestCode, permissions, grantResults)) {
            hasCameraPermission = true;
            fotoapparat.start();
            adjustViewsVisibility();
            cameraView.setVisibility(View.VISIBLE);
        }
    }

    private class SampleFrameProcessor implements FrameProcessor {
        @Override
        public void process(@NotNull Frame frame) {
            // Perform frame processing, if needed
        }
    }

}
