package com.v2retail.dotvik.scanner;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageInfo;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCase;
import androidx.camera.core.CameraSelector.Builder;
import androidx.camera.core.ImageAnalysis.Analyzer;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory;
import androidx.lifecycle.ViewModelProvider.Factory;
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.barcode.common.Barcode.UrlBookmark;
import com.google.mlkit.vision.barcode.common.Barcode.WiFi;
import com.google.mlkit.vision.common.InputImage;
import com.v2retail.dotvik.R;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Metadata(
        mv = {1, 5, 1},
        k = 1,
        d1 = {"\u0000l\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0011\n\u0000\n\u0002\u0010\u0015\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\u0018\u00002\u00020\u0001B\u0005¢\u0006\u0002\u0010\u0002J\b\u0010\u0012\u001a\u00020\u0013H\u0002J\b\u0010\u0014\u001a\u00020\u0013H\u0002J\b\u0010\u0015\u001a\u00020\u0013H\u0002J\b\u0010\u0016\u001a\u00020\u0017H\u0002J\u0012\u0010\u0018\u001a\u00020\u00132\b\u0010\u0019\u001a\u0004\u0018\u00010\u001aH\u0014J+\u0010\u001b\u001a\u00020\u00132\u0006\u0010\u001c\u001a\u00020\u00042\f\u0010\u001d\u001a\b\u0012\u0004\u0012\u00020\u00060\u001e2\u0006\u0010\u001f\u001a\u00020 H\u0016¢\u0006\u0002\u0010!J\u0018\u0010\"\u001a\u00020\u00132\u0006\u0010#\u001a\u00020$2\u0006\u0010%\u001a\u00020&H\u0003J\b\u0010'\u001a\u00020\u0013H\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082D¢\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082D¢\u0006\u0002\n\u0000R\u0010\u0010\u0007\u001a\u0004\u0018\u00010\bX\u0082\u000e¢\u0006\u0002\n\u0000R\u0010\u0010\t\u001a\u0004\u0018\u00010\nX\u0082\u000e¢\u0006\u0002\n\u0000R\u0010\u0010\u000b\u001a\u0004\u0018\u00010\fX\u0082\u000e¢\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\u0004X\u0082\u000e¢\u0006\u0002\n\u0000R\u0010\u0010\u000e\u001a\u0004\u0018\u00010\u000fX\u0082\u000e¢\u0006\u0002\n\u0000R\u0010\u0010\u0010\u001a\u0004\u0018\u00010\u0011X\u0082\u000e¢\u0006\u0002\n\u0000¨\u0006("},
        d2 = {"Lcom/c1ctech/barcodescannerexp/MainActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "PERMISSION_CAMERA_REQUEST", "", "TAG", "", "analysisUseCase", "Landroidx/camera/core/ImageAnalysis;", "cameraProvider", "Landroidx/camera/lifecycle/ProcessCameraProvider;", "cameraSelector", "Landroidx/camera/core/CameraSelector;", "lensFacing", "previewUseCase", "Landroidx/camera/core/Preview;", "previewView", "Landroidx/camera/view/PreviewView;", "bindAnalyseUseCase", "", "bindCameraUseCases", "bindPreviewUseCase", "isCameraPermissionGranted", "", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "onRequestPermissionsResult", "requestCode", "permissions", "", "grantResults", "", "(I[Ljava/lang/String;[I)V", "processImageProxy", "barcodeScanner", "Lcom/google/mlkit/vision/barcode/BarcodeScanner;", "imageProxy", "Landroidx/camera/core/ImageProxy;", "setupCamera", "BarcodeScannerExp.app.main"}
)
public final class ScannerActivity extends AppCompatActivity {
    private PreviewView previewView;
    private ProcessCameraProvider cameraProvider;
    private CameraSelector cameraSelector;
    private int lensFacing = CameraSelector.LENS_FACING_BACK;
    private Preview previewUseCase;
    private ImageAnalysis analysisUseCase;
    private final String TAG = "Barcode Scanner";
    private final int PERMISSION_CAMERA_REQUEST = 1;
    private HashMap _$_findViewCache;
    private TextView textViewData;

    private String REQUEST_CODE = "barcode";
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);
        REQUEST_CODE = getIntent().getStringExtra("ScanType");
        this.setupCamera();
    }
    private void closeActivity() {
        Intent intent = new Intent();
        intent.putExtra("ScanType", REQUEST_CODE);
        intent.putExtra("ScannedData", textViewData.getText().toString());
        setResult(RESULT_OK, intent); // You can also send result without any data using setResult(int resultCode)
        finish();
    }
    private final void setupCamera() {
        this.previewView = this.findViewById(R.id.preview_view);
        textViewData = this.findViewById(R.id.tvScannedData);
        this.cameraSelector = (new Builder()).requireLensFacing(this.lensFacing).build();
        ViewModelStoreOwner var10002 = (ViewModelStoreOwner)this;
        Companion var10003 = AndroidViewModelFactory.Companion;
        Application var10004 = this.getApplication();
        Intrinsics.checkNotNullExpressionValue(var10004, "application");
        ((CameraXViewModel)(new ViewModelProvider(var10002, (Factory)var10003.getInstance(var10004))).get(CameraXViewModel.class)).getProcessCameraProvider().observe((LifecycleOwner)this, (Observer)(new Observer() {
            // $FF: synthetic method
            // $FF: bridge method
            public void onChanged(Object var1) {
                this.onChanged((ProcessCameraProvider)var1);
            }

            public final void onChanged(@Nullable ProcessCameraProvider provider) {
                ScannerActivity.this.cameraProvider = provider;
                if (ScannerActivity.this.isCameraPermissionGranted()) {
                    ScannerActivity.this.bindCameraUseCases();
                } else {
                    ActivityCompat.requestPermissions((Activity)ScannerActivity.this, new String[]{"android.permission.CAMERA"}, ScannerActivity.this.PERMISSION_CAMERA_REQUEST);
                }

            }
        }));
    }

    private final void bindCameraUseCases() {
        this.bindPreviewUseCase();
        this.bindAnalyseUseCase();
    }

    private final void bindPreviewUseCase() {
        if (this.cameraProvider != null) {
            ProcessCameraProvider var10000;
            if (this.previewUseCase != null) {
                var10000 = this.cameraProvider;
                Intrinsics.checkNotNull(var10000);
                var10000.unbind(new UseCase[]{(UseCase)this.previewUseCase});
            }

            androidx.camera.core.Preview.Builder var10001 = new androidx.camera.core.Preview.Builder();
            PreviewView var10002 = this.previewView;
            Intrinsics.checkNotNull(var10002);
            Display var8 = var10002.getDisplay();
            Intrinsics.checkNotNullExpressionValue(var8, "previewView!!.display");
            this.previewUseCase = var10001.setTargetRotation(var8.getRotation()).build();
            Preview var4 = this.previewUseCase;
            Intrinsics.checkNotNull(var4);
            PreviewView var6 = this.previewView;
            Intrinsics.checkNotNull(var6);
            var4.setSurfaceProvider(var6.getSurfaceProvider());

            String var5;
            String var7;
            try {
                var10000 = this.cameraProvider;
                Intrinsics.checkNotNull(var10000);
                LifecycleOwner var10 = (LifecycleOwner)this;
                CameraSelector var9 = this.cameraSelector;
                Intrinsics.checkNotNull(var9);
                Intrinsics.checkNotNullExpressionValue(var10000.bindToLifecycle(var10, var9, new UseCase[]{(UseCase)this.previewUseCase}), "cameraProvider!!.bindToL…viewUseCase\n            )");
            } catch (IllegalStateException var2) {
                var5 = this.TAG;
                var7 = var2.getMessage();
                if (var7 == null) {
                    var7 = "IllegalStateException";
                }

                Log.e(var5, var7);
            } catch (IllegalArgumentException var3) {
                var5 = this.TAG;
                var7 = var3.getMessage();
                if (var7 == null) {
                    var7 = "IllegalArgumentException";
                }

                Log.e(var5, var7);
            }

        }
    }

    private final void bindAnalyseUseCase() {
        BarcodeScannerOptions var10000 = (new com.google.mlkit.vision.barcode.BarcodeScannerOptions.Builder()).setBarcodeFormats(0, new int[0]).build();
        Intrinsics.checkNotNullExpressionValue(var10000, "BarcodeScannerOptions.Bu…RMAT_ALL_FORMATS).build()");
        BarcodeScannerOptions options = var10000;
        BarcodeScanner var7 = BarcodeScanning.getClient(options);
        Intrinsics.checkNotNullExpressionValue(var7, "BarcodeScanning.getClient(options)");
        final BarcodeScanner barcodeScanner = var7;
        if (this.cameraProvider != null) {
            ProcessCameraProvider var8;
            if (this.analysisUseCase != null) {
                var8 = this.cameraProvider;
                Intrinsics.checkNotNull(var8);
                var8.unbind(new UseCase[]{(UseCase)this.analysisUseCase});
            }

            androidx.camera.core.ImageAnalysis.Builder var10001 = new androidx.camera.core.ImageAnalysis.Builder();
            PreviewView var10002 = this.previewView;
            Intrinsics.checkNotNull(var10002);
            Display var12 = var10002.getDisplay();
            Intrinsics.checkNotNullExpressionValue(var12, "previewView!!.display");
            this.analysisUseCase = var10001.setTargetRotation(var12.getRotation()).build();
            ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();
            ImageAnalysis var9 = this.analysisUseCase;
            if (var9 != null) {
                var9.setAnalyzer((Executor)cameraExecutor, (Analyzer)(new Analyzer() {
                    public final void analyze(@NotNull ImageProxy imageProxy) {
                        Intrinsics.checkNotNullParameter(imageProxy, "imageProxy");
                        ScannerActivity.this.processImageProxy(barcodeScanner, imageProxy);
                    }
                }));
            }

            String var10;
            String var11;
            try {
                var8 = this.cameraProvider;
                Intrinsics.checkNotNull(var8);
                LifecycleOwner var14 = (LifecycleOwner)this;
                CameraSelector var13 = this.cameraSelector;
                Intrinsics.checkNotNull(var13);
                Intrinsics.checkNotNullExpressionValue(var8.bindToLifecycle(var14, var13, new UseCase[]{(UseCase)this.analysisUseCase}), "cameraProvider!!.bindToL…ysisUseCase\n            )");
            } catch (IllegalStateException var5) {
                var10 = this.TAG;
                var11 = var5.getMessage();
                if (var11 == null) {
                    var11 = "IllegalStateException";
                }

                Log.e(var10, var11);
            } catch (IllegalArgumentException var6) {
                var10 = this.TAG;
                var11 = var6.getMessage();
                if (var11 == null) {
                    var11 = "IllegalArgumentException";
                }

                Log.e(var10, var11);
            }

        }
    }

    @SuppressLint({"UnsafeOptInUsageError"})
    private final void processImageProxy(BarcodeScanner barcodeScanner, final ImageProxy imageProxy) {
        Image var10000 = imageProxy.getImage();
        Intrinsics.checkNotNull(var10000);
        ImageInfo var10001 = imageProxy.getImageInfo();
        Intrinsics.checkNotNullExpressionValue(var10001, "imageProxy.imageInfo");
        InputImage var4 = InputImage.fromMediaImage(var10000, var10001.getRotationDegrees());
        Intrinsics.checkNotNullExpressionValue(var4, "InputImage.fromMediaImag…mageInfo.rotationDegrees)");
        InputImage inputImage = var4;
        barcodeScanner.process(inputImage).addOnSuccessListener((OnSuccessListener)(new OnSuccessListener() {
            // $FF: synthetic method
            // $FF: bridge method
            public void onSuccess(Object var1) {
                this.onSuccess((List)var1);
            }

            public final void onSuccess(List barcodes) {
                Intrinsics.checkNotNullExpressionValue(barcodes, "barcodes");
                Iterable $this$forEach$iv = (Iterable)barcodes;
                boolean $i$f$forEach = false;
                Iterator var4 = $this$forEach$iv.iterator();

                while(var4.hasNext()) {
                    Object element$iv = var4.next();
                    Barcode barcode = (Barcode)element$iv;
                    boolean var7 = false;
                    Intrinsics.checkNotNullExpressionValue(barcode, "barcode");
                    Rect bounds = barcode.getBoundingBox();
                    Point[] corners = barcode.getCornerPoints();
                    String rawValue = barcode.getRawValue();
                    TextView var10000 = (TextView)ScannerActivity.this._$_findCachedViewById(R.id.tvScannedData);
                    Intrinsics.checkNotNullExpressionValue(var10000, "tvScannedData");
                    var10000.setText((CharSequence)barcode.getRawValue());
                    int valueType = barcode.getValueType();
                    String ssid;
                    String password;
                    switch(valueType) {
                        case 8:
                            UrlBookmark var16 = barcode.getUrl();
                            Intrinsics.checkNotNull(var16);
                            ssid = var16.getTitle();
                            var16 = barcode.getUrl();
                            Intrinsics.checkNotNull(var16);
                            password = var16.getUrl();
                            var10000 = (TextView)ScannerActivity.this._$_findCachedViewById(R.id.tvScannedData);
                            Intrinsics.checkNotNullExpressionValue(var10000, "tvScannedData");
                            var10000.setText((CharSequence)("Title: " + ssid + "\nURL: " + password));
                            break;
                        case 9:
                            WiFi var15 = barcode.getWifi();
                            Intrinsics.checkNotNull(var15);
                            ssid = var15.getSsid();
                            var15 = barcode.getWifi();
                            Intrinsics.checkNotNull(var15);
                            password = var15.getPassword();
                            var15 = barcode.getWifi();
                            Intrinsics.checkNotNull(var15);
                            int type = var15.getEncryptionType();
                            var10000 = (TextView)ScannerActivity.this._$_findCachedViewById(R.id.tvScannedData);
                            Intrinsics.checkNotNullExpressionValue(var10000, "tvScannedData");
                            var10000.setText((CharSequence)("ssid: " + ssid + "\npassword: " + password + "\ntype: " + type));
                    }
                    closeActivity();
                }

            }
        })).addOnFailureListener((OnFailureListener)(new OnFailureListener() {
            public final void onFailure(Exception it) {
                String var10000 = ScannerActivity.this.TAG;
                String var10001 = it.getMessage();
                if (var10001 == null) {
                    var10001 = it.toString();
                }

                Log.e(var10000, var10001);
            }
        })).addOnCompleteListener((OnCompleteListener)(new OnCompleteListener() {
            public final void onComplete(Task it) {
                imageProxy.close();
            }
        }));
    }

    public void onRequestPermissionsResult(int requestCode, @NotNull String[] permissions, @NotNull int[] grantResults) {
        Intrinsics.checkNotNullParameter(permissions, "permissions");
        Intrinsics.checkNotNullParameter(grantResults, "grantResults");
        if (requestCode == this.PERMISSION_CAMERA_REQUEST) {
            if (this.isCameraPermissionGranted()) {
                this.bindCameraUseCases();
            } else {
                Log.e(this.TAG, "no camera permission");
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private final boolean isCameraPermissionGranted() {
        return ContextCompat.checkSelfPermission(this.getBaseContext(), "android.permission.CAMERA") == 0;
    }

    // $FF: synthetic method
    public static final ProcessCameraProvider access$getCameraProvider$p(ScannerActivity $this) {
        return $this.cameraProvider;
    }

    public View _$_findCachedViewById(int var1) {
        if (this._$_findViewCache == null) {
            this._$_findViewCache = new HashMap();
        }

        View var2 = (View)this._$_findViewCache.get(var1);
        if (var2 == null) {
            var2 = this.findViewById(var1);
            this._$_findViewCache.put(var1, var2);
        }

        return var2;
    }

    public void _$_clearFindViewByIdCache() {
        if (this._$_findViewCache != null) {
            this._$_findViewCache.clear();
        }

    }
}