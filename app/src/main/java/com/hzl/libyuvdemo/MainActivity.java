package com.hzl.libyuvdemo;

import android.Manifest;
import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.hzl.libyuvdemo.contacts.Contacts;
import com.hzl.libyuvdemo.listener.CameraPictureListener;
import com.hzl.libyuvdemo.manager.CameraSurfaceManager;
import com.hzl.libyuvdemo.manager.CameraSurfaceView;
import com.hzl.libyuvdemo.util.PermissionsUtils;
import com.hzl.libyuvdemo.util.SPUtil;

public class MainActivity extends Activity implements View.OnClickListener, CameraPictureListener {

    private final int REQUEST_CODE_PERMISSIONS = 10;
    private CameraSurfaceManager manager;
    private CameraSurfaceView mSurfaceView;
    private ImageView mBtnCamera;
    private ImageView mBtnPicture;
    private ImageView mBtnClose;
    private ImageView ivImage;

    public TextView tvCameraInfo;
    public EditText etScaleWidth;
    public TextView tvScaleHeight;
    public EditText etCropStartX;
    public EditText etCropStartY;
    public EditText etCropWidth;
    public EditText etCropHeight;

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

        //图片的相关信息
        tvCameraInfo = (TextView) findViewById(R.id.tv_camera_info);
        tvScaleHeight = (TextView) findViewById(R.id.tv_scale_height);
        etScaleWidth = (EditText) findViewById(R.id.et_scale_width);
        etCropStartX = (EditText) findViewById(R.id.et_crop_start_x);
        etCropStartY = (EditText) findViewById(R.id.et_crop_start_y);
        etCropWidth = (EditText) findViewById(R.id.et_crop_width);
        etCropHeight = (EditText) findViewById(R.id.et_crop_height);
        //edittext输入的监听
        editTextWatch();

        mBtnClose.setOnClickListener(this);
        mBtnCamera.setOnClickListener(this);
        mBtnPicture.setOnClickListener(this);

        manager = new CameraSurfaceManager(mSurfaceView);
        manager.setCameraPictureListener(this);
    }

    private void initCameraInfo() {
        //摄像头预览设置
        int width = (int) SPUtil.get(Contacts.CAMERA_WIDTH, 0);
        int height = (int) SPUtil.get(Contacts.CAMERA_HEIGHT, 0);
        int morientation = (int) SPUtil.get(Contacts.CAMERA_Morientation, 0);
        tvCameraInfo.setText(String.format("摄像头预览大小:%d*%d\n旋转的角度:%d度", width, height, morientation));

        //缩放大小设置
        etScaleWidth.setText(SPUtil.get(Contacts.SCALE_WIDTH, 720).toString());
        tvScaleHeight.setText(SPUtil.get(Contacts.SCALE_HEIGHT, 1280).toString());

        //裁剪的设置
        etCropStartX.setText(SPUtil.get(Contacts.CROP_START_X, 0).toString());
        etCropStartY.setText(SPUtil.get(Contacts.CROP_START_Y, 0).toString());
        etCropWidth.setText(SPUtil.get(Contacts.CROP_WIDTH, 720).toString());
        etCropHeight.setText(SPUtil.get(Contacts.CROP_HEIGHT, 720).toString());
    }

    private void editTextWatch() {
        //缩放宽高的设置
        etScaleWidth.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!TextUtils.isEmpty(etScaleWidth.getText().toString())) {
                    int scaleWidth = Integer.parseInt(etScaleWidth.getText().toString());
                    tvScaleHeight.setText(String.valueOf((int) (scaleWidth * 16 / (float) 9)));
                }
            }
        });

    }

    @Override
    public void onPictureBitmap(Bitmap btmp) {
        ivImage.setImageBitmap(btmp);

        mBtnClose.setVisibility(View.VISIBLE);
        ivImage.setVisibility(View.VISIBLE);
    }

    @Override
    public void onClick(View v) {
        if (v == mBtnCamera) {//切换摄像头
            manager.changeCamera();
            initCameraInfo();
        } else if (v == mBtnPicture) {//进行拍照
            //进行值的设置
            SPUtil.put(Contacts.SCALE_WIDTH, Integer.parseInt(etScaleWidth.getText().toString()));
            SPUtil.put(Contacts.SCALE_HEIGHT, Integer.parseInt(tvScaleHeight.getText().toString()));
            SPUtil.put(Contacts.CROP_WIDTH, Integer.parseInt(etCropWidth.getText().toString()));
            SPUtil.put(Contacts.CROP_HEIGHT, Integer.parseInt(etCropHeight.getText().toString()));
            SPUtil.put(Contacts.CROP_START_X, Integer.parseInt(etCropStartX.getText().toString()));
            SPUtil.put(Contacts.CROP_START_Y, Integer.parseInt(etCropStartY.getText().toString()));


            manager.takePicture();
        } else if (v == mBtnClose) { //关闭显示的图片
            ivImage.setVisibility(View.GONE);
            mBtnClose.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        manager.onResume();
        initCameraInfo();
    }

    @Override
    protected void onStop() {
        super.onStop();
        manager.onStop();
    }
}
