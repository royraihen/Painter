package com.example.painter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.painter.Interface.BrushFragmentListener;
import com.example.painter.Interface.TextFragmentListener;
import com.google.android.material.snackbar.Snackbar;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import ja.burhanrashid52.photoeditor.PhotoEditor;
import ja.burhanrashid52.photoeditor.PhotoEditorView;

/*
 * TODO:
 *  FIX SAVE
 * */

public class MainActivity extends AppCompatActivity implements BrushFragmentListener, TextFragmentListener {
    private Button selectImageBtn;
    private Button bwBtn;
    private Button textBtn;
    private Button takePictureBtn;
    private Button cropBtn;
    private Button saveImage;
    private Button backBtn;
    private PhotoEditorView imageView;
    private PhotoEditor photoEditor;
    private Bitmap finalBitmap;
    private CoordinatorLayout coordinatorLayout;


    float downx = 0, downy = 0;
    float upx = 0, upy = 0;
    Paint paint;
    Matrix matrix;


    private final static int REQUEST_PERMISSIONS = 111;
    private final static int REQUEST_PICK_IMAGE = 112;
    private static final int REQUEST_IMAGE_CAPTURE = 113;
    private final static int PERMISSIONS_COUNT = 2;
    private final static String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private Uri imageUri;
    private Uri selectedImageUri;
    private static final String appID = "photoEditor";


    private static boolean editMode = false;

    static {
        System.loadLibrary("NativeImageProcessor");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();


    }

    @SuppressLint("NewAPI")
    private boolean permissions() {
        for (int i = 0; i < PERMISSIONS_COUNT; i++)
            if (checkSelfPermission(PERMISSIONS[i]) != PackageManager.PERMISSION_GRANTED)
                return true;

        return false;
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (permissions() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            requestPermissions(PERMISSIONS, REQUEST_PERMISSIONS);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResult) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResult);
        if (requestCode == REQUEST_PERMISSIONS && grantResult.length > 0) {
            if (permissions()) {
                ((ActivityManager) this.getSystemService(ACTIVITY_SERVICE)).clearApplicationUserData();
                recreate();
            }
        }
    }


    private void init() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());
        }
        if (!MainActivity.this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            findViewById(R.id.takePictureBtn).setVisibility(View.GONE);
        }

        selectImageBtn = findViewById(R.id.selectImageBtn);
        takePictureBtn = findViewById(R.id.takePictureBtn);
        imageView = findViewById(R.id.IV);
        bwBtn = findViewById(R.id.bw);
        textBtn = findViewById(R.id.text);
        cropBtn = findViewById(R.id.crop);
        saveImage = findViewById(R.id.save);
        backBtn = findViewById(R.id.back);

        photoEditor = new PhotoEditor.Builder(this, imageView).setPinchTextScalable(true).build();


        selectImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                final Intent pickIntent = new Intent(Intent.ACTION_PICK);
                pickIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                final Intent chooser = Intent.createChooser(intent, "Select Image");
                startActivityForResult(chooser, REQUEST_PICK_IMAGE);
            }
        });

        takePictureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Intent takePicIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePicIntent.resolveActivity(getPackageManager()) != null) {
                    //create a file for the photo that was just taken
                    final File photo = createImageFile();
                    imageUri = Uri.fromFile(photo);
                    final SharedPreferences preferences = getSharedPreferences(appID, 0);
                    preferences.edit().putString("path", photo.getAbsolutePath()).apply();
                    takePicIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                    startActivityForResult(takePicIntent, REQUEST_IMAGE_CAPTURE);
                } else {
                    Toast.makeText(MainActivity.this, "Camera not compatible", Toast.LENGTH_SHORT).show();
                }
            }
        });

        bwBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                photoEditor.setBrushDrawingMode(true);
                BrushFragment brushFragment = BrushFragment.getInstace();
                brushFragment.setListener(MainActivity.this);
                brushFragment.show(getSupportFragmentManager(), brushFragment.getTag());
            }
        });

        textBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TextFragment textFragment = TextFragment.getInstance();
                textFragment.setListener(MainActivity.this);
                textFragment.show(getSupportFragmentManager(),textFragment.getTag());
            }
        });

        cropBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startCrop(selectedImageUri);
            }
        });

        saveImage.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onClick(View view) {
                String photoDir = Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DCIM + "/";
                photoEditor.saveAsFile(photoDir, new PhotoEditor.OnSaveListener() {
                    @Override
                    public void onSuccess(@NonNull String imagePath) {
                        Log.e("PhotoEditor","Image Saved Successfully");

                    }
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        Log.e("PhotoEditor","Failed to save Image");
                    }
                });
