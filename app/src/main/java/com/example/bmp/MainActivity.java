package com.example.bmp;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.icu.util.Calendar;
import android.location.LocationManager;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.FjUtils.BitmapQuMo;
import com.FjUtils.CountryUtils;
import com.FjUtils.FileUtils;
import com.FjUtils.FjIntentUtils;
import com.FjUtils.LocationUtils;
import com.FjUtils.SDCardUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;


import static android.app.PendingIntent.getActivity;
import static android.graphics.Bitmap.CompressFormat.JPEG;
import static android.graphics.ImageDecoder.decodeBitmap;

import com.blankj.utilcode.util.DeviceUtils;
import com.blankj.utilcode.util.EncodeUtils;
import com.blankj.utilcode.util.ImageUtils;
import com.blankj.utilcode.util.RomUtils;
import com.blankj.utilcode.util.SPStaticUtils;
import com.blankj.utilcode.util.SPUtils;
import com.blankj.utilcode.util.TimeUtils;
import com.jakewharton.rxbinding3.view.RxView;
import com.jakewharton.rxbinding3.widget.RxSeekBar;
import com.nestia.biometriclib.BiometricPromptManager;
import com.tbruyelle.rxpermissions2.RxPermissions;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.functions.Consumer;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    final RxPermissions rxPermissions = new RxPermissions(this);

    private static final int ReturnFromActivity_CHOOSE_PHOTO = 0;
    private static final int ReturnFromActivity_TAKE_PHOTO = 1;
    private static final int ReturnFromActivity_GPS = 2;

    private Uri imageUri;

    private Bitmap bitmap_original;     //原始图片
    private Bitmap bitmap_blackwhite;   //转换后的黑白图片

    private BiometricPromptManager mManager;



