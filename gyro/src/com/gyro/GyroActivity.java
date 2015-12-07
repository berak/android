package com.gyro;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
// import android.location.LocationManager;
import android.location.LocationListener;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

class GyroFrame {
	public long timestamp;
	float[] values;

	GyroFrame(long t, float[] v) {
		timestamp = t;
		values = v;
	}
}

public class GyroActivity extends Activity  implements SensorEventListener{
    ImageView drawingImageView;
    Canvas canvas;
	final private String TAG = "Gyro";
	private SensorManager mSensorManager;
	private Sensor mSensor;
	List<Sensor>   zensor;
	List<MenuItem> mItems;
	List<GyroFrame> frames;
	boolean curve = true;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_gyro);
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        
	    drawingImageView = (ImageView) this.findViewById(R.id.imageView1);
	    Bitmap bitmap = Bitmap.createBitmap((int) getWindowManager()
	        .getDefaultDisplay().getWidth(), (int) getWindowManager()
	        .getDefaultDisplay().getHeight(), Bitmap.Config.ARGB_8888);
	    canvas = new Canvas(bitmap);
	    drawingImageView.setImageBitmap(bitmap);

	    frames = new ArrayList<GyroFrame>();
	}

	// circle
	void draw(int x1, int y1, int r, int c) {
	    Paint paint = new Paint();
	    paint.setColor(c);
	    paint.setStrokeWidth(1);
	    canvas.drawCircle(x1,y1,r, paint);
	}
	// rect
	void draw(int x1, int y1, int x2, int y2, int c) {
	    Paint paint = new Paint();
	    paint.setColor(c);
	    paint.setStrokeWidth(2);
	    canvas.drawLine(x1,y1,x2,y2, paint);
	}
	void clear(int c) {
	    Paint paint = new Paint();
	    paint.setColor(c);
	    canvas.drawRect(new Rect(0,0,canvas.getWidth(),canvas.getHeight()), paint);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if ( mSensor != null )
			mSensorManager.unregisterListener(this);
		mSensor = null;

		int id = item.getGroupId();
		mSensor = zensor.get(id);
		if ( mSensor == null )
			return false;
		
        boolean reg = mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_UI);
		TextView ed = (TextView)findViewById(R.id.textView1);
		String txt = mSensor.getName() + " " + reg; 
		ed.setText(txt);

		clear(Color.WHITE);

		return super.onOptionsItemSelected(item);
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.main, menu);
		mItems = new ArrayList<MenuItem>();
		zensor = mSensorManager.getSensorList(Sensor.TYPE_ALL);
		for ( int i=0; i<zensor.size(); i++ )
		{
			Sensor s = zensor.get(i);
			mItems.add( menu.add(s.getName()) );
		}
		return true;
	}


	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void onSensorChanged(SensorEvent arg0) {
		this.setTitle(arg0.sensor.getName() +  " " + arg0.sensor.getType());
		String z = "";  
		for ( int i = 0; i<arg0.values.length; i++ )
			z += arg0.values[i] + "\n";
		TextView ed = (TextView)findViewById(R.id.textView1);
		ed.setText(z);	

		long t = System.currentTimeMillis();
		frames.add(new GyroFrame(t, arg0.values));

		Log.w("GYRO", "f " + frames.size() + " t " + t + " " + z);
	    int [] colors = {Color.GREEN,Color.BLUE,Color.RED,Color.YELLOW,Color.MAGENTA,Color.CYAN,Color.LTGRAY,Color.DKGRAY};
		float yScale = 15.0f;
		int xScale = 98; // speed
		int yOff = canvas.getHeight() / 2;
		int x = (int)(t/xScale) % canvas.getWidth();
		if (x >=0 || x >= canvas.getWidth()-2) {
			clear(Color.WHITE);
		}
		for ( int i = 0; i<arg0.values.length; i++ ) {
			int y = yOff + (int)(arg0.values[i] * yScale) ;
			draw(x, y, 5, colors[i%colors.length]);
		}
	}

} // end of GyroMic class
