package com.bobo.test;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    String[] PERMISSION_STORAGES=new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,//写内存卡的权限
            Manifest.permission.READ_EXTERNAL_STORAGE,//读内存卡的权限
    };
    private static final int REQUEST_CODE_STORAGE = 1;
    public static final int PHOTO_REQUEST_TAKEPHOTO = 2;// 拍照
    public static final int PHOTO_REQUEST_CUT = 3;// 结果

    ImageView iv_show;
    public File tempFile;
    private Uri cropImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        iv_show= (ImageView) findViewById(R.id.iv_show);
        requestStoragePermission(this);
    }
    public void onClick(View v){
        switch (v.getId()){
            case R.id.bt_takephoto:
                startCamera();
                break;

        }
    }

    //开启手机摄像头
    private void startCamera() {
        String status= Environment.getExternalStorageState();
        if(status.equals(Environment.MEDIA_MOUNTED))
        {
            tempFile=new File(getExternalCacheDir(),getPhotoFileName());//SD卡的应用关联缓存目录
            try {
                if(tempFile.exists()){
                    tempFile.delete();
                }
                tempFile.createNewFile();
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "没有找到储存目录",Toast.LENGTH_LONG).show();
            }
        }else {
            Toast.makeText(MainActivity.this, "没有储存卡",Toast.LENGTH_LONG).show();
        }
        Intent cameraintent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // 指定调用相机拍照后照片的储存路径
        Uri uri=null;
        if (Build.VERSION.SDK_INT >= 24) {
            uri = FileProvider.getUriForFile(MainActivity.this,"com.bobo.test",tempFile);
        }else {
            uri = Uri.fromFile(tempFile);
        }
        cameraintent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
       startActivityForResult(cameraintent, PHOTO_REQUEST_TAKEPHOTO);
    }

    //使用系统当前日期加以调整作为照片的名称
    private String getPhotoFileName() {
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat dateFormat = new SimpleDateFormat("'IMG'_yyyyMMdd_HHmmss");
        return dateFormat.format(date) + ".jpg";
    }

    /**
     * 申请权限 SD卡的读写权限
     * @param activity
     */
    private void requestStoragePermission(Activity activity){
        //检测权限
        int permission = ContextCompat.checkSelfPermission(activity.getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission!= PackageManager.PERMISSION_GRANTED){
            //没有权限 则申请权限  弹出对话框
            ActivityCompat.requestPermissions(activity,PERMISSION_STORAGES,REQUEST_CODE_STORAGE);
        }
    }

    /**
     * 申请权限结果回调
     * @param requestCode 请求码
     * @param permissions 申请权限的数组
     * @param grantResults 申请权限成功与否的结果
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case 1:
                if (grantResults.length > 0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    //申请成功
                    Toast.makeText(MainActivity.this,"授权SD卡权限成功",Toast.LENGTH_SHORT).show();
                }else {
                    //申请失败
                    Toast.makeText(MainActivity.this,"授权SD卡权限失败 可能会影响应用的使用",Toast.LENGTH_SHORT).show();
                }
                break;
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case PHOTO_REQUEST_TAKEPHOTO:// 当选择拍照时调用
                Uri uri=null;
                if(Build.VERSION.SDK_INT >= 24) {
                    uri = FileProvider.getUriForFile(MainActivity.this,"com.bobo.test",tempFile);
                }else {
                    uri = Uri.fromFile(tempFile);
                }
                startPhotoZoom(uri);

                break;
            case PHOTO_REQUEST_CUT:// 返回的结果
                if(resultCode==RESULT_OK) {
                    if (data != null)
                        try {
                            Bitmap bp = BitmapFactory.decodeStream(getContentResolver().openInputStream(cropImageUri));
                            iv_show.setImageBitmap(bp);
                    } catch (FileNotFoundException e) {
                            e.printStackTrace();
                    }
                }
                break;
        }
    }

    public void startPhotoZoom(Uri uri) {
        File CropPhoto=new File(getExternalCacheDir(),"crop.jpg");
        try{
            if(CropPhoto.exists()){
                CropPhoto.delete();
            }
            CropPhoto.createNewFile();
        }catch(IOException e){
            e.printStackTrace();
        }

       
        cropImageUri=Uri.fromFile(CropPhoto);
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uri, "image/*");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); //添加这一句表示对目标应用临时授权该Uri所代表的文件
        }
        // 下面这个crop=true是设置在开启的Intent中设置显示的VIEW可裁剪
        intent.putExtra("crop", "true");
        intent.putExtra("scale", true);

        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);

        //输出的宽高

        intent.putExtra("outputX", 300);
        intent.putExtra("outputY", 300);

        intent.putExtra("return-data", false);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cropImageUri);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        intent.putExtra("noFaceDetection", true); // no face detection
        startActivityForResult(intent, PHOTO_REQUEST_CUT);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
}
