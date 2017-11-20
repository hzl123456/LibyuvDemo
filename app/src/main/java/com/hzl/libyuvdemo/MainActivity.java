package com.hzl.libyuvdemo;

import android.Manifest;
import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.hzl.libyuvdemo.listener.CameraPictureListener;
import com.hzl.libyuvdemo.manager.CameraSurfaceManager;
import com.hzl.libyuvdemo.manager.CameraSurfaceView;
import com.hzl.libyuvdemo.util.PermissionsUtils;

public class MainActivity extends Activity implements View.OnClickListener, CameraPictureListener {

    private final int REQUEST_CODE_PERMISSIONS = 10;
    private CameraSurfaceManager manager;
    private CameraSurfaceView mSurfaceView;
    private ImageView mBtnCamera;
    private ImageView mBtnPicture;
    private ImageView mBtnClose;
    private ImageView ivImage;
    private TextView tvImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MainApplication.setCurrentActivity(this);
        //设置底部虚拟状态栏为透明，并且可以充满，4.4以上才有
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }
        //权限申请使用
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            final String[] PERMISSIONS = new String[]{Manifest.permission.CAMERA};
            PermissionsUtils.checkAndRequestMorePermissions(this, PERMISSIONS, REQUEST_CODE_PERMISSIONS,
                    new PermissionsUtils.PermissionRequestSuccessCallBack() {
                        @Override
                        public void onHasPermission() {
                            setContentView(R.layout.activity_main);
                            initView();
                        }
                    });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (PermissionsUtils.isPermissionRequestSuccess(grantResults)) {
            setContentView(R.layout.activity_main);
            initView();
        }
    }

    private void initView() {
        mSurfaceView = (CameraSurfaceView) findViewById(R.id.camera_surface);
        mBtnCamera = (ImageView) findViewById(R.id.btn_camera);
        mBtnPicture = (ImageView) findViewById(R.id.btn_take_picture);
        mBtnClose = (ImageView) findViewById(R.id.btn_close);
        ivImage = (ImageView) findViewById(R.id.iv_image);
        tvImage = (TextView) findViewById(R.id.tv_image);

        mBtnClose.setOnClickListener(this);
        mBtnCamera.setOnClickListener(this);
        mBtnPicture.setOnClickListener(this);

        manager = new CameraSurfaceManager(mSurfaceView);
        manager.setCameraPictureListener(this);
    }

    @Override
    public void onPictureBitmap(Bitmap btmp) {
        ivImage.setImageBitmap(btmp);
        tvImage.setText(String.format("Size:%d*%d", btmp.getWidth(), btmp.getHeight()));

        mBtnClose.setVisibility(View.VISIBLE);
        tvImage.setVisibility(View.VISIBLE);
        ivImage.setVisibility(View.VISIBLE);
    }

    @Override
    public void onClick(View v) {
        if (v == mBtnCamera) {//切换摄像头
            manager.changeCamera();
        } else if (v == mBtnPicture) {//进行拍照
            manager.takePicture();
        } else if (v == mBtnClose) { //关闭显示的图片
            ivImage.setVisibility(View.GONE);
            tvImage.setVisibility(View.GONE);
            mBtnClose.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        manager.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        manager.onStop();
    }

}
