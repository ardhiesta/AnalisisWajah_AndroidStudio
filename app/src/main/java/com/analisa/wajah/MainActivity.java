package com.analisa.wajah;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import clarifai2.api.ClarifaiBuilder;
import clarifai2.api.ClarifaiClient;
import clarifai2.dto.input.ClarifaiInput;
import clarifai2.dto.model.output.ClarifaiOutput;
import clarifai2.dto.prediction.Concept;
import clarifai2.dto.prediction.Region;

public class MainActivity extends AppCompatActivity {
    static final int TAKE_PICTURE = 1;
    static final int PICK_IMAGE = 2;
    static final String CLARIFAI_KEY = "CLARIFAI_KEY";
    // Storage Permissions variables
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    Button btnTakePic, btnChoosePic;
    TextView tvHasil;
    ImageView ivThumbnailPhoto;
    ProgressBar pbLoading;
    LinearLayout llTombol;

    //permission method.
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have read or write permission
        int writePermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int readPermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);

        if (writePermission != PackageManager.PERMISSION_GRANTED || readPermission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        verifyStoragePermissions(this);

        // Get reference to views
        btnTakePic = (Button) findViewById(R.id.btnTakePic);
        btnChoosePic = (Button) findViewById(R.id.btnChoosePic);
        ivThumbnailPhoto = (ImageView) findViewById(R.id.ivThumbnailPhoto);
        pbLoading = (ProgressBar) findViewById(R.id.pb_loading);
        tvHasil = (TextView) findViewById(R.id.text_hasil);
        llTombol = (LinearLayout) findViewById(R.id.ll_tombol);

        // add onclick listener to the button
        btnTakePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                cameraClick();
            }
        });

        btnChoosePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                galleryClick();
            }
        });
    }

    private void cameraClick() {
        // create intent with ACTION_IMAGE_CAPTURE action
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // this part to save captured image on provided path
        File file = new File(
                Environment
                        .getExternalStoragePublicDirectory
                                (Environment.DIRECTORY_PICTURES),
                "my-photo.jpg");
        Uri photoPath = Uri.fromFile(file);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoPath);

        // start camera activity
        startActivityForResult(intent, TAKE_PICTURE);
    }

    private void galleryClick() {
        // action memilih image dari gallery untuk versi Android KitKat
        // memilih image menggunakan Storage Access Framework
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {
        if (requestCode == TAKE_PICTURE && resultCode == RESULT_OK) {
            File file = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES).getPath(), "my-photo.jpg");
            Uri uri = Uri.fromFile(file);
            Bitmap bitmap;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(
                        getContentResolver(), uri);
                ivThumbnailPhoto.setImageBitmap(bitmap);

                String imagePath = file.getAbsolutePath();

                MyTask myTask = new MyTask();
                myTask.execute(imagePath, null, null);
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && intent != null) {
            Uri uri = intent.getData();
            try {
                String realPath;
                // SDK < API11
                if (Build.VERSION.SDK_INT < 11)
                    realPath = RealPathUtil.getRealPathFromURI_BelowAPI11(this, intent.getData());

                    // SDK >= 11 && SDK < 19
                else if (Build.VERSION.SDK_INT < 19)
                    realPath = RealPathUtil.getRealPathFromURI_API11to18( this, intent.getData());

                    // SDK > 19 (Android 4.4)
                else
                    realPath = RealPathUtil.getRealPathFromURI_API19(this, intent.getData());

                Bitmap bitmap = getBitmapFromUri(uri);
                ivThumbnailPhoto.setImageBitmap(bitmap);

                MyTask myTask = new MyTask();
                myTask.execute(realPath, null, null);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Bitmap getBitmapFromUri(Uri uri) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor =
                getContentResolver().openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        return image;
    }

    private void setTextHasil(final TextView text, final String value){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                text.setText(value);
            }
        });
    }

    private void setLoading(boolean status){
        pbLoading.setIndeterminate(status);
        if (status) {
            pbLoading.setVisibility(View.VISIBLE);
            llTombol.setVisibility(View.GONE);
            tvHasil.setVisibility(View.GONE);
            ivThumbnailPhoto.setVisibility(View.GONE);
        } else {
            pbLoading.setVisibility(View.GONE);
            llTombol.setVisibility(View.VISIBLE);
            tvHasil.setVisibility(View.VISIBLE);
            ivThumbnailPhoto.setVisibility(View.VISIBLE);
        }
    }

    private class MyTask extends AsyncTask<String, Void, Void> {
        @Override
        protected void onPreExecute() {
            setLoading(true);
        }

        @Override
        protected Void doInBackground(String... arg0) {
            // TODO Auto-generated method stub
            ClarifaiClient client = new ClarifaiBuilder(
                    CLARIFAI_KEY).buildSync();

            List<ClarifaiOutput<Region>> predictionResults =
                    client.getDefaultModels().demographicsModel().predict()
                            .withInputs(ClarifaiInput.forImage(new File(arg0[0].toString())))
                            .executeSync().get();

            if (predictionResults.size() > 0) {
                if (predictionResults.get(0).data().size() > 0) {
                    List<Concept> ageList = predictionResults.get(0).data().get(0).ageAppearances();
                    List<Concept> genderList = predictionResults.get(0).data().get(0).genderAppearances();
                    List<Concept> appearancesList = predictionResults.get(0).data().get(0).multiculturalAppearances();

                    String jk = genderList.get(0).name();
                    if (jk.equalsIgnoreCase("masculine")) {
                        jk = "laki-laki";
                    } else {
                        jk = "perempuan";
                    }

                    setTextHasil(tvHasil, "Hasil analisis foto \n Usia: "+ageList.get(0).name()
                            +" tahun\nJenis kelamin: "+jk+"\nRas: "+appearancesList.get(0).name());
                } else {
                    setTextHasil(tvHasil, "gagal menganalisis foto");
                }
            } else {
                setTextHasil(tvHasil, "gagal menganalisis foto");
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);
            setLoading(false);
        }
    }
}
/* http://hmkcode.com/android-camera-taking-photos-camera/ */