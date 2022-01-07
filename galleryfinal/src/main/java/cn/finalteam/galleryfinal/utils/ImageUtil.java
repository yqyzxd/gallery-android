package cn.finalteam.galleryfinal.utils;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by shi on 2015/9/18.
 */
public class ImageUtil {

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;
        if(height > reqHeight || width > reqWidth) {
            if(width < height) {
                inSampleSize = Math.round((float)height / (float)reqHeight);
            } else {
                inSampleSize = Math.round((float)width / (float)reqWidth);
            }
        }

        if(inSampleSize != 1 && inSampleSize % 2 > 0) {
            ++inSampleSize;
        }

        return inSampleSize;
    }

    public static BitmapFactory.Options getImageSourceOptions(BitmapFactory.Options opts, File imageFile) {
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imageFile.getPath(), opts);
        return opts;
    }

    public static BitmapFactory.Options getImageSourceOptions(BitmapFactory.Options opts, InputStream in) {
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(in, (Rect)null, opts);
        return opts;
    }

    public static int getImageAngle(String path) {
        short angle = 0;

        try {
            ExifInterface e = new ExifInterface(path);
            switch(e.getAttributeInt("Orientation", 0)) {
                case 3:
                    angle = 180;
                case 4:
                case 5:
                case 7:
                default:
                    break;
                case 6:
                    angle = 90;
                    break;
                case 8:
                    angle = 270;
            }
        } catch (IOException var3) {
            var3.printStackTrace();
        }

        return angle;
    }

    public static int getUriImageAngel(Context context, Uri imageUri) {
        Cursor cursor = null;
        int angle = 0;

        try {
            String[] e = new String[]{"_data", "orientation"};
            ContentResolver cr = context.getContentResolver();
            cursor = cr.query(imageUri, e, (String)null, (String[])null, (String)null);
            if(cursor != null) {
                cursor.moveToFirst();
                int orientationIndex = cursor.getColumnIndex(e[1]);
                String orientation = cursor.getString(orientationIndex);
                if(orientation != null) {
                    if(orientation.equals("90")) {
                        angle = 90;
                    } else if(orientation.equals("180")) {
                        angle = 180;
                    } else if(orientation.equals("270")) {
                        angle = 270;
                    }
                } else {
                    angle = readBmpDegree(getRealPath(context, imageUri));
                }
            }
        } catch (Exception var11) {
            var11.printStackTrace();
        } finally {
            if(cursor != null) {
                cursor.close();
                cursor = null;
            }

        }

        return angle;
    }

    @SuppressLint({"NewApi"})
    public static String getRealPath(Context context, Uri uri) {
        if(uri.toString().startsWith("content://com.google.android.apps.photos.content")) {
            return null;
        } else {
            boolean isKitKat = Build.VERSION.SDK_INT >= 19;
            if(isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
                String docId;
                String[] split;
                String type;
                if(isExternalStorageDocument(uri)) {
                    docId = DocumentsContract.getDocumentId(uri);
                    split = docId.split(":");
                    type = split[0];
                    if("primary".equalsIgnoreCase(type)) {
                        return Environment.getExternalStorageDirectory() + "/" + split[1];
                    }
                } else {
                    if(isDownloadsDocument(uri)) {
                        docId = DocumentsContract.getDocumentId(uri);
                        Uri split1 = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId).longValue());
                        return getDataColumn(context, split1, (String)null, (String[])null);
                    }

                    if(isMediaDocument(uri)) {
                        docId = DocumentsContract.getDocumentId(uri);
                        split = docId.split(":");
                        type = split[0];
                        Uri contentUri = null;
                        if("image".equals(type)) {
                            contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                        } else if("video".equals(type)) {
                            contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                        } else if("audio".equals(type)) {
                            contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                        }

                        String selection = "_id=?";
                        String[] selectionArgs = new String[]{split[1]};
                        return getDataColumn(context, contentUri, "_id=?", selectionArgs);
                    }
                }
            } else {
                if("content".equalsIgnoreCase(uri.getScheme())) {
                    return getDataColumn(context, uri, (String)null, (String[])null);
                }

                if("file".equalsIgnoreCase(uri.getScheme())) {
                    return uri.getPath();
                }
            }

            return null;
        }
    }

    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        String column = "_data";
        String[] projection = new String[]{"_data"};

        String var9;
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, (String)null);
            if(cursor == null || !cursor.moveToFirst()) {
                return null;
            }

            int column_index = cursor.getColumnIndexOrThrow("_data");
            var9 = cursor.getString(column_index);
        } finally {
            if(cursor != null) {
                cursor.close();
            }

        }

        return var9;
    }

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    private static int readBmpDegree(String path) {
        short degree = 0;

        try {
            ExifInterface e = new ExifInterface(path);
            int orientation = e.getAttributeInt("Orientation", 1);
            switch(orientation) {
                case 3:
                    degree = 180;
                case 4:
                case 5:
                case 7:
                default:
                    break;
                case 6:
                    degree = 90;
                    break;
                case 8:
                    degree = 270;
            }

            return degree;
        } catch (IOException var4) {
            var4.printStackTrace();
            return degree;
        }
    }

    public static Bitmap decodeSampledBitmapFromFile(String filename, int reqWidth, int reqHeight, int rotateAngel) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filename, options);
        int inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight, rotateAngel);
        options.inSampleSize = inSampleSize;
        options.inJustDecodeBounds = false;
        System.gc();

        try {
            Bitmap e = BitmapFactory.decodeFile(filename, options);
            if(rotateAngel != 0) {
                Matrix matrix = new Matrix();
                matrix.setRotate((float)rotateAngel, (float)(e.getWidth() / 2), (float)(e.getHeight() / 2));
                Bitmap result = Bitmap.createBitmap(e, 0, 0, e.getWidth(), e.getHeight(), matrix, true);
                e.recycle();
                e = result;
            }

            return e;
        } catch (Exception var9) {
            var9.printStackTrace();
            return null;
        } catch (OutOfMemoryError var10) {
            var10.printStackTrace();
            return null;
        }
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight, int rotateAngel) {
        int height;
        int width;
        if(rotateAngel != 0 && rotateAngel != 180) {
            height = options.outWidth;
            width = options.outHeight;
        } else {
            height = options.outHeight;
            width = options.outWidth;
        }

        int inSampleSize = 1;
        if(height > reqHeight || width > reqWidth) {
            if(width < height) {
                inSampleSize = (int)Math.ceil((double)((float)height / (float)reqHeight));
            } else {
                inSampleSize = (int)Math.ceil((double)((float)width / (float)reqWidth));
            }
        }

        return inSampleSize;
    }

    public static void recyleBitmap(Bitmap bitmap) {
        if(bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }

    }

    public static void notifyAlbumInsertToContentProvider(Context context, File imageFile) {
        try {
            ContentValues e1 = new ContentValues();
            e1.put("_data", imageFile.toString());
            e1.put("description", "camera image");
            e1.put("mime_type", "image/png");
            long e2 = System.currentTimeMillis() / 1000L;
            e1.put("date_added", Long.valueOf(e2));
            e1.put("date_modified", Long.valueOf(e2));
            ContentResolver localContentResolver = context.getContentResolver();
            Uri localUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            localContentResolver.insert(localUri, e1);
        } catch (Exception var8) {
            Exception e = var8;

            try {
                //Log.e("notifyAlbumInsertToContentProvider", "notifyAlbumInsertToContentProvider error", e);
                MediaScannerConnection.scanFile(context, new String[]{imageFile.getPath()}, new String[]{"png"}, (MediaScannerConnection.OnScanCompletedListener) null);
            } catch (Exception var7) {
                //Log.e("notifyAlbumInsertToContentProvider", "scanFile error", var7);
                notifyAlbum(context, imageFile);
            }
        }

    }

    public static void notifyAlbum(Context context, File imageFile) {
        Uri localUri = Uri.fromFile(imageFile);
        Intent localIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE", localUri);
        context.sendBroadcast(localIntent);
    }

    @SuppressLint("NewApi")
    public static Bitmap fastblur(Context context, Bitmap sentBitmap, int radius) {


        Bitmap bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);

        if (radius < 1) {
            return (null);
        }

        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        int[] pix = new int[w * h];
