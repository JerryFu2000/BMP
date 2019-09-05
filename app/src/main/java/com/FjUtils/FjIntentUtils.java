package com.FjUtils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.example.bmp.MainActivity;

import java.io.File;

public final class FjIntentUtils {

    //启动MediaScanner服务，通知它扫描入口指定的文件
    public static void actionMediaScanFileIntent(Context context, File file){
        //通过Intent通知，启动MediaScanner服务
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        //获得指定图片文件的Uri对象
        Uri uri = Uri.fromFile(file);
        //将Uri对象添加到Intent中，即告知MediaScanner，需要扫描的文件
        intent.setData(uri);
        //通过广播发送Intent
        context.sendBroadcast(intent);
    }
}
