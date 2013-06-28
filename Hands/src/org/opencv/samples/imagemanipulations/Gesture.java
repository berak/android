package org.opencv.samples.imagemanipulations;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.KalmanFilter;

public class Gesture {
    // Lower and Upper bounds for range checking in HSV color space
    public Scalar mLowerBound = new Scalar(0, 90, 60);
    public Scalar mUpperBound = new Scalar(35,255,255);
//    private static double mMinContourSize = 40;
    private List<MatOfPoint> mContours = new ArrayList<MatOfPoint>();
    MatOfPoint[] recs = new MatOfPoint[5];
    int sel_id = -1;
    int found_id = -1;
    double found_dist = -1;
    // Cache
    Mat mPyrDownMat = new Mat();
    Mat mHsvMat = new Mat();
    Mat mMask = new Mat();
    Mat mMaskRgba = new Mat();
    Mat mDilatedMask = new Mat();
    Mat mHierarchy = new Mat();
    KalmanFilter kalman = new KalmanFilter(1,1);
	
	// hausdorff distance calculation
	//
    double distance_2( Point[] a, Point[] b )
	{
	 	double maxDistAB = 0;
	 	for (int i=0; i<a.length; i++)
	 	{
	 		double minB = 1000000;
	 		for (int j=0; j<b.length; j++)
	 		{
	 			double dx = (a[i].x - b[j].x);		
	 			double dy = (a[i].y - b[j].y);		
	 			double tmpDist = dx*dx + dy*dy;
	
	 			if (tmpDist < minB)
	 			{
	 				minB = tmpDist;
	 			}
	 			if ( tmpDist == 0 )
	 			{
	 				break; // can't get better than equal.
	 			}
	 		}
	 		maxDistAB += minB;
	 	}
	 	return Math.sqrt(maxDistAB);
	}
	
    double distance_hausdorff( MatOfPoint a, MatOfPoint b )
	{
		Point[] pa = a.toArray();
		Point[] pb = b.toArray();
		double maxDistAB = distance_2( pa, pb );
		double maxDistBA = distance_2( pb, pa );	
	 	return Math.max(maxDistAB,maxDistBA);
	}
	


    public void process(Mat rgbaImage) {
		found_id = -1;
		found_dist = -1;
        mContours.clear();
        Imgproc.pyrDown(rgbaImage, mPyrDownMat);
        Imgproc.pyrDown(mPyrDownMat, mPyrDownMat);
        Imgproc.blur(mPyrDownMat, mPyrDownMat, new Size(6,6));
        Imgproc.cvtColor(mPyrDownMat, mHsvMat, Imgproc.COLOR_RGB2HSV_FULL);
        Core.inRange(mHsvMat, mLowerBound, mUpperBound, mMask);
        Imgproc.dilate(mMask, mMask, new Mat());

//        Mat mr = rgbaImage.submat( 0, rgbaImage.rows()/2,  rgbaImage.cols()/2, rgbaImage.cols() );
        Mat mr = rgbaImage.submat( 0, rgbaImage.rows()/4,  3*rgbaImage.cols()/4, rgbaImage.cols() );
        Imgproc.cvtColor(mMask, mMaskRgba, Imgproc.COLOR_GRAY2RGBA);
        mMaskRgba.copyTo(mr);

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(mMask, contours, mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        long mt=-1;
        MatOfPoint best=null;
        for ( int i=0; i< contours.size(); i++)
        {
        	MatOfPoint m = contours.get(i);
        	if ( m.total() > mt )
        	{
        		mt = m.total();
        		best = m;
        	}
        }
        //Core.multiply(m, new Scalar(4,4),m);
        if ( best == null )
        	return;
        if ( sel_id != -1 )
        {
    		recs[sel_id] = best;
        	sel_id = -1;
        }
    	mContours.add(best);
    	double md = 100000000;
    	for ( int i=0; i<5; i++)
    	{
    		if ( recs[i] == null)
    			continue;
    		double d = Imgproc.matchShapes(best.clone(), recs[i].clone(), Imgproc.CV_CONTOURS_MATCH_I2,0);
//    		double d = distance_hausdorff(best, recs[i]);
    		if ( d < md)
    		{
    			md = d;
    			found_id = i;
    			found_dist = d;
   			}        		
    	}
    }

    public void setRec(int i)
    {
    	sel_id = i;
    }

    public List<MatOfPoint> getContours() {
        return mContours;
    }
}