//        Log.e("pix", w + " " + h + " " + pix.length);
        bitmap.getPixels(pix, 0, w, 0, 0, w, h);

        int wm = w - 1;
        int hm = h - 1;
        int wh = w * h;
        int div = radius + radius + 1;

        int r[] = new int[wh];
        int g[] = new int[wh];
        int b[] = new int[wh];
        int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
        int vmin[] = new int[Math.max(w, h)];

        int divsum = (div + 1) >> 1;
        divsum *= divsum;
        int temp = 256 * divsum;
        int dv[] = new int[temp];
        for (i = 0; i < temp; i++) {
            dv[i] = (i / divsum);
        }

        yw = yi = 0;

        int[][] stack = new int[div][3];
        int stackpointer;
        int stackstart;
        int[] sir;
        int rbs;
        int r1 = radius + 1;
        int routsum, goutsum, boutsum;
        int rinsum, ginsum, binsum;

        for (y = 0; y < h; y++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            for (i = -radius; i <= radius; i++) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))];
                sir = stack[i + radius];
                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);
                rbs = r1 - Math.abs(i);
                rsum += sir[0] * rbs;
                gsum += sir[1] * rbs;
                bsum += sir[2] * rbs;
                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }
            }
            stackpointer = radius;

            for (x = 0; x < w; x++) {

                r[yi] = dv[rsum];
                g[yi] = dv[gsum];
                b[yi] = dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm);
                }
                p = pix[yw + vmin[x]];

                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[(stackpointer) % div];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi++;
            }
            yw += w;
        }
        for (x = 0; x < w; x++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            yp = -radius * w;
            for (i = -radius; i <= radius; i++) {
                yi = Math.max(0, yp) + x;

                sir = stack[i + radius];

                sir[0] = r[yi];
                sir[1] = g[yi];
                sir[2] = b[yi];

                rbs = r1 - Math.abs(i);

                rsum += r[yi] * rbs;
                gsum += g[yi] * rbs;
                bsum += b[yi] * rbs;

                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }

                if (i < hm) {
                    yp += w;
                }
            }
            yi = x;
            stackpointer = radius;
            for (y = 0; y < h; y++) {
                // Preserve alpha channel: ( 0xff000000 & pix[yi] )
                pix[yi] = (0xff000000 & pix[yi]) | (dv[rsum] << 16) | (dv[gsum] << 8) | dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (x == 0) {
                    vmin[y] = Math.min(y + r1, hm) * w;
                }
                p = x + vmin[y];

                sir[0] = r[p];
                sir[1] = g[p];
                sir[2] = b[p];

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[stackpointer];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi += w;
            }
        }

