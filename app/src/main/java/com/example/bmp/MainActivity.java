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
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.FjUtils.FileUtils;
import com.FjUtils.SDCardUtils;
import com.FjUtils.TimeUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;


import static android.app.PendingIntent.getActivity;
import static android.graphics.ImageDecoder.decodeBitmap;
import com.nestia.biometriclib.BiometricPromptManager;
import com.tbruyelle.rxpermissions2.RxPermissions;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    final RxPermissions rxPermissions = new RxPermissions(this);

    private static final int CHOOSE_PHOTO = 0;
    private static final int TAKE_PHOTO = 1;

    private SeekBar seekBar = null;

    private Uri imageUri;

    private Bitmap bitmap;
    private Bitmap bitmap_blackwhite;

    private BiometricPromptManager mManager;




    @BindView(R.id.img_path)
    public TextView imgPath;

    @BindView(R.id.edit_Text)
    public EditText editText;

    @BindView(R.id.imgShow)
    public ImageView imgShow;


    @OnClick(R.id.btn_add)
    public void gotoChoosePhoto(){
        //通过Intent切换到系统相册，让用户选择需要的图片
        Intent intent = new Intent(
                Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, CHOOSE_PHOTO);
    }

    @OnClick(R.id.btn_blackwhite)
    public void tBlackWhite(){
        //转成黑白图片
        int yuzhi = Integer.parseInt(editText.getText().toString(), 10);
        Log.d(TAG, "阈值="+yuzhi);
        bitmap_blackwhite = convertToBlackWhite(bitmap, yuzhi);
        Log.d(TAG, "转成黑白图片成功! Height="+bitmap_blackwhite.getHeight()+" Width="+bitmap_blackwhite.getWidth());
        //显示得到bitmap图片
        imgShow.setImageBitmap(bitmap_blackwhite);
    }

    @OnClick(R.id.btn_takephoto)
    public void tTakePhoto(){
        takePhoto();
    }

    @OnClick(R.id.btn_save)
    public void savePic(){
        //若已转换，就保存黑白图片
        if(bitmap_blackwhite!=null) {
            String str = saveImageBitmap(bitmap_blackwhite);
            //获取Bitmap中的数据字节，并打印输出
            //byte[] byte_i = getBitmapBytes(bitmap_blackwhite);
            //printHexString("黑白图片的字节=",byte_i);
            Toast.makeText(MainActivity.this, "黑白图片已保存:"+str, Toast.LENGTH_LONG).show();
        }
        //若尚未转换，就保存原始图片
        else if(bitmap!=null){
            String str = saveImageBitmap(bitmap);
            //获取Bitmap中的数据字节，并打印输出
            //byte[] byte_i = getBitmapBytes(bitmap);
            //printHexString("原始图片的字节=",byte_i);
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




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //绑定初始化ButterKnife
        ButterKnife.bind(this);
        //因为ButterKnife对SeekBar不支持，所以需要按通常方式来处理
        seekBar = (SeekBar) findViewById(R.id.seekbar_yuzhi);
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
    }

    private void requestPermissions() {
        RxPermissions rxPermissions = new RxPermissions(MainActivity.this);
        rxPermissions
                .requestEach(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                .subscribe(permission -> { // will emit 2 Permission objects
                    if (permission.granted) {
                        // I can control the camera now
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
                                                        startActivityForResult(intent, 1);
                                                    }
                                                })
                                        //不显示第三个按键
                                        .setCancelable(false)
                                        //显示对话框
                                        .show();

                                //openGPS(this);  //强行打开GPS的方法不行！！！
                            }
                        }

                    } else if (permission.shouldShowRequestPermissionRationale) {
                        Toast.makeText(this, "用户本次没有授权，下次继续申请\n"+permission.name, Toast.LENGTH_LONG).show();
                        Log.d(TAG, "用户本次没有授权，下次继续申请\n"+permission.name);
                     } else {
                        Toast.makeText(this, "用户彻底不授权\n"+permission.name, Toast.LENGTH_LONG).show();
                        Log.d(TAG, "用户彻底不授权\n"+permission.name);
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
            //转成黑白图片
            int yuzhi = Integer.parseInt(editText.getText().toString(), 10);
            Log.d(TAG, "阈值="+yuzhi);
            bitmap_blackwhite = convertToBlackWhite(bitmap, yuzhi);
            Log.d(TAG, "转成黑白图片成功! Height="+bitmap_blackwhite.getHeight()+" Width="+bitmap_blackwhite.getWidth());
            //显示得到bitmap图片
            imgShow.setImageBitmap(bitmap_blackwhite);
        }
    };


    //从其他Activity返回
    @RequiresApi(api = Build.VERSION_CODES.N)
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //判断是从哪个Activity返回
        switch (requestCode){
            //若是从“相册”返回
            case CHOOSE_PHOTO:
                Log.d(TAG, "是从相册返回");
                //if(requestCode==RESULT_OK){
                    if(Build.VERSION.SDK_INT>=19){
                        Log.d(TAG, "SDK_INT>=19");
                        //从入口传入的Intent获得 用户在相册中选中的图片的路径
                        String imagePath = handleImageOnKitKat(data);

                        Log.d(TAG, "imagePath=" + imagePath);
                        //对指定的图片路径进行解码获得Bitmap对象
                        bitmap = BitmapFactory.decodeFile(imagePath);
                        //在ImageView控件上显示该图片
                        imgShow.setImageBitmap(bitmap);
                        imgPath.setText(imagePath);

                        bitmap_blackwhite = null;
                    }
                //}
                Log.d(TAG, "显示图片完成 Height="+bitmap.getHeight()+" Width="+bitmap.getWidth());
                File[] files = getExternalFilesDirs(Environment.MEDIA_MOUNTED);
                for(File file:files) {
                    Log.e(TAG, "得到的全部外存：" + String.valueOf(file));
                }
                    break;


            //若是从“拍照”返回
            case TAKE_PHOTO:
                //若拍照成功
                if (resultCode == RESULT_OK) {
                    try{
                        //调用BitmapFactory.decodeStream方法，将“相机拍摄后保存的照片路径Uri”指定的照片
                        //解析成Bitmap对象
                        bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                        Log.d(TAG, "拍照后的imageUri="+imageUri);
                        //在ImageView中显示这张照片
                        imgShow.setImageBitmap(bitmap);

                        //将这个临时照片保存
                        String str = saveImageBitmap(bitmap);
                        Toast.makeText(MainActivity.this, "照片已保存:"+str, Toast.LENGTH_LONG).show();

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
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
        startActivityForResult(intent, TAKE_PHOTO);
        Log.d(TAG, "调用系统拍照程序");
    }

    public void printHexString(String hint, byte[] b) {
        Log.d(TAG, hint);
        int byte_c = 0;
        int line_c = 0;
        String str ="";

        str += "第"+line_c+"行：";
        for (int i = 0; i < b.length; i++) {
            String hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1){
                hex = '0' + hex;
            }
            str += hex.toUpperCase() + " ";
            byte_c += 1;
            if(byte_c>=16) {
                Log.d(TAG, str);
                byte_c = 0;
                line_c += 1;
                str = "第"+line_c+"行：";;
            }
        }
        Log.d(TAG,"");
    }



    private String saveImageBitmap(Bitmap bmp) {
        //将这个临时照片保存
        //定义路径=“Android内置的外部存储器的路径/DCIM/Fj”
        String fileDir = FileUtils.getInStoragePath(MainActivity.this) + "/DCIM/Fj";
        //定义路径=“Android外置的外部存储器的路径/DCIM/Fj”
        //String fileDir = FileUtils.getOutStoragePath(MainActivity.this) + "/DCIM/Fj";
        //创建文件夹，若已存在则返回false，但不影响后续操作
        boolean bl =  FileUtils.createFolder(fileDir);
        if(bl){
            Log.d(TAG, "创建文件夹成功！");
        }
        else {
            Log.d(TAG, "创建文件夹失败");
        }

        //定义文件名称=“FjPhoto当前时间.jpg”
        String fileName = "FjPhoto" + TimeUtils.format(System.currentTimeMillis()) + ".jpg";
        //根据路径及文件名，创建文件对象
        File file = new File(fileDir, fileName);

        Log.d(TAG, "fileDir="+fileDir);
        Log.d(TAG, "file=" + file);
        FileOutputStream fout = null;
        try {
            //创建文件输出流对象
            fout = new FileOutputStream(file);
            //将Bitmap压缩成JPEG格式保存到指定的文件输出流对象中
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fout);
            Log.d(TAG, "图片保存成功");

            //通过Intent通知，启动MediaScanner服务
            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            //获得指定图片文件的Uri对象
            Uri uri = Uri.fromFile(file);
            //将Uri对象添加到Intent中，即告知MediaScanner，需要扫描的文件
            intent.setData(uri);
            //通过广播发送Intent
            MainActivity.this.sendBroadcast(intent);

            return fileName;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                fout.flush();
                fout.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "保存失败";
    }

    private byte[] getBitmapBytes(Bitmap bmp) {
        int width = bmp.getWidth(); // 获取位图的宽
        int height = bmp.getHeight(); // 获取位图的高
        int[] pixels = new int[width * height]; // 通过位图的大小创建像素点数组
        byte[] byte_pixel = new byte[width * height/8];


        //将bmp中的各个像素点读取到pixels数组中，便于后续处理
        //getPixels本身非常非常简单，就是一个拷贝的过程。
        //关键：拷贝的话，就涉及到2个因素，从哪里来，到哪里去？
        //这里的x, y, width, height是属于从哪里来的参数，也就是我们控制怎样读取mBitmap1的参数
        //offset, stride是到哪里去的参数，也就是控制如何放入到pixels[]中去的参数
        //offset是目标内存的起始地址的偏移量，stride是目标内存中隔多少个Pixels再写下一行；
        bmp.getPixels(pixels, 0, width, 0, 0, width, height);

        //设定1个字节的位数=0
        int bit_c = 0;
        int byte_c = 0;
        //遍历pixels数组中的像素点（相对图片来说，是从左往右，从上到下）

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int grey = pixels[width * i + j];

                //获取每个像素点的RGB组合值
                int rgb = (grey & 0x00FFFFFF);

                switch (bit_c) {
                    //若是bit0
                    case 0:
                        if (rgb == 0) {
                            byte_pixel[byte_c] |= 0x01;
                        } else {
                            byte_pixel[byte_c] &= 0xFE;
                        }
                        bit_c += 1;
                        break;

                    //若是bit1
                    case 1:
                        if (rgb == 0) {
                            byte_pixel[byte_c] |= 0x02;
                        } else {
                            byte_pixel[byte_c] &= 0xFD;
                        }
                        bit_c += 1;
                        break;

                    case 2:
                        if (rgb == 0) {
                            byte_pixel[byte_c] |= 0x04;
                        } else {
                            byte_pixel[byte_c] &= 0xFB;
                        }
                        bit_c += 1;
                        break;

                    case 3:
                        if (rgb == 0) {
                            byte_pixel[byte_c] |= 0x08;
                        } else {
                            byte_pixel[byte_c] &= 0xF7;
                        }
                        bit_c += 1;
                        break;

                    case 4:
                        if (rgb == 0) {
                            byte_pixel[byte_c] |= 0x10;
                        } else {
                            byte_pixel[byte_c] &= 0xEF;
                        }
                        bit_c += 1;
                        break;

                    case 5:
                        if (rgb == 0) {
                            byte_pixel[byte_c] |= 0x20;
                        } else {
                            byte_pixel[byte_c] &= 0xDF;
                        }
                        bit_c += 1;
                        break;

                    case 6:
                        if (rgb == 0) {
                            byte_pixel[byte_c] |= 0x40;
                        } else {
                            byte_pixel[byte_c] &= 0xBF;
                        }
                        bit_c += 1;
                        break;

                    case 7:
                        if (rgb == 0) {
                            byte_pixel[byte_c] |= 0x80;
                        } else {
                            byte_pixel[byte_c] &= 0x7F;
                        }
                        bit_c = 0;
                        byte_c += 1;
                        break;
                }
            }
        }
        return byte_pixel;

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

    /**
     * 将彩色图转换为纯黑白二色
     *
     * @param bmp
     * @return 返回转换好的位图
     */
    public static Bitmap convertToBlackWhite(Bitmap bmp, int yuzhi) {
        int width = bmp.getWidth(); // 获取位图的宽
        int height = bmp.getHeight(); // 获取位图的高
        int[] pixels = new int[width * height]; // 通过位图的大小创建像素点数组

        //将bmp中的各个像素点读取到pixels数组中，便于后续处理
        //getPixels本身非常非常简单，就是一个拷贝的过程。
        //关键：拷贝的话，就涉及到2个因素，从哪里来，到哪里去？
        //这里的x, y, width, height是属于从哪里来的参数，也就是我们控制怎样读取mBitmap1的参数
        //offset, stride是到哪里去的参数，也就是控制如何放入到pixels[]中去的参数
        //offset是目标内存的起始地址的偏移量，stride是目标内存中隔多少个Pixels再写下一行；
        bmp.getPixels(pixels, 0, width, 0, 0, width, height);

        //设定转换为黑白色的阈值
        int tmp = yuzhi;//100;
        //设定透明度为固定值=0xFF
        int alpha = 0xFF << 24;
        //遍历pixels数组中的像素点（相对图片来说，是从左往右，从上到下）
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int grey = pixels[width * i + j];

                //将每个像素点的RGB分离出来
                alpha = ((grey & 0xFF000000) >> 24);
                int red = ((grey & 0x00FF0000) >> 16);
                int green = ((grey & 0x0000FF00) >> 8);
                int blue = (grey & 0x000000FF);

                if (red > tmp) {
                    red = 255;
                } else {
                    red = 0;
                }
                if (blue > tmp) {
                    blue = 255;
                } else {
                    blue = 0;
                }
                if (green > tmp) {
                    green = 255;
                } else {
                    green = 0;
                }

                //转换为黑白色
                grey = alpha << 24 | red << 16 | green << 8 | blue;
                //若本像素点=0xFFFFFFFF，则认为是白色
                if (grey == 0xFFFFFFFF){//-1) {
                    grey = 0xFFFFFFFF;//-1;
                }
                //否则认为是黑色
                else {
                    grey = 0xFF000000;//-16777216;
                }

//                //转化成灰度像素
//                grey = (int) (red * 0.3 + green * 0.59 + blue * 0.11);
//                grey = alpha | (grey << 16) | (grey << 8) | grey;
                pixels[width * i + j] = grey;
            }
        }
        //新建图片
        //Bitmap newBmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        //Bitmap newBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Bitmap newBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        //设置图片数据
        newBmp.setPixels(pixels, 0, width, 0, 0, width, height);

// Matrix类进行图片处理（缩小或者旋转）
        Matrix matrix = new Matrix();
        // 缩小一倍
        float x = (float) 128*4/width;
        float y = (float) 64*4/height;
        Log.d(TAG, "x="+x+" y="+y);
        matrix.postScale(x, y);
        // 生成新的图片
        Bitmap result = Bitmap.createBitmap(newBmp, 0, 0, newBmp.getWidth(),
                newBmp.getHeight(), matrix, true);


        return result;
        //Bitmap resizeBmp = ThumbnailUtils.extractThumbnail(newBmp, width, height);
        //Bitmap resizeBmp = ThumbnailUtils.extractThumbnail(newBmp, 380, 460);
        //Bitmap resizeBmp = ThumbnailUtils.extractThumbnail(newBmp, 128*4, 64*4);
        //return resizeBmp;
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
                public void onUsePassword() {
                    Toast.makeText(MainActivity.this, "onUsePassword", Toast.LENGTH_SHORT).show();
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
