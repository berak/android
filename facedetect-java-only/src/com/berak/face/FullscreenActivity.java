package com.berak.face;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.EditText;
import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;


public class FullscreenActivity extends Activity implements CvCameraViewListener2 {
    private static final Scalar FACE_RECT_COLOR = new Scalar(0,200,0);
    private double mAbsoluteFaceSize = 100;
    String TAG = "face";
    String folder = Environment.getExternalStorageDirectory().getPath() + "/ocv";
    CameraBridgeViewBase mOpenCvCameraView;
    CascadeClassifier mCascade;
    Mat mRgba,mGray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.face_activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    mOpenCvCameraView.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };


    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_5, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    
    @Override
    public void onCameraViewStarted(int width, int height) {
		mGray = new Mat();
        try {
            // load cascade file from application resources
            InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            File cascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
            FileOutputStream os = new FileOutputStream(cascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            mCascade = new CascadeClassifier(cascadeFile.getAbsolutePath());
            if (mCascade.empty()) {
                Log.e(TAG, "Failed to load cascade classifier");
                mCascade = null;
            } else {
                Log.i(TAG, "Loaded cascade classifier from " + cascadeFile.getAbsolutePath());
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
        }
    }

    @Override
    public void onCameraViewStopped() {    
    }

    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        Imgproc.cvtColor(mRgba, mGray, Imgproc.COLOR_RGBA2GRAY);
        MatOfRect faces = new MatOfRect();
        if (mCascade != null ) {
            mCascade.detectMultiScale(mGray, faces, 1.1, 2, 2, 
                            new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());
            Rect[] facesArray = faces.toArray();
            for (int i = 0; i < facesArray.length; i++) {
                Rect found = facesArray[i];
                found.x += found.width*0.1;
                found.y += found.height*0.1;
                found.width *= 0.8;
                found.height *= 0.8;
                if ( found.x<0 ) found.x=0;
                if ( found.y<0 ) found.y=0;
                if ( found.x+found.width >= mRgba.cols() ) found.x=mRgba.cols()-found.width-1;
                if ( found.y+found.height >= mRgba.rows() ) found.y=mRgba.rows()-found.height-1;
                Core.rectangle(mRgba, found.tl(), found.br(), FACE_RECT_COLOR, 3);
            }
        }
        return mRgba;
    }   
}