//        Log.e("pix", w + " " + h + " " + pix.length);
        bitmap.setPixels(pix, 0, w, 0, 0, w, h);
        return (bitmap);
    }



    /**
     * 读取图片的旋转的角度
     *
     * @param path
     *            图片绝对路径
     * @return 图片的旋转角度
     */
    public static int getBitmapDegree(String path) {
        int degree = 0;
        try {
            // 从指定路径下读取图片，并获取其EXIF信息
            ExifInterface exifInterface = new ExifInterface(path);
            // 获取图片的旋转信息
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }

    /**
     * 将图片按照某个角度进行旋转
     *
     * @param bm
     *            需要旋转的图片
     * @param degree
     *            旋转角度
     * @return 旋转后的图片
     */
    public static Bitmap rotateBitmapByDegree(Bitmap bm, int degree) {
        Bitmap returnBm = null;

        // 根据旋转角度，生成旋转矩阵
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        try {
            // 将原始图片按照旋转矩阵进行旋转，并得到新的图片
            returnBm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
        } catch (OutOfMemoryError e) {
        }
        if (returnBm == null) {
            returnBm = bm;
        }
        if (bm != returnBm) {
            bm.recycle();
        }
        return returnBm;
    }


    public static String getWebViewUploadDirPath() {
        return Environment.getExternalStorageDirectory().getPath() + "/WebViewUploadImage";
    }

    public static Intent choosePicture() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        return Intent.createChooser(intent, null);
    }

    public static Intent takeBigPicture() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, newPictureUri(getNewPhotoPath()));
        return intent;
    }

    private static Uri newPictureUri(String path) {
        return Uri.fromFile(new File(path));
    }
    private static String getNewPhotoPath() {
        return getWebViewUploadDirPath() + "/" + System.currentTimeMillis() + ".jpg";
    }
}
