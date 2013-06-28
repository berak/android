package com.berak.face;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;

public class Reco {

	public class Record {
		public Mat img;
		public String name;
		public Record() {}
		public Record(Mat i, String n) {
			img=i; name=n;
		}
	}
	List<Record> rec = new ArrayList<Record>();

	
	public void addRec(Mat m, String n, String dir) {
		rec.add(new Record(m,n));
		File node = new File(dir);
		File subDir = new File(node, n);
		subDir.mkdirs();
		String fname = subDir.getAbsolutePath() + "/" + ((int)(Math.random()*100000)) + ".png";
		Highgui.imwrite(fname,m);
	}

	//
    // in lieu of the real thing ..
	//
	public double predict(Mat m, Record best) {
		double dist = 999999999.;
		double distMax = -999999999.;
		for ( int i=0; i<rec.size(); i++) {
			Record r = rec.get(i);
			double d = Core.norm(r.img,m,Core.NORM_L2);
			if (d<dist) {
				best.name = r.name;
				best.img  = r.img;
				dist = d;
			}
			if (d>distMax) {
				distMax = d;
			}
		}
		return 1.0 - dist/distMax;
	}
	
	public void load(String dir) {
		File node = new File(dir);
		String[] subNote = node.list();
		if ( subNote==null ) return;
		for(String person : subNote) {
			File subDir = new File(node, person);		
			File[] pics = subDir.listFiles();
			for(File f : pics) {
				Mat m = Highgui.imread(f.getAbsolutePath(),0);
				rec.add(new Record(m,person));
			}
		}
	}	
}
