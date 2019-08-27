package com.FjUtils;

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;

/*
一般的Android App能读取的存储空间主要有三种：

app自己的私有目录，也就是/data/data/《app 目录》。
读写这个目录不需要单独的权限。每个app只能读写自己的目录，而不能读写其他app的目录。 Android通过Seandroid对权限进行了管理。

/sdcard。
这个其实是Android手机的internal storage。 也就是一般你买手机的时候， 说手机是64GB的存储空间，就是说的这个地方。这个地方的读写需要申请权限。READ_EXTERNAL_STORAGE 是读， WRITE_EXTERNAL_STORAGE 是写， 有写的权限就自动有读的权限。 这个权限是正对整个/sdcard，不区分子目录， 也就是说一旦你申请权限你可以对整个/sdcard上的所有文件和目录有操作权限。这个权限的管理会显示再settings里相应的app permission里。

外置sdcard
这个对应的是你放入手机sdcard插槽的microSD卡。 有时候也叫removable storage。 Android里无法通过申请权限来获取读写的权利。 这一点和上面说的2不同。 因此，如果需要获取写权限， 需要用户指定特定的目录单独授权。这里举个简单的例子。 如果外置sdcard的路径是/mnt/extsdcard，然后上面有两个目录a和b， 那么你可以让用户授权你写/mnt/extsdcard/a, 但是你还需要让用户再单独授权你写/mnt/extsdcard/b， 也就是要授权两次。 具体的实现方法， 就不多说了， google再github上给了个例子， 其中的wiki页面有比较详细的描述。

特别要说明的是， 由于这个没有对应的android permission， 所以如果你得到授权以后， 对应的目录路径不会显示再settings中的app permission。 相反， 它会显示再app storage里，用户可以在那里revoke对app的授权（同样， 也是指定目录）。这个我感觉其实不好， 因为这本质上也是权限问题， 应该都放在app permission里。
 ————————————————
版权声明：本文为CSDN博主「Omni-Space」的原创文章，遵循CC 4.0 by-sa版权协议，转载请附上原文出处链接及本声明。
原文链接：https://blog.csdn.net/omnispace/article/details/79302862

 */
public class FileUtils {

/*
Context.getPackageName();           // 用于获取APP的所在包目录
Context.getPackageCodePath();       //来获得当前应用程序对应的apk文件的路径
Context.getPackageResourcePath();   // 获取该程序的安装包路径
Context.getDatabasePath();          //返回通过Context.openOrCreateDatabase创建的数据库文件

Environment.getDataDirectory().getPath(); 　　　　　　   // 获得根目录/data
Environment.getDownloadCacheDirectory().getPath();     //获得缓存目录/cache
Environment.getExternalStorageDirectory().getPath();   //获得SD卡目录/mnt/sdcard
//定义路径=“Android内置的外部存储器的路径/DCIM/Fj”
    String fileDir = Environment.getExternalStorageDirectory() + "/DCIM/Fj";
//      fileDir=/storage/emulated/0/DCIM/Fj
//      这里的“/storage/emulated/0/”就是 文件管理器->手机->内部存储设备 这个路径
Environment.getRootDirectory().getPath();   　　　　    // 获得系统目录/system
//File.separator 代表 "/"


String path = File.getPath();//获得文件或文件夹的绝对路径
String path = File.getAbsoultePath();//获得文件或文件夹的相对路径

String parentPath = File.getParent();//获得文件或文件夹的父目录

String Name = File.getName();//获得文件或文件夹的名称

File.mkDir(); //建立文件夹
File.createNewFile();//建立文件

File[] files = File.listFiles();//列出文件夹下的所有文件和文件夹名

File.isDirectory();//true是文件夹,false是文件

File.renameTo(dest);//修改文件夹和文件名

File.delete();//删除文件夹或文件


资源文件raw和assets
res/raw：文件会被映射到R.java文件中，访问的时候直接通过资源ID访问，没有有目录结构
//例如访问raw下的文件:
InputStream is = getResources().openRawResource(R.raw.filename);
//从资源文件中获取Bitmap
Bitmap bmp=BitmapFactory.decodeResource(getResources(), R.drawable.ico);

assets：不会映射到R.java文件中，通过AssetManager来访问，能有目录结构
//例如访问assets下的文件:
AssetManager am = getAssets();
InputStream is = am.open("filename");
*/

