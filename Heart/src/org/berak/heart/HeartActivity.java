package org.berak.heart;



import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;

class Ring {
    int SIZ=0;
    int SIZ_F=0;
    int cur = -1;
    Mat m1,m2,mt;

    Ring(int n1,int n2) {
    	SIZ = n1;
    	SIZ_F = 2*Core.getOptimalDFTSize((n2+1)/2);
        mt = Mat.zeros(1,SIZ,CvType.CV_64F);
        m1 = Mat.zeros(1,SIZ,CvType.CV_64F);
        m2 = Mat.zeros(1,SIZ_F,CvType.CV_64F);
    }
     
    public int push(double t, double m) {
        cur ++;
        cur %= SIZ;
        m1.put(0, cur, m);
        mt.put(0, cur, t);
        return cur;
    }    

    public int k=0;
    public Mat interpolated(double timeStep) { 
    	k  = 0;
		int j  = (cur)%SIZ;
		int j2 = (j+1)%SIZ;
		double t  = mt.get(0,j)[0] * 0.001;
		double t1 = mt.get(0,j)[0] * 0.001;
		double t2 = mt.get(0,j2)[0] * 0.001;
		double dt = (t2-t1) + 0.000001;
		double v1 = m1.get(0,j)[0];
		double v2 = m1.get(0,j2)[0];
    	for ( int i=0; i<SIZ_F; i++ ) {
    		double v  = v1 + (t-t1)*(v2-v1) / dt; // lerp
    		m2.put(0, SIZ_F-1-i, v);              // reversed time

    		t += timeStep;
    		if ( t >= t2 ) {
    			j  += 1;
    			j  %= SIZ;
    			j2 += 1;
    			j2 %= SIZ;
    			t = t2;
        		t1 = mt.get(0,j)[0] * 0.001;
        		t2 = mt.get(0,j2)[0] * 0.001;
        		dt = (t2-t1) + 0.000001;
        		v1 = m1.get(0,j)[0];
        		v2 = m1.get(0,j2)[0];
    			k ++;
    		}
    	}
        return m2;
    }    
}


class Processor {
	static final int rectsize=80;
	static final int roi_size=64;
	Rect region = new Rect(440,100,rectsize,rectsize);
//    Rect region_2 = new Rect(0,0,rectsize,rectsize);
    Ring ring = new Ring(128,400);
    Mat  peak = new Mat(1,64,CvType.CV_64F);
    double mv = 0;
    long t0 = 0;
    int mp = 0;
    static double [] ham_tab = new double[roi_size];
    Processor() {
	    for (int k=0; k<roi_size; k++)
	        ham_tab[k] = hamming(k,roi_size);
    }
    
    static double hamming(double n,int N) { 
	    return 0.54-0.46*Math.cos((2.0*Math.PI*n)/(N-1)); 
	}

    static void hamming(Mat m) {
	    for (int k=0; k<m.cols(); k++)
	        m.put(0, k, m.get(0, k)[0] * ham_tab[k]);
	}
    static void hamming_inv(Mat m) {
	    for (int k=0; k<m.cols(); k++)
	        m.put(0, k, m.get(0, k)[0] / ham_tab[k]);
	}
	static double ipol(double a , double b, double u, int n) {
	   return  (n-u) * a / n + u * b / n;
	}
	static double bin2bpm(int b, double timeStep ) {
	    return 0.5 * ( 60.0 * b ) / ( timeStep );
	}

    
    void draw(Mat img, Mat curve,int offx,int offy, double scalex, double scaley, Scalar col) {
        Point pt1 = new Point();
        Point pt2 = new Point();
        double m = Core.mean(curve).val[0];
        try {
        	pt1.x = offx + (0) *scalex;
        	pt1.y = offy + m - curve.get(0, 0)[0]*scaley;
	        for ( int i=1; i<curve.cols(); i++) {
	        	pt2.x = offx + (i)   *scalex;
	        	pt2.y = offy + m - curve.get(0, i)[0]*scaley;
	        	Core.line(img,pt1,pt2,col,2);
	        	pt1.x = pt2.x;
	        	pt1.y = pt2.y;
	        }
        }
        catch(Exception e) {
        	Log.e("DRAW",e.toString());
        }
    }
    
