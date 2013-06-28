package com.example.aa;


import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.Prediction;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MotionEvent;
import android.widget.TextView;

public class MainActivity<GesturesAdapter> extends Activity implements android.gesture.GestureOverlayView.OnGestureListener {
	private final File mStoreFile = new File(Environment.getExternalStorageDirectory(), "gestures");
    private static GestureLibrary sStore;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
        GestureOverlayView overlay = (GestureOverlayView) findViewById(R.id.gestures_overlay);
        if (sStore == null) {
            sStore = GestureLibraries.fromFile(mStoreFile);
        }
        if (sStore.load()) {
        	String s="";
            for (String name : sStore.getGestureEntries()) {
            	s += name + "\n";
            }
    		TextView tx = (TextView) findViewById(R.id.editText1);
    		tx.setText(s);
        }

        overlay.addOnGestureListener(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onGesture(GestureOverlayView overlay, MotionEvent arg1) {
	}
	@Override
	public void onGestureCancelled(GestureOverlayView arg0, MotionEvent arg1) {		
	}
	@Override
	public void onGestureEnded(GestureOverlayView overlay, MotionEvent arg1) {
		String s = "";
		ArrayList<Prediction>  pred = sStore.recognize(overlay.getGesture());
		for (int i=0; i<pred.size(); i++) {
			Prediction p = pred.get(i);
			s += p.name + " " + p.score + "\n"; 
		}
		TextView tx = (TextView) findViewById(R.id.editText1);
		tx.setText(s);
	}
	@Override
	public void onGestureStarted(GestureOverlayView overlay, MotionEvent arg1) {
	}
}
