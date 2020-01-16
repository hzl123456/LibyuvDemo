package com.hzl.libyuvdemo.manager;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.hzl.libyuvdemo.MainApplication;
import com.hzl.libyuvdemo.contacts.Contacts;
import com.hzl.libyuvdemo.listener.CameraPictureListener;
import com.hzl.libyuvdemo.listener.CameraYUVDataListener;
import com.hzl.libyuvdemo.util.CameraUtil;
import com.hzl.libyuvdemo.util.SPUtil;
import com.libyuv.util.YuvUtil;

import java.io.ByteArrayOutputStream;

import static android.content.Context.SENSOR_SERVICE;

/**
 * 作者：请叫我百米冲刺 on 2017/11/7 上午10:52
 * 邮箱：mail@hezhilin.cc
 */
@SuppressWarnings("deprecation")
public class CameraSurfaceManager implements SensorEventListener, CameraYUVDataListener {

    private CameraSurfaceView mCameraSurfaceView;
    private CameraUtil mCameraUtil;
    private boolean isTakingPicture;
    private boolean isRunning;
    private CameraPictureListener listener;

    private int cameraWidth;
    private int cameraHeight;
    private int scaleWidth;
    private int scaleHeight;
    private int cropStartX;
    private int cropStartY;
    private int cropWidth;
    private int cropHeight;

    //传感器需要，这边使用的是重力传感器
    private SensorManager mSensorManager;
    //第一次实例化的时候是不需要的
    private boolean mInitialized = false;
    private float mLastX = 0f;
    private float mLastY = 0f;
    private float mLastZ = 0f;

    public CameraSurfaceManager(CameraSurfaceView cameraSurfaceView) {
        mCameraSurfaceView = cameraSurfaceView;
        mCameraUtil = cameraSurfaceView.getCameraUtil();
        mCameraSurfaceView.setCameraYUVDataListener(this);

        mSensorManager = (SensorManager) MainApplication.getInstance().getSystemService(SENSOR_SERVICE);
    }

    public void setCameraPictureListener(CameraPictureListener listener) {
        this.listener = listener;
    }

    public int changeCamera() {
        return mCameraSurfaceView.changeCamera();
    }

    public void takePicture() {
        if (isSupport()) {
            isTakingPicture = true;
        }
    }

    public void onResume() {
        //打开摄像头
        mCameraSurfaceView.openCamera();
        //注册加速度传感器
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI);
    }

    public void onStop() {
        //释放摄像头
        mCameraSurfaceView.releaseCamera();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onCallback(final byte[] srcData) {
        //进行一次拍照
        if (isTakingPicture && !isRunning) {
            isTakingPicture = false;
            isRunning = true;
            new Thread(new Runnable() {

                @Override
                public void run() {
                    //进行yuv数据的缩放，旋转镜像缩放等操作
                    final byte[] dstData = new byte[scaleWidth * scaleHeight * 3 / 2];
                    final int morientation = mCameraUtil.getMorientation();
                    YuvUtil.compressYUV(srcData, cameraWidth, cameraHeight, dstData, scaleHeight, scaleWidth, 0, morientation, morientation == 270);

                    //进行yuv数据裁剪的操作
                    final byte[] cropData = new byte[cropWidth * cropHeight * 3 / 2];
                    YuvUtil.cropYUV(dstData, scaleWidth, scaleHeight, cropData, cropWidth, cropHeight, cropStartX, cropStartY);

                    //这里将yuvi420转化为nv21，因为yuvimage只能操作nv21和yv12，为了演示方便，这里做一步转化的操作
                    final byte[] nv21Data = new byte[cropWidth * cropHeight * 3 / 2];
                    YuvUtil.yuvI420ToNV21(cropData, nv21Data, cropWidth, cropHeight);

                    //这里采用yuvImage将yuvi420转化为图片，当然用libyuv也是可以做到的，这里主要介绍libyuv的裁剪，旋转，缩放，镜像的操作
                    YuvImage yuvImage = new YuvImage(nv21Data, ImageFormat.NV21, cropWidth, cropHeight, null);
                    ByteArrayOutputStream fOut = new ByteArrayOutputStream();
                    yuvImage.compressToJpeg(new Rect(0, 0, cropWidth, cropHeight), 100, fOut);

                    //将byte生成bitmap
                    byte[] bitData = fOut.toByteArray();
                    final Bitmap bitmap = BitmapFactory.decodeByteArray(bitData, 0, bitData.length);
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            if (listener != null) {
                                listener.onPictureBitmap(bitmap);
                            }
                            isRunning = false;
                        }
                    });
                }
            }).start();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        if (!mInitialized) {
            mLastX = x;
            mLastY = y;
            mLastZ = z;
            mInitialized = true;
        }

        float deltaX = Math.abs(mLastX - x);
        float deltaY = Math.abs(mLastY - y);
        float deltaZ = Math.abs(mLastZ - z);

        if (mCameraSurfaceView != null && (deltaX > 0.6 || deltaY > 0.6 || deltaZ > 0.6)) {
            mCameraSurfaceView.startAutoFocus(-1, -1);
        }

        mLastX = x;
        mLastY = y;
        mLastZ = z;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    //主要是对裁剪的判断
    private boolean isSupport() {
        cropWidth = (int) SPUtil.get(Contacts.CROP_WIDTH, 720);
        cropHeight = (int) SPUtil.get(Contacts.CROP_HEIGHT, 720);
        cropStartX = (int) SPUtil.get(Contacts.CROP_START_X, 0);
        cropStartY = (int) SPUtil.get(Contacts.CROP_START_Y, 0);

        int cameraWidth = mCameraUtil.getCameraWidth();
        int cameraHeight = mCameraUtil.getCameraHeight();
        int scaleWidth = (int) SPUtil.get(Contacts.SCALE_WIDTH, 720);
        int scaleHeight = (int) SPUtil.get(Contacts.SCALE_HEIGHT, 1280);

        // 初始化，输入输出宽高不变的情况下只需要初始化一次
        if (this.cameraWidth != cameraWidth || this.cameraHeight != cameraHeight || this.scaleWidth != scaleWidth || this.scaleHeight != scaleHeight) {
            YuvUtil.init(cameraWidth, cameraHeight, scaleHeight, scaleWidth);
            this.cameraWidth = cameraWidth;
            this.cameraHeight = cameraHeight;
            this.scaleWidth = scaleWidth;
            this.scaleHeight = scaleHeight;
        }

        if (cropStartX % 2 != 0 || cropStartY % 2 != 0) {
            Toast.makeText(MainApplication.getInstance(), "裁剪的开始位置必须为偶数", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (cropStartX + cropWidth > scaleWidth || cropStartY + cropHeight > scaleHeight) {
            Toast.makeText(MainApplication.getInstance(), "裁剪区域超出范围", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
}
