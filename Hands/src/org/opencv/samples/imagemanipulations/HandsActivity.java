package org.opencv.samples.imagemanipulations;
import org.opencv.core.Scalar;

import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnTouchListener;

public class HandsActivity extends Activity implements OnTouchListener, CvCameraViewListener2 {
    private static final String  TAG                 = "OCVSample::Activity";
    private Gesture  			 mDetector;

    private MenuItem             mItem1;
    private MenuItem             mItem2;
    private MenuItem             mItem3;
    private MenuItem             mItem4;
    private MenuItem             mItem5;
    private MenuItem             mItemLo;
    private MenuItem             mItemHi;
    private CameraBridgeViewBase mOpenCvCameraView;
    boolean slUpper = false, slLower=false; 
    Point[] slp = new Point[3];

    int prog = 0;
    private Mat                  mRgba;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public HandsActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.image_manipulations_surface_view);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.image_manipulations_activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setOnTouchListener(this);
        slp[0] = new Point(0,200);
        slp[1] = new Point(0,250);
        slp[2] = new Point(0,300);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_5, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        mItem1  = menu.add("g 1");
        mItem2  = menu.add("g 2");
        mItem3  = menu.add("g 3");
        mItem4  = menu.add("g 4");
        mItem5  = menu.add("g 5");
        mItemLo = menu.add("hsv low range");
        mItemHi = menu.add("hsv hi  range");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        if (item == mItemHi)
        {
        	if (slUpper) slUpper = false;
        	else slUpper = true;
        	slLower = false;
        }
        if (item == mItemLo)
        {
        	if (slLower) slLower = false;
        	else slLower = true;
        	slUpper = false;
        }
        if (item == mItem1)    	mDetector.setRec(0);
        if (item == mItem2)    	mDetector.setRec(1);
        if (item == mItem3)    	mDetector.setRec(2);
        if (item == mItem4)    	mDetector.setRec(3);
        if (item == mItem5)    	mDetector.setRec(4);
        return true;
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat();
        mDetector = new Gesture();
    }


    public void onCameraViewStopped() {
        if (mRgba != null)
            mRgba.release();
        mRgba = null;
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        mDetector.process(mRgba);
        List<MatOfPoint> contours = mDetector.getContours();
        if ( contours.size() > 0 )
        {
          	Imgproc.drawContours( mRgba, contours, -1, new Scalar(200,0,0));
        }
        String loggr = "" + (mDetector.found_id+1) + " : " + mDetector.found_dist;
    	Core.putText(mRgba, loggr, new Point(60,60), Core.FONT_HERSHEY_PLAIN, 3.1, new Scalar(60,180,60),3);
    	
    	if ( slUpper )
    	{
    		Core.line(mRgba, slp[0], new Point( mDetector.mUpperBound.val[0] ,slp[0].y), new Scalar(0,200,0),8);
    		Core.line(mRgba, slp[1], new Point( mDetector.mUpperBound.val[1] ,slp[1].y), new Scalar(0,200,0),8);
    		Core.line(mRgba, slp[2], new Point( mDetector.mUpperBound.val[2] ,slp[2].y), new Scalar(0,200,0),8);
    	}
    	if ( slLower )
    	{
    		Core.line(mRgba, slp[0], new Point( mDetector.mLowerBound.val[0] ,slp[0].y), new Scalar(0,0,200),8);
    		Core.line(mRgba, slp[1], new Point( mDetector.mLowerBound.val[1] ,slp[1].y), new Scalar(0,0,200),8);
    		Core.line(mRgba, slp[2], new Point( mDetector.mLowerBound.val[2] ,slp[2].y), new Scalar(0,0,200),8);
    	}
    	return mRgba;
    }

    public boolean onTouch(View v, MotionEvent event) {
        int cols = mRgba.cols();
        int rows = mRgba.rows();

        int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
        int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;

        int x = (int)event.getX() - xOffset;
        int y = (int)event.getY() - yOffset;

        Log.i(TAG, "Touch image coordinates: (" + x + ", " + y + ")");
        if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;

        for ( int i=0; i<3; i++)
        {
        	if ( x < 256 )
	            if ( Math.abs(y -slp[i].y) < 15 )
	            	if ( slUpper && Math.abs(x - mDetector.mUpperBound.val[i]) < 15 )
	            	{
	            		mDetector.mUpperBound.val[i] = x;
		        		break;
	            	}
        			if ( slLower && Math.abs(x - mDetector.mLowerBound.val[i]) < 15 )
        			{
		        		mDetector.mLowerBound.val[i] = x;
		        		break;
        			}
        }
        return true;
    }

}