    //检查SDCard存在并且可以读写
    public static boolean isSDCardState(){
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    /**
     * 判断文件是否已经存在
     *@param fileName 要检查的文件名
     * @return boolean, true表示存在，false表示不存在
     */
    public static boolean isFileExist(String fileName) {
        File file = new File("绝对路径" + fileName);
        return file.exists();
    }

    /**
     * 新建目录
     * @param path 目录的绝对路径
     * @return 创建成功则返回true
     */
    public static boolean createFolder(String path){
        File file = new File(path);
        return file.mkdir();
    }

    /**
     * 创建文件
     *@param path 文件所在目录的目录名
     * @param fileName 文件名
     * @return 文件新建成功则返回true
     */
    public static boolean createFile(String path, String fileName) {
        File file = new File(path + File.separator + fileName);
        if (file.exists()) {
            return false;
        } else {
            try {
                return file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 删除单个文件
     * @param path 文件所在的绝对路径
     * @param fileName 文件名
     * @return 删除成功则返回true
     */
    public static boolean deleteFile(String path, String fileName) {
        File file = new File(path + File.separator + fileName);
        return file.exists() && file.delete();
    }

    /**
     * 删除一个目录（可以是非空目录）
     * @param dir 目录绝对路径
     */
    public static boolean deleteDirection(File dir) {
        if (dir == null || !dir.exists() || dir.isFile()) {
            return false;
        }
        for (File file : dir.listFiles()) {
            if (file.isFile()) {
                file.delete();
            } else if (file.isDirectory()) {
                deleteDirection(file);//递归
            }
        }
        dir.delete();
        return true;
    }

    /**
     * 将字符串写入文件
     *@param text  写入的字符串
     * @param fileStr 文件的绝对路径
     * @param isAppend true从尾部写入，false从头覆盖写入
     */
    public static void writeFile(String text, String fileStr, boolean isAppend) {
        try {
            File file = new File(fileStr);
            File parentFile = file.getParentFile();
            if (!parentFile.exists()) {
                parentFile.mkdirs();
            }
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream f = new FileOutputStream(fileStr, isAppend);
            f.write(text.getBytes());
            f.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * 拷贝文件
     *@param srcPath 绝对路径
     * @param destDir 目标文件所在目录
     * @return boolean true拷贝成功
     */
    public static boolean copyFile(String srcPath, String destDir){
        boolean flag = false;
        File srcFile = new File(srcPath); // 源文件
        if (!srcFile.exists()){
            Log.i("FileUtils is copyFile：","源文件不存在");
            return false;
        }
        // 获取待复制文件的文件名
        String fileName = srcPath.substring(srcPath.lastIndexOf(File.separator));
        String destPath = destDir + fileName;
        if (destPath.equals(srcPath)){
            Log.i("FileUtils is copyFile：","源文件路径和目标文件路径重复");
            return false;
        }
        File destFile = new File(destPath); // 目标文件
        if (destFile.exists() && destFile.isFile()){
            Log.i("FileUtils is copyFile：","该路径下已经有一个同名文件");
            return false;
        }
        File destFileDir = new File(destDir);
        destFileDir.mkdirs();
        try{
            FileInputStream fis = new FileInputStream(srcPath);
            FileOutputStream fos = new FileOutputStream(destFile);
            byte[] buf = new byte[1024];
            int c;
            while ((c = fis.read(buf)) != -1) {
                fos.write(buf, 0, c);
            }
            fis.close();
            fos.close();
            flag = true;
        }catch (IOException e){
            e.printStackTrace();
        }
        return flag;
    }

    /**
     * 重命名文件
     *@param oldPath 旧文件的绝对路径
     * @param newPath 新文件的绝对路径
     * @return 文件重命名成功则返回true
     */
    public static boolean renameTo(String oldPath, String newPath){
        if (oldPath.equals(newPath)){
            Log.i("FileUtils is renameTo：","文件重命名失败：新旧文件名绝对路径相同");
            return false;
        }
        File oldFile = new File(oldPath);
        File newFile = new File(newPath);

        return oldFile.renameTo(newFile);
    }

    /**
     * 计算某个文件的大小
     *@param path 文件的绝对路径
     *@return 文件大小
     */
    public static long getFileSize(String path){
        File file = new File(path);
        return file.length();
    }

    /**
     *计算某个文件夹的大小
     *@param  file 目录所在绝对路径
     * @return 文件夹的大小
     */
    public static double getDirSize(File file) {
        if (file.exists()) {
            //如果是目录则递归计算其内容的总大小
            if (file.isDirectory()) {
                File[] children = file.listFiles();
                double size = 0;
                for (File f : children)
                    size += getDirSize(f);
                return size;
            } else {//如果是文件则直接返回其大小,以“兆”为单位
                return (double) file.length() / 1024 / 1024;
            }
        } else {
            return 0.0;
        }
    }

    /**
     * 获取某个路径下的文件列表
     * @param path 文件路径
     * @return 文件列表File[] files
     */
    public static File[] getFileList(String path) {
        File file = new File(path);
        if (file.isDirectory()){
            File[] files = file.listFiles();
            if (files != null){
                return files;
            }else{
                return null;
            }
        }else{
            return null;
        }
    }

    /**
     * 计算某个目录包含的文件数量
     *@param path 目录的绝对路径
     * @return  文件数量
     */
    public static int getFileCount(String path){
        File directory = new File(path);
        File[] files = directory.listFiles();
        return files.length;
    }

    /**
     * 获取SDCard 总容量大小(MB)
     *@param path 目录的绝对路径
     * @return 总容量大小
     * */
    public long getSDCardTotal(String path){

        if(null != path&&path.equals("")){

            StatFs statfs = new StatFs(path);
            //获取SDCard的Block总数
            long totalBlocks = statfs.getBlockCount();
            //获取每个block的大小
            long blockSize = statfs.getBlockSize();
            //计算SDCard 总容量大小MB
            return totalBlocks*blockSize/1024/1024;

        }else{
            return 0;
        }
    }

    /**
     * 获取SDCard 可用容量大小(MB)
     *@param path 目录的绝对路径
     * @return 可用容量大小
     * */
    public long getSDCardFree(String path){

        if(null != path&&path.equals("")){

            StatFs statfs = new StatFs(path);
            //获取SDCard的Block可用数
            long availaBlocks = statfs.getAvailableBlocks();
            //获取每个block的大小
            long blockSize = statfs.getBlockSize();
            //计算SDCard 可用容量大小MB
            return availaBlocks*blockSize/1024/1024;

        }else{
            return 0;
        }
    }

    public static String getInStoragePath(Context mContext){
        String str;
        str = getStoragePath(mContext, false);
        return str;
    }

    public static String getOutStoragePath(Context mContext){
        String str;
        str = getStoragePath(mContext, true);
        return str;
    }

    /**
     * 通过反射调用获取内置存储和外置sd卡根路径(通用)
     *
     * @param mContext    上下文
     * @param is_removale 是否可移除，输入false则返回内部存储路径，输入true则返回外置SD卡路径
     * @return
     */
    public static String getStoragePath(Context mContext, boolean is_removale) {
        String path = "";
        //使用getSystemService(String)检索一个StorageManager用于访问系统存储功能。
        StorageManager mStorageManager = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
        Class<?> storageVolumeClazz = null;
        try {
            storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");
            Method getVolumeList = mStorageManager.getClass().getMethod("getVolumeList");
            Method getPath = storageVolumeClazz.getMethod("getPath");
            Method isRemovable = storageVolumeClazz.getMethod("isRemovable");
            Object result = getVolumeList.invoke(mStorageManager);

            for (int i = 0; i < Array.getLength(result); i++) {
                Object storageVolumeElement = Array.get(result, i);
                path = (String) getPath.invoke(storageVolumeElement);
                boolean removable = (Boolean) isRemovable.invoke(storageVolumeElement);
                if (is_removale == removable) {
                    return path;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return path;
    }
}
