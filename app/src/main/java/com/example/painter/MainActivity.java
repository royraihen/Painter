package com.example.painter;

import androidx.appcompat.app.AppCompatActivity;

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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.painter.Interface.BrushFragmentListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;

import ja.burhanrashid52.photoeditor.PhotoEditor;
import ja.burhanrashid52.photoeditor.PhotoEditorView;

public class MainActivity extends AppCompatActivity {
    private Button selectImageBtn;
    private Button bwBtn;
    private Button takePictureBtn;
    private Button saveImage;
    private Button backBtn;
    private PhotoEditorView imageView;
    private PhotoEditor photoEditor;

    float downx = 0, downy = 0;
    float upx = 0, upy = 0;
    Paint paint;
    Matrix matrix;


    private Canvas canvas;
    private final static int REQUEST_PERMISSIONS = 111;
    private final static int REQUEST_PICK_IMAGE = 112;
    private static final int REQUEST_IMAGE_CAPTURE = 113;
    private final static int PERMISSIONS_COUNT = 2;
    private final static String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private Uri imageUri;
    private static final String appID = "photoEditor";


    private static boolean editMode = false;


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
        saveImage = findViewById(R.id.save);
        backBtn = findViewById(R.id.back);

        photoEditor = new PhotoEditor.Builder(this,imageView).setPinchTextScalable(true).build();


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
//                new Thread() {
//                    public void run() {
//                        for (int i = 0; i < pixCount; i++) {
//                            pixels[i] /= 2;
//                        }
//                        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
//
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                imageView.getSource().setImageBitmap(bitmap);
//                            }
//                        });
//                    }
//                }.start();
                BrushFragment brushFragment = BrushFragment.getInstace();
                brushFragment.setListener((BrushFragmentListener) MainActivity.this);
                brushFragment.show(getSupportFragmentManager(),brushFragment.getTag());
            }
        });

        saveImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                final DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (i == DialogInterface.BUTTON_POSITIVE) {
                            final File outFile = createImageFile();
                            try (FileOutputStream out = new FileOutputStream(outFile)) {
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                                imageUri = Uri.parse("file://" + outFile.getAbsolutePath());
                                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, imageUri));
                                Toast.makeText(MainActivity.this, "SAVED", Toast.LENGTH_SHORT).show();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                };
                builder.setMessage("Save current photo to Gallery?")
                        .setPositiveButton("Yes", onClickListener)
                        .setNegativeButton("No", onClickListener).show();
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



    public void onBackPressed(){
        if(editMode){
            findViewById(R.id.editScreen).setVisibility(View.GONE);
            findViewById(R.id.welcomeScreen).setVisibility(View.VISIBLE);
            editMode = false;
        }
        else{
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
                //canvas.drawLine(downx, downy, upx, upy, paint);
                imageView.invalidate();
                downx = upx;
                downy = upy;
                break;
            case MotionEvent.ACTION_UP:
                upx = event.getX();
                upy = event.getY();
                //canvas.drawLine(downx, downy, upx, upy, paint);
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
                //canvas = new Canvas(bitmap);
                paint = new Paint();
                paint.setColor(Color.GREEN);
                paint.setStrokeWidth(15);
                matrix = new Matrix();
//                canvas.drawBitmap(bitmap, matrix, paint);

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
}