package com.zjun.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.support.annotation.FloatRange;
import android.widget.Toast;

import java.io.InputStream;

/**
 * 常用工具类
 * Created by Ralap on 2016/3/28.
 */
public class CommonTool {

    private static Toast mToast;

    public static int getColor(Context context, int resId) {
        return context.getResources().getColor(resId);
    }

    public static Bitmap getBitmap(Context context, int resId) {
        InputStream is = context.getResources().openRawResource(resId);
        return BitmapFactory.decodeStream(is);
    }
    public static int dp2px(Context context, int dp){
        float density = context.getResources().getDisplayMetrics().density;
        return (int) (density * dp + .5f);
    }

    public static int px2dp(Context context, int px){
        float density = context.getResources().getDisplayMetrics().density;
        return (int) (density / px + .5f);
    }


    /**
     * 根据百分比，估算在指定范围内的值
     * @param start
     * @param end
     * @param percent
     * @return
     */
    public static int estimateInt(int start ,int end, @FloatRange(from = 0.0f, to = 1.0f) float percent) {
        return (int) (start + percent * (end - start));
    }

    public static float estimateFloat(float start ,float end, @FloatRange(from = 0.0f, to = 1.0f) float percent) {
        return  start + percent * (end - start);
    }

    /**
     * 估算给定值在指定范围内的百分比
     * @param start
     * @param end
     * @param value
     * @return 0.0f ~ 1.0f。如果没有指定范围，或给定值不在范围内则返回-1
     */
    public static float estimatePercent(float start, float end, float value) {
        if (start == end
                || (value < start && value < end)
                || (value > start && value > end)){
            return -1;
        }
        return (value - start) / (end - start);
    }

    public static void toast(Context context, String msg) {
        if (mToast == null) {
            mToast = Toast.makeText(context, msg, Toast.LENGTH_SHORT);
        }else{
            mToast.setText(msg);
        }
        mToast.show();
    }
}
