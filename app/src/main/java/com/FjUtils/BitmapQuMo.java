package com.FjUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.Log;

import com.blankj.utilcode.util.TimeUtils;
import com.example.bmp.MainActivity;

import java.io.File;

public final class BitmapQuMo {


    /**
     * 将彩色图转换为纯黑白二色
     *
     * @param bmp
     * @return 返回转换好的位图
     */
    public static Bitmap convertToBlackWhite(String tag, Bitmap bmp, int yuzhi, float mWidth, float mHeight) {
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
        float x = (float) mWidth/width;
        float y = (float) mHeight/height;
        Log.d(tag, "x="+x+" y="+y);
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

    //读取黑白图片，将其转换为“从左往右，从上到下”的byte[]，用于单色LCD的点阵图
    public static byte[] getBlackWhiteBmpBytes(Bitmap BlackWhiteBmp) {
        int width = BlackWhiteBmp.getWidth(); // 获取位图的宽
        int height = BlackWhiteBmp.getHeight(); // 获取位图的高
        int[] pixels = new int[width * height]; // 通过位图的大小创建像素点数组
        byte[] byte_pixel = new byte[width * height/8];//创建用于单色LCD的点阵图，图片取模方式为“从左往右，从上到下”


        //将bmp中的各个像素点读取到pixels数组中，便于后续处理
        //getPixels本身非常非常简单，就是一个拷贝的过程。
        //关键：拷贝的话，就涉及到2个因素，从哪里来，到哪里去？
        //这里的x, y, width, height是属于从哪里来的参数，也就是我们控制怎样读取mBitmap1的参数
        //offset, stride是到哪里去的参数，也就是控制如何放入到pixels[]中去的参数
        //offset是目标内存的起始地址的偏移量，stride是目标内存中隔多少个Pixels再写下一行；
        BlackWhiteBmp.getPixels(pixels, 0, width, 0, 0, width, height);


        int bit_c = 0;  //设定1个字节的位数=0
        int byte_c = 0; //设定字节数=0
        //遍历pixels数组中的像素点（相对图片来说，是从左往右，从上到下）
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                //取出当前像素点，去除透明度，只获取每个像素点的RGB组合值
                int rgb = (pixels[width * i + j] & 0x00FFFFFF);

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


    //按照一行16个字节（0x00~0x0F），进行打印输出
    public static void printHexString(String tag, String hint, byte[] b) {
        Log.d(tag, hint);
        int byte_c = 0; //累计字节数
        int line_c = 0; //累计行数
        String str ="";

        str += "第"+line_c+"行：";
        for (int i = 0; i < b.length; i++) {
            //将当前INT转换为Hex，若不足2位就在前面补个0，然后转为大写，添加到str
            String hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1){ hex = '0' + hex; }
            str += hex.toUpperCase() + " ";
            //已转字节数+1，若为第8个字节就插入"-"，若达到16字节，就log输出
            byte_c += 1;
            if(byte_c==8){
                str += "- ";
            }
            if(byte_c>=16) {
                Log.d(tag, str);
                byte_c = 0;
                line_c += 1;
                str = "第"+line_c+"行：";
            }
        }
        Log.d(tag,"");
    }


    //创建自定义的FJ名称的文件
    public static File creatBmpFile(String tag, Context context) {

        //将这个临时照片保存
        //定义路径=“Android内置的外部存储器的路径/DCIM/Fj”
        String fileDir = FileUtils.getInStoragePath(context) + "/DCIM/Fj";
        //定义路径=“Android外置的外部存储器的路径/DCIM/Fj”
        //String fileDir = FileUtils.getOutStoragePath(MainActivity.this) + "/DCIM/Fj";
        //创建文件夹，若已存在则返回false，但不影响后续操作
        boolean bl = FileUtils.createFolder(fileDir);
        if (bl) {
            Log.d(tag, "创建文件夹成功！");
        } else {
            Log.d(tag, "创建文件夹失败");
        }

        //定义文件名称=“FjPhoto当前时间.jpg”
        String fileName = "FjPhoto" + TimeUtils.getNowString() + ".jpg";
        //根据路径及文件名，创建文件对象
        File file = new File(fileDir, fileName);

        Log.d(tag, "fileDir=" + fileDir);
        Log.d(tag, "file=" + file);

        return file;

    }
}
