package com.example.painter;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.painter.Interface.BrushFragmentListener;
import com.example.painter.Interface.RotateFragmentListener;
import com.example.painter.Interface.TextFragmentListener;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Stack;
import java.util.UUID;

import ja.burhanrashid52.photoeditor.PhotoEditor;
import ja.burhanrashid52.photoeditor.PhotoEditorView;


public class MainActivity extends AppCompatActivity implements BrushFragmentListener, TextFragmentListener, RotateFragmentListener {
    private Button selectImageBtn;
    private Button bwBtn;
    private Button textBtn;
    private Button takePictureBtn;
    private Button cropBtn;
    private Button saveImage;
    private Button backBtn;
    private Button undoBtn;
    private Button rotateBtn;
    private PhotoEditorView imageView;
    private PhotoEditor photoEditor;

    private Stack<Uri> cropStack = new Stack<Uri>();
    int undo_it = 0;
    int undo_array[] = new int[200];
    private int rotate_angle;

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
    private static boolean backWasPressed=false;
    //private boolean wasCropped = false;

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
        undoBtn = findViewById(R.id.undo);
        rotateBtn = findViewById(R.id.rotate);

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
                textFragment.show(getSupportFragmentManager(), textFragment.getTag());
            }
        });

        rotateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RotateFragment rotateFragment = RotateFragment.getInstance();
                if(backWasPressed){
                    backWasPressed=false;
                    rotateFragment.seekBar_rotate.setProgress(0);
                }
                rotateFragment.setListener(MainActivity.this);
                rotateFragment.show(getSupportFragmentManager(),rotateFragment.getTag());
            }
        });


        cropBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                undo_array[undo_it++] = 1;
                //undo_it++;
                cropStack.push(imageUri);
                startCrop(imageUri);
            }
        });

        undoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.out.println(undo_array);
                if (undo_it > 0) undo_it--;
                if (undo_array[undo_it] == 1) {
                    imageView.getSource().setImageURI(cropStack.pop());
                } else
                    photoEditor.undo();
            }
        });

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.out.println("it size=" + undo_it + " array=" + undo_array.toString());

                undo_it++;
                undo_array[undo_it] = 0;
            }
        });


        saveImage.setOnClickListener(new View.OnClickListener() {
            Uri mSaveImageUri;

            @SuppressLint("MissingPermission")
            @Override
            public void onClick(View view) {
                final File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                        System.currentTimeMillis() + ".png");

                photoEditor.saveAsFile(file.getAbsolutePath(), new PhotoEditor.OnSaveListener() {
                    @Override
                    public void onSuccess(@NonNull String imagePath) {
                        imageView.getSource().setImageURI(mSaveImageUri);
                        galleryAddPic(MainActivity.this, file.getAbsolutePath());
                        toasting("Picture Saved");
                        imageView.setRotation(0);
                        backWasPressed=true;
                        onBackPressed();
                    }

                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        Log.e("E", "FAILED");
                        toasting("Saving Failed");
                    }
                });

            }
        });

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                while (!cropStack.empty())
                    cropStack.pop();
                findViewById(R.id.editScreen).setVisibility(View.GONE);
                findViewById(R.id.welcomeScreen).setVisibility(View.VISIBLE);
                editMode = false;
            }
        });


    }

    private void toasting(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private static void galleryAddPic(Context context, String imagePath) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(imagePath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        context.sendBroadcast(mediaScanIntent);
    }

    private void startCrop(Uri imageUri) {
        String dest = new StringBuilder(UUID.randomUUID().toString()).append(".jpg").toString();
        UCrop uCrop = UCrop.of(imageUri, Uri.fromFile(new File(getCacheDir(), dest)));

        uCrop.start(MainActivity.this);
    }


    public void onBackPressed() {
        if (editMode) {
            while (!cropStack.empty())
                cropStack.pop();
            imageView.setRotation(0);
            backWasPressed=true;
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
        } else if (requestCode == UCrop.REQUEST_CROP) {
            handleCropResult(data);
            //imageView.getSource().setImageURI(tempUri);
        }
        if (resultCode == UCrop.RESULT_ERROR) {
            handleCropError(data);

        }
        final ProgressDialog dialog = ProgressDialog.show(MainActivity.this, "Loading", "Wait", true);

        editMode = true;

        findViewById(R.id.welcomeScreen).setVisibility(View.GONE);
        findViewById(R.id.editScreen).setVisibility(View.VISIBLE);


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
        imageView.getSource().setImageBitmap(bitmap);
        dialog.cancel();

        width = bitmap.getWidth();
        height = bitmap.getHeight();
        bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        pixCount = width * height;
        pixels = new int[pixCount];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);


    }

    private void handleCropError(Intent data) {
        final Throwable cropError = UCrop.getError(data);
        if (cropError != null) {
            Toast.makeText(this, "" + cropError.getMessage(), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Unexpected Error", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleCropResult(Intent data) {
        //final Uri resultUri = UCrop.getOutput(data);
        imageUri = UCrop.getOutput(data);
        if (imageUri != null) {
            imageView.getSource().setImageURI(imageUri);
        } else {
            Toast.makeText(this, "Error retrieving cropped image", Toast.LENGTH_SHORT).show();
        }

    }

    private void openImage(String path) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(path), "image/*");
        startActivity(intent);
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
        if (isEraser) {
            undo_array[undo_it++] = 0;
            photoEditor.brushEraser();

        } else {
            undo_array[undo_it++] = 0;
            photoEditor.setBrushDrawingMode(true);

        }
    }

    @Override
    public void onTextButtonClicked(String actualText, int color) {
        undo_array[undo_it++] = 0;
        photoEditor.addText(actualText, color);
    }

    @Override
    public void onRotateAngleChangedListener(int angle) {
        if(backWasPressed){
            angle=0;
            backWasPressed=false;
        }
        imageView.setRotation(angle);
    }
}