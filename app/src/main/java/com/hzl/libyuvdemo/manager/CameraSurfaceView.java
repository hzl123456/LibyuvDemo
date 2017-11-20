package com.hzl.libyuvdemo.manager;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.hzl.libyuvdemo.R;
import com.hzl.libyuvdemo.listener.CameraSurfaceListener;
import com.hzl.libyuvdemo.listener.CameraYUVDataListener;
import com.hzl.libyuvdemo.util.CameraUtil;

/**
 * 作者：请叫我百米冲刺 on 2017/11/7 上午10:24
 * 邮箱：mail@hezhilin.cc
 * <p>
 * 将camera和sufaceview结合起来的控件
 */
@SuppressWarnings("deprecation")
public class CameraSurfaceView extends FrameLayout implements CameraSurfaceListener, Camera.PreviewCallback, SurfaceHolder.Callback {

    private SurfaceView mSurfaceView;
    private ImageView ivFoucView;
    private CameraUtil mCameraUtil;
    private double pointLength;  //双指刚按下去时候的距离

    private CameraYUVDataListener listener;

    private double mTargetAspect = -1.0;


    public void setCameraYUVDataListener(CameraYUVDataListener listener) {
        this.listener = listener;
    }

    public CameraSurfaceView(Context context) {
        super(context);
        init();
    }

    public CameraSurfaceView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init();
    }

    public CameraSurfaceView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        init();
    }

    private void init() {
        View view = View.inflate(getContext(), R.layout.layout_camera, null);
        mSurfaceView = (SurfaceView) view.findViewById(R.id.surface);
        ivFoucView = (ImageView) view.findViewById(R.id.iv_focus);
        removeAllViews();
        addView(view);

        //创建所需要的camera和surfaceview
        mCameraUtil = new CameraUtil();
        mSurfaceView.getHolder().addCallback(this);
    }

    public CameraUtil getCameraUtil() {
        return mCameraUtil;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        camera.addCallbackBuffer(data);
        //进行回调
        if (listener != null) {
            listener.onCallback(data);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mCameraUtil.handleCameraStartPreview(mSurfaceView.getHolder(), this);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mCameraUtil.releaseCamera();
    }


    @Override
    public void openCamera() {
        mCameraUtil.openCamera(mCameraUtil.getCurrentCameraType());
        mSurfaceView.post(new Runnable() {
            @Override
            public void run() {
                mCameraUtil.handleCameraStartPreview(mSurfaceView.getHolder(), CameraSurfaceView.this);
                //这里可以获取真正的预览的分辨率，在这里要进行屏幕的适配，主要适配非16:9的屏幕
                mTargetAspect = ((float) mCameraUtil.getCameraHeight()) / mCameraUtil.getCameraWidth();
                CameraSurfaceView.this.measure(-1, -1);
            }
        });
    }

    @Override
    public void releaseCamera() {
        mCameraUtil.releaseCamera();
    }


    @Override
    public int changeCamera() {
        mCameraUtil.releaseCamera();
        if (mCameraUtil.getCurrentCameraType() == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            mCameraUtil.setCurrentCameraType(Camera.CameraInfo.CAMERA_FACING_BACK);
        } else {
            mCameraUtil.setCurrentCameraType(Camera.CameraInfo.CAMERA_FACING_FRONT);
        }
        openCamera();
        return mCameraUtil.getCurrentCameraType();
    }

    @Override
    public void startAutoFocus(float x, float y) {
        //后置摄像头才有对焦功能
        if (mCameraUtil != null && mCameraUtil.getCurrentCameraType() == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            return;
        }
        if (x != -1 && y != -1) { //这里有一个对焦的动画
            //设置位置和初始状态
            ivFoucView.setTranslationX(x - (ivFoucView.getWidth()) / 2);
            ivFoucView.setTranslationY(y - (ivFoucView.getWidth()) / 2);
            ivFoucView.clearAnimation();

            //执行动画
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(ivFoucView, "scaleX", 1.75f, 1.0f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(ivFoucView, "scaleY", 1.75f, 1.0f);
            AnimatorSet animSet = new AnimatorSet();
            animSet.play(scaleX).with(scaleY);
            animSet.setDuration(500);
            animSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    ivFoucView.setVisibility(VISIBLE);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    ivFoucView.setVisibility(GONE);
                }
            });
            animSet.start();
        }
        mCameraUtil.startAutoFocus();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mTargetAspect > 0) {
            int initialWidth = MeasureSpec.getSize(widthMeasureSpec);
            int initialHeight = MeasureSpec.getSize(heightMeasureSpec);

            int horizPadding = getPaddingLeft() + getPaddingRight();
            int vertPadding = getPaddingTop() + getPaddingBottom();
            initialWidth -= horizPadding;
            initialHeight -= vertPadding;

            double viewAspectRatio = (double) initialWidth / initialHeight;
            double aspectDiff = mTargetAspect / viewAspectRatio - 1;

            if (Math.abs(aspectDiff) < 0.01) {
            } else {
                if (aspectDiff > 0) {
                    initialHeight = (int) (initialWidth / mTargetAspect);
                } else {
                    initialWidth = (int) (initialHeight * mTargetAspect);
                }
                initialWidth += horizPadding;
                initialHeight += vertPadding;
                widthMeasureSpec = MeasureSpec.makeMeasureSpec(initialWidth, MeasureSpec.EXACTLY);
                heightMeasureSpec = MeasureSpec.makeMeasureSpec(initialHeight, MeasureSpec.EXACTLY);
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getPointerCount() == 1 && event.getAction() == MotionEvent.ACTION_DOWN) {
            startAutoFocus(event.getX(), event.getY());
        }
        return true;
    }
}