    void process ( Mat img ) {
        final Scalar col  = new Scalar(150,170,0);
        final Scalar col2 = new Scalar(0,170,150);
        final Scalar col3 = new Scalar(200,0,0);        
    	long t1 = System.currentTimeMillis();
    	long dt = t1 - t0;
    	t0 = t1;
        long P0 = System.nanoTime();
    	try {
    		Mat s1 = img.submat(region);
	    	//Mat s2 = img.submat(region_2);
	    	
	    	Scalar mean1 = Core.mean(s1);
	    	//Scalar mean2 = Core.mean(s2);    	
	        double red = (double)(mean1.val[1]);//-mean2.val[1]); // green diff
	        ring.push(t1,red);

	        double ts = 0.001*(double)dt;
	        double timeStep = ts * (double)ring.SIZ / (double)ring.SIZ_F;
	        Mat curve = ring.interpolated(timeStep);
	       // hamming(curve);
	        
	        long P1 = System.nanoTime();
	        Mat idct = new Mat();
	        Core.dct(curve, idct);

	        int cutstart = 11;
	        int cutend = cutstart + peak.cols();
	        Mat roi = idct.submat(0,1,cutstart,cutend);
	        hamming(roi);
	        long P2 = System.nanoTime();
	        
	        double m = -9999.0;
	        int mi = 0;
	        for (int i=0;i<peak.cols(); i++) {
	        	double v = Math.abs(roi.get(0,i)[0]);
	        	if (v>m) {
	        		m  = v;
		        	mi = cutstart + i;
	        	}
	        	peak.put(0, i, v);
	        }
	        mp = (int)ipol(mp,mi,1,10);
	        long P3 = System.nanoTime();

	        Mat cardiac = new Mat();
	        Core.dct(roi,cardiac,Core.DCT_INVERSE);
	        
	        long P4 = System.nanoTime();
	        int xoff = 10;
	        double bpm = bin2bpm(mp, timeStep * idct.cols());
	        String bpmstr = "unstable";
	        if ( m < 10 ) {
	        	bpmstr = "" + (int)bpm + "." + (int)((bpm-(int)bpm)*10) +" bpm.";
	        }
        	Core.putText(img, bpmstr, new Point(40,40), Core.FONT_HERSHEY_PLAIN, 2.3, col3, 3);
	        draw(img, cardiac, xoff, 280, 4,  30, col3);
        	//draw(img, curve,   xoff, 330, 0.8, 6, col);
	        draw(img, peak,    xoff, 360, 4,  30, col2);
	    	Core.rectangle(img,region.tl(),region.br(),col,2);
	        Core.line(img,region.tl(),new Point(region.tl().x,region.tl().y+(int)(mean1.val[0]/8)),col2,3);
	        long P5 = System.nanoTime();
	    	Log.i("DRAW",""+ts + " " + mp + " " + ring.k + " " + ring.cur + " " + timeStep);
	    	Log.i("PROF",""+(P1-P0) + " " + (P2-P1) + " " + (P3-P2) + " " + (P4-P3) + " " + (P5-P4));
	    } catch(Exception e) {
	    	Log.e("DRAW","",e);
	    }
    }
}

public class HeartActivity extends Activity implements OnTouchListener, CvCameraViewListener2 {
    private static final String  TAG = "Heart";
    private CameraBridgeViewBase mOpenCvCameraView;

    Mat mRgba;
    Processor proc;
    
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

    public HeartActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.image_manipulations_surface_view);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.image_manipulations_activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setOnTouchListener(this);
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

   @Override
   public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        return true;
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat();
        proc = new Processor();
    }


    public void onCameraViewStopped() {
        if (mRgba != null)
            mRgba.release();
        mRgba = null;
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();       
        proc.process(mRgba);
    	return mRgba;
    }

    public boolean onTouch(View v, MotionEvent event) {
        int cols = mRgba.cols();
        int rows = mRgba.rows();

        int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
        int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;

        int x = (int)event.getX() - xOffset;
        int y = (int)event.getY() - yOffset;
        if (x<0||y<0||x>=cols||y>=cols) return false;
        proc.region.x = x;
        proc.region.y = y;
        return true;
    }

}