//                final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
//                final DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialogInterface, int i) {
//                        if (i == DialogInterface.BUTTON_POSITIVE) {
//                            final File outFile = createImageFile();
//                            try (FileOutputStream out = new FileOutputStream(outFile)) {
//                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
//                                imageUri = Uri.parse("file://" + outFile.getAbsolutePath());
//                                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, imageUri));
//                                Toast.makeText(MainActivity.this, "SAVED", Toast.LENGTH_SHORT).show();
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                    }
//                };
//                builder.setMessage("Save current photo to Gallery?")
//                        .setPositiveButton("Yes", onClickListener)
//                        .setNegativeButton("No", onClickListener).show();
            }
        });

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                findViewById(R.id.editScreen).setVisibility(View.GONE);
                findViewById(R.id.welcomeScreen).setVisibility(View.VISIBLE);
                editMode = false;
            }
        });


    }

    private void startCrop(Uri selectedImageUri) {
        String dest = new StringBuilder(UUID.randomUUID().toString()).append(".jpg").toString();
        UCrop uCrop = UCrop.of(selectedImageUri, Uri.fromFile(new File(getCacheDir(),dest)));

        uCrop.start(MainActivity.this);
    }


    public void onBackPressed() {
        if (editMode) {
            findViewById(R.id.editScreen).setVisibility(View.GONE);
            findViewById(R.id.welcomeScreen).setVisibility(View.VISIBLE);
            editMode = false;
        } else {
            super.onBackPressed();
        }
    }

    private File createImageFile() {
        final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        final File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        final String filename = "/JPEG_" + timeStamp + ".jpg";
        return new File(dir + filename);
    }

    private Bitmap bitmap;
    private int width = 0;
    private int height = 0;
    private static final int MAX_PIX_COUNT = 2048;

    private int[] pixels;
    private int pixCount = 0;

    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                downx = event.getX();
                downy = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                upx = event.getX();
                upy = event.getY();
                imageView.invalidate();
                downx = upx;
                downy = upy;
                break;
            case MotionEvent.ACTION_UP:
                upx = event.getX();
                upy = event.getY();
                imageView.invalidate();
                break;
            case MotionEvent.ACTION_CANCEL:
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK)
            return;

        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (imageUri == null) {
                final SharedPreferences sharedPreferences = getSharedPreferences(appID, 0);
                final String path = sharedPreferences.getString("path", "");
                if (path.length() < 1) {
                    recreate();
                    return;
                }
                imageUri = Uri.parse("file://" + path);
            }
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, imageUri));
        } else if (data == null) {
            recreate();
            return;
        } else if (requestCode == REQUEST_PICK_IMAGE) {
            imageUri = data.getData();
            selectedImageUri = data.getData();
        } else if(requestCode == UCrop.REQUEST_CROP){
            handleCropResult(data);
        }
        if(resultCode == UCrop.RESULT_ERROR){
            handleCropError(data);
        }

        final ProgressDialog dialog = ProgressDialog.show(MainActivity.this, "Loading",
                "Wait", true);

        editMode = true;

        findViewById(R.id.welcomeScreen).setVisibility(View.GONE);
        findViewById(R.id.editScreen).setVisibility(View.VISIBLE);

        new Thread() {
            public void run() {
                bitmap = null;
                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inBitmap = bitmap;
                options.inJustDecodeBounds = true;
                try (InputStream inputStream = getContentResolver().openInputStream(imageUri)) {
                    bitmap = BitmapFactory.decodeStream(inputStream, null, options);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                options.inJustDecodeBounds = false;
                width = options.outWidth;
                height = options.outHeight;
                int resizeScale = 1;
                if (width > MAX_PIX_COUNT) {
                    resizeScale = width / MAX_PIX_COUNT;
                } else if (height > MAX_PIX_COUNT) {
                    resizeScale = height / MAX_PIX_COUNT;
                }
                if (width / resizeScale > MAX_PIX_COUNT || height / resizeScale > MAX_PIX_COUNT) {
                    resizeScale++;
                }
                options.inSampleSize = resizeScale;
                InputStream inputStream = null;
                try {
                    inputStream = getContentResolver().openInputStream(imageUri);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    recreate();
                    return;
                }
                bitmap = BitmapFactory.decodeStream(inputStream, null, options);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imageView.getSource().setImageBitmap(bitmap);
                        dialog.cancel();

                    }
                });
                width = bitmap.getWidth();
                height = bitmap.getHeight();
                bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

                pixCount = width * height;
                pixels = new int[pixCount];
                bitmap.getPixels(pixels, 0, width, 0, 0, width, height);


            }
        }.start();

    }

    private void handleCropError(Intent data) {
        final Throwable cropError = UCrop.getError(data);
        if(cropError != null){
            Toast.makeText(this,""+cropError.getMessage(),Toast.LENGTH_SHORT).show();
        }
        else{
            Toast.makeText(this,"Unexpected Error",Toast.LENGTH_SHORT).show();
        }
    }

    private void handleCropResult(Intent data) {
        final Uri resultUri = UCrop.getOutput(data);
        if (resultUri != null){
            imageView.getSource().setImageURI(resultUri);
        }
        else {
            Toast.makeText(this,"Error retrieving cropped image", Toast.LENGTH_SHORT).show();
        }

    }

    private void openImage(String path) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(path), "image/*");
        startActivity(intent);
    }

    private void saveToGallery (){
        Dexter.withActivity(this).withPermissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {
                            final String path = BitmapUtils.insertImage(getContentResolver(), finalBitmap, System.currentTimeMillis() + "_profile.jpg", null);
                            if (!TextUtils.isEmpty(path)) {
                                Snackbar snackbar = Snackbar
                                        .make(coordinatorLayout, "Image saved to gallery!", Snackbar.LENGTH_LONG)
                                        .setAction("OPEN", new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                openImage(path);
                                            }
                                        });

                                snackbar.show();
                            } else {
                                Snackbar snackbar = Snackbar
                                        .make(coordinatorLayout, "Unable to save image!", Snackbar.LENGTH_LONG);

                                snackbar.show();
                            }
                        } else {
                            Toast.makeText(getApplicationContext(), "Permissions are not granted!", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }

    @Override
    public void onBrushSizeChangedListener(float size) {
        photoEditor.setBrushSize(size);
    }

    @Override
    public void onBrushColorChangedListener(int color) {
        photoEditor.setBrushColor(color);
    }

    @Override
    public void onBrushStateChangedListener(boolean isEraser) {
        if (isEraser)
            photoEditor.brushEraser();
        else
            photoEditor.setBrushDrawingMode(true);
    }

    @Override
    public void onTextButtonClicked(String actualText, int color) {
        photoEditor.addText(actualText, color);
    }
}