//通过butterknife来初始化布局中的控件
    @BindView(R.id.img_path)
    public TextView imgPath;

    @BindView(R.id.edit_Text)
    public EditText editText;

    @BindView(R.id.imgShow)
    public ImageView imgShow;

    @BindView(R.id.seekbar_yuzhi)
    public SeekBar seekBar;

    @OnClick(R.id.btn_add)
    public void gotoChoosePhoto(){
        //通过Intent切换到系统相册，让用户选择需要的图片
        Intent intent = new Intent(
                Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, ReturnFromActivity_CHOOSE_PHOTO);
    }

    @OnClick(R.id.btn_blackwhite)
    public void transformBlackWhite(){
        if(bitmap_original!=null) {
            //将文本转换为十进制数值
            int yuzhi = Integer.parseInt(editText.getText().toString(), 10);
            Log.d(TAG, "阈值=" + yuzhi);
            //按指定的灰度阈值来转成黑白图片
            bitmap_blackwhite = BitmapQuMo.convertToBlackWhite(TAG, bitmap_original, yuzhi, 128 * 4, 64 * 4);
            Log.d(TAG, "转成黑白图片成功! Height=" + bitmap_blackwhite.getHeight() + " Width=" + bitmap_blackwhite.getWidth());
            //显示得到黑白图片
            imgShow.setImageBitmap(bitmap_blackwhite);
        }
    }

    @OnClick(R.id.btn_takephoto)
    public void doTakePhoto(){
        takePhoto();
    }

    @OnClick(R.id.btn_save)
    public void doSavePic(){
        //若已转换，就保存黑白图片
        if(bitmap_blackwhite!=null) {
            String str = saveImageBitmap(bitmap_blackwhite);
            //获取Bitmap中的数据字节，并打印输出
            byte[] byte_i = BitmapQuMo.getBlackWhiteBmpBytes(bitmap_blackwhite);
            BitmapQuMo.printHexString(TAG,"黑白图片的字节=",byte_i);
            Toast.makeText(MainActivity.this, "黑白图片已保存:"+str, Toast.LENGTH_LONG).show();
        }
        //若尚未转换，就保存原始图片
        else if(bitmap_original!=null){
            String str = saveImageBitmap(bitmap_original);
            //获取Bitmap中的数据字节，并打印输出
            //byte[] byte_i = ImageUtils.bitmap2Bytes(bitmap_original,JPEG);
            //ImageUtils.save(bitmap_original,);
            //byte[] byte_i = getBitmapBytes(bitmap);
            //BitmapQuMo.printHexString(TAG,"原始图片的字节=",byte_i);
            Toast.makeText(MainActivity.this, "原始图片已保存:"+str, Toast.LENGTH_LONG).show();
        }
        String string1 = FileUtils.getStoragePath(MainActivity.this,false);
        Log.d(TAG,"内部sd路径="+string1);
        String string2 = FileUtils.getStoragePath(MainActivity.this,true);
        Log.d(TAG,"外部sd路径="+string2);
    }


    @OnClick(R.id.btn_txtpic)
    public void gotoFP(){
        List<SDCardUtils.SDCardInfo> list = SDCardUtils.getSDCardInfo(MainActivity.this);
        Log.d(TAG, list.toString());
        FP();
    }

    @OnClick(R.id.btn_login)
    public void gotoLogin(){
        //startActivity(new Intent(this, LoginActivity.class));
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //绑定初始化ButterKnife
        ButterKnife.bind(this);
        //因为ButterKnife对SeekBar不支持，所以需要按通常方式来处理
        //seekBar = (SeekBar) findViewById(R.id.seekbar_yuzhi);
        seekBar.setOnSeekBarChangeListener(seekBarChangeListener);

        //判断SD卡是否存在
        if(!FileUtils.isSDCardState()){
            Log.d(TAG, "SD卡不存在");
            Toast.makeText(this, "SD卡不存在", Toast.LENGTH_LONG).show();
        } else{
            Toast.makeText(this, "有SD卡", Toast.LENGTH_LONG).show();
        }

        requestPermissions();

        mManager = BiometricPromptManager.from(this);
        //在文本框显示指纹的相关信息
        imgPath.setText(preFP());

        if(SPUtils.getInstance().getString("PASSWORD").equals("")) {
            Toast.makeText(this, "密码不存在，初始化为 12345678", Toast.LENGTH_LONG).show();
            SPUtils.getInstance().put("PASSWORD","12345678",false);
        }
        else {
            String string = SPUtils.getInstance().getString("PASSWORD");
            Toast.makeText(this, "密码="+string, Toast.LENGTH_LONG).show();
        }
    }

    private void requestPermissions() {
        RxPermissions rxPermissions = new RxPermissions(MainActivity.this);
        rxPermissions
            //依次请求每一个权限，然后依次发射“权限请求的结果”即permission事件 给观察者
            .requestEach(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_FINE_LOCATION)
            //被观察者订阅
            .subscribe(permission -> {
                //若本次permission事件是“批准权限”
                if (permission.granted) {
                    Toast.makeText(this, "用户已授权\n"+permission.name, Toast.LENGTH_LONG).show();
                    Log.d(TAG, "用户已授权\n"+permission.name);
                    if(permission.name.equals("android.permission.ACCESS_FINE_LOCATION")){
                        //若 当前设备的API_Level>=23(M版) 且 GPS未打开
                        //则弹出对话框，提示用户去打开Android系统内部的“定位设置界面”去打开GPS
                        if ( (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) && (!checkGPSIsOpen()) ) {
                            //构建对话框
                            new AlertDialog.Builder(this)
                                    .setTitle("提示")
                                    .setMessage("当前手机扫描蓝牙需要打开定位功能")
                                    //若按下“取消键”则直接finish()
                                    .setNegativeButton("取消",
                                            new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    finish();
                                                }
                                            })
                                    //若按下“前往设置”则
                                    .setPositiveButton("前往设置",
                                            new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    //构建Intent
                                                    //此处是跳转到Android系统内部的“定位设置界面”
                                                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                                    //启动新的Activity，并携带请求码=REQUEST_CODE_OPEN_GPS
                                                    //这样当新Activity销毁时会返回数据及此请求码并回调本Activity的onActivityResult()
                                                    startActivityForResult(intent, ReturnFromActivity_GPS);
                                                }
                                            })
                                    //不显示第三个按键
                                    .setCancelable(false)
                                    //显示对话框
                                    .show();
                        }
                    }

                }
                //若本次permission事件是“临时禁止权限”
                else if (permission.shouldShowRequestPermissionRationale) {
                    Toast.makeText(this, "用户本次没有授权，下次继续申请\n"+permission.name, Toast.LENGTH_LONG).show();
                    Log.d(TAG, "用户本次没有授权，下次继续申请\n"+permission.name);
                }
                //若本次permission事件是“永久禁止权限”
                else {
                    Toast.makeText(this, "用户永久禁止权限\n"+permission.name, Toast.LENGTH_LONG).show();
                    Log.d(TAG, "用户永久禁止权限\n"+permission.name);
                }
            });
    }

    //滑动条的监听
    private SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener(){

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            editText.setText(String.valueOf(progress*255/100));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            transformBlackWhite();
        }
    };


    //从其他Activity返回
    @RequiresApi(api = Build.VERSION_CODES.N)
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //判断是从哪个Activity返回
        switch (requestCode){
            //若是从“相册”返回
            case ReturnFromActivity_CHOOSE_PHOTO:
                Log.d(TAG, "是从相册返回");
                //if(requestCode==RESULT_OK){
                    if(Build.VERSION.SDK_INT>=19){
                        Log.d(TAG, "SDK_INT>=19");
                        //从入口传入的Intent获得 用户在相册中选中的图片的路径
                        String imagePath = handleImageOnKitKat(data);

                        Log.d(TAG, "imagePath=" + imagePath);
                        //对指定的图片路径进行解码获得Bitmap对象
                        bitmap_original = BitmapFactory.decodeFile(imagePath);
                        //在ImageView控件上显示该图片
                        imgShow.setImageBitmap(bitmap_original);
                        imgPath.setText(imagePath);

                        bitmap_blackwhite = null;
                    }
                //}
                Log.d(TAG, "显示图片完成 Height="+bitmap_original.getHeight()+" Width="+bitmap_original.getWidth());
                File[] files = getExternalFilesDirs(Environment.MEDIA_MOUNTED);
                for(File file:files) {
                    Log.e(TAG, "得到的全部外存：" + String.valueOf(file));
                }
                break;


            //若是从“拍照”返回
            case ReturnFromActivity_TAKE_PHOTO:
                //若拍照成功
                if (resultCode == RESULT_OK) {
                    try{
                        //调用BitmapFactory.decodeStream方法，将“相机拍摄后保存的照片路径Uri”指定的照片
                        //解析成Bitmap对象
                        bitmap_original = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                        Log.d(TAG, "拍照后的imageUri="+imageUri);
                        //在ImageView中显示这张照片
                        imgShow.setImageBitmap(bitmap_original);

                        //将这个临时照片保存
                        String str = saveImageBitmap(bitmap_original);
                        Toast.makeText(MainActivity.this, "照片已保存:"+str, Toast.LENGTH_LONG).show();

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                break;

            //若是从“GPS”返回
            case ReturnFromActivity_GPS:
                if (checkGPSIsOpen()){
                    Toast.makeText(MainActivity.this, "GPS已使能", Toast.LENGTH_LONG).show();
                }
                else{
                    Toast.makeText(MainActivity.this, "GPS没有使能！", Toast.LENGTH_LONG).show();
                }
                break;

            default:
                break;

        }
    }


    private void takePhoto(){
        Log.d(TAG, "已检测到按下拍照键");
        //声明“Android内置的外部存储器的路径/test/当前时间.jpg”文件对象
        //感觉这个文件是个临时文件(便于临时保存相机拍到的照片), 因为在Android系统中无法找到它的存在
        File outputImage = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/test/" + System.currentTimeMillis() + ".jpg");
        //根据文件对象，创建指定的路径
        outputImage.getParentFile().mkdirs();

        //将文件对象转换得到相应的Uri
        if(Build.VERSION.SDK_INT>=24){
            //若SDK版本>=24，则采用安全的FileProvider方式获得Uri
            //注意"com.example.bmp.fileprovider"和xml中的一致
            imageUri = FileProvider.getUriForFile(MainActivity.this,
                    "com.example.bmp.fileprovider", outputImage);
            Log.d(TAG, "SDK>=24");
        } else {
            //若SDK版本<24，则采用传统方式获得Uri
            imageUri = Uri.fromFile(outputImage);
            Log.d(TAG, "SDK<24");
        }
        Log.d(TAG, "拍照前的imageUri="+imageUri);
        //通过Intent切换到“系统拍照程序”，当拍完后返回到onActivityResult()
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        //传入“相机拍摄后保存的照片路径Uri”
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, ReturnFromActivity_TAKE_PHOTO);
        Log.d(TAG, "调用系统拍照程序");
    }





    private String saveImageBitmap(Bitmap bmp) {
        //创建自定义的FJ名称的文件
        File file = BitmapQuMo.creatBmpFile(TAG, MainActivity.this);
        //将入口传入的bmp保存为JPEG格式的文件
        boolean save = ImageUtils.save(bmp, file, JPEG);
        if(save){
            Log.d(TAG, "图片保存成功");
            //启动MediaScanner服务，通知它扫描入口指定的文件
            FjIntentUtils.actionMediaScanFileIntent(MainActivity.this, file);
            //return fileName;
            return file.getName();
        }
        return "保存失败";
    }




    //将用户在相册中选中的图片显示在ImageView控件上
    @TargetApi(19)
    private String handleImageOnKitKat(Intent data){
        String imagePath = null;
        //从入口传入的Intent中取出封装好的Uri(即用户点选的图片的路径)
        Uri uri = data.getData();
        Log.d(TAG, "uri=" + uri);
        //若是document类型的Uri，则通过document id处理
        if(DocumentsContract.isDocumentUri(this, uri)){
            //根据uri，取出DocumentId
            String docId = DocumentsContract.getDocumentId(uri);
            //
            if("com.android.providers.media.documents".equals(uri.getAuthority())){
                Log.d(TAG, "getAuthority = com.android.providers.media.documents");
                //解析出数字格式的id
                String id = docId.split(":")[1];
                String selection = MediaStore.Images.Media._ID + "=" + id;
                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
            } else if("com.android.providers.downloads.documents".equals(uri.getAuthority())){
                Log.d(TAG, "getAuthority = com.android.providers.downloads.documents");
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
                imagePath = getImagePath(contentUri, null);
            }
        } else if("content".equalsIgnoreCase(uri.getScheme())){
            Log.d(TAG, "getScheme = content");
            imagePath = getImagePath(uri, null);
        } else if("file".equalsIgnoreCase(uri.getScheme())){
            Log.d(TAG, "getScheme = file");
            imagePath = uri.getPath();
        }
        Log.d(TAG, "imagePath=" + imagePath);
        return imagePath;
    }

    //从入口指定的Uri对象（即用户在相册中选中的图片）中获得对应的图片路径
    private String getImagePath(Uri uri, String selection){
        String path = null;
        Cursor cursor = getContentResolver().query(uri, null, selection, null, null);
        if(cursor != null){
            if(cursor.moveToFirst()){
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }



    //检测指纹认证的信息及环境
    private String preFP(){
        String string;

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("SDK version is "+ Build.VERSION.SDK_INT);
        stringBuilder.append("\n");
        stringBuilder.append("isHardwareDetected : "+mManager.isHardwareDetected());
        stringBuilder.append("\n");
        stringBuilder.append("hasEnrolledFingerprints : "+mManager.hasEnrolledFingerprints());
        stringBuilder.append("\n");
        stringBuilder.append("isKeyguardSecure : "+mManager.isKeyguardSecure());
        stringBuilder.append("\n");

        stringBuilder.append("厂家="+DeviceUtils.getManufacturer()+"\n型号="+DeviceUtils.getModel());
        stringBuilder.append("\n");

        stringBuilder.append("国家="+ CountryUtils.getCountryBySim());
        stringBuilder.append("\n");



        string = stringBuilder.toString();

        return string;
    }


    //调用指纹认证
    private boolean FP(){
        boolean result = false;

        result = mManager.isBiometricPromptEnable();
        if (result) {
            mManager.authenticate(new BiometricPromptManager.OnBiometricIdentifyCallback() {
                @Override
                //点击“改用密码”
                public void onUsePassword() {
                    Toast.makeText(MainActivity.this, "onUsePassword", Toast.LENGTH_SHORT).show();
                    LayoutInflater factory = LayoutInflater.from(MainActivity.this);
                    final View v1 = factory.inflate(R.layout.dialog_password, null);
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("请输入密码")
                            //.setMessage("当前手机扫描蓝牙需要打开定位功能")
                            .setView(v1)
                            //若按下“取消键”则直接finish()
                            .setNegativeButton("取消",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {

                                        }
                                    })
                            //若按下“前往设置”则
                            .setPositiveButton("确认",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            EditText passwd = (EditText)v1.findViewById(R.id.editText_password);
                                            String passWordStr1 = passwd.getText().toString();
                                            String passWordStr2 = SPUtils.getInstance().getString("PASSWORD");
                                            Toast.makeText(MainActivity.this, "你输入的密码是："+passWordStr1+"\n正确密码是："+passWordStr2, Toast.LENGTH_SHORT).show();

                                        }
                                    })
                            //不显示第三个按键
                            .setCancelable(false)
                            //显示对话框
                            .show();
                }

                @Override
                public void onSucceeded() {

                    Toast.makeText(MainActivity.this, "onSucceeded", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailed() {

                    Toast.makeText(MainActivity.this, "onFailed", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(int code, String reason) {

                    Toast.makeText(MainActivity.this, "onError", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onCancel() {

                    Toast.makeText(MainActivity.this, "onCancel", Toast.LENGTH_SHORT).show();
                }
            });
        }
        return result;
    }

    //检测GPS是否已打开
    //返回：true=GPS已打开；false=GPS已关闭
    private boolean checkGPSIsOpen() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null)
            return false;
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
    }

/*
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        //判断是从哪儿提出的权限申请
        switch (requestCode){
            case 1:
                if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(this, "用户已授权", Toast.LENGTH_LONG).show();
                    Log.d(TAG,"用户已授权");
                } else {
                    Toast.makeText(this, "用户未授权", Toast.LENGTH_LONG).show();
                    Log.d(TAG,"用户未授权");
                }
                break;

            default:
                break;
        }
    }
*/
}
