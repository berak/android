package com.gyro;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.DateTimeKeyListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.DatePicker;
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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
	private SensorManager mSensorManager;
	private Sensor mSensor;
	List<Sensor>   zensor;
	List<MenuItem> mItems;
	List<GyroFrame> frames;
	
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
	public void onPause() {
		if (mSensor != null)
			mSensorManager.unregisterListener(this);
		mSensor = null;
			
		super.onPause();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getTitle()=="Save") {
			new Postit().start();
			return true;
		}
		if ( mSensor != null )
			mSensorManager.unregisterListener(this);
		mSensor = null;

		int id = item.getGroupId();
		mSensor = zensor.get(id);
		if (mSensor == null)
			return false;
		
        boolean reg = mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_UI);
		TextView ed = (TextView)findViewById(R.id.textView1);
		String txt = mSensor.getName() + " " + reg; 
		ed.setText(txt);

		frames.clear();
		clear(Color.WHITE);

		return super.onOptionsItemSelected(item);
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		mItems = new ArrayList<MenuItem>();
		zensor = mSensorManager.getSensorList(Sensor.TYPE_ALL);
		for ( int i=0; i<zensor.size(); i++ )
		{
			Sensor s = zensor.get(i);
			mItems.add( menu.add(s.getName()) );
		}
		mItems.add(menu.add("Save"));
		return true;
	}


	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
	}



	@Override
	public void onSensorChanged(SensorEvent arg0) {
		this.setTitle(arg0.sensor.getName() +  " " + arg0.sensor.getType());
		String z = "";  
		for ( int i = 0; i<arg0.values.length; i++ )
			z += arg0.values[i] + "\n";
		TextView ed = (TextView)findViewById(R.id.textView1);
		ed.setText(z);	

		long t = //System.currentTimeMillis();
		arg0.timestamp;
		frames.add(new GyroFrame(t, arg0.values));
		//Log.w("GYRO", "f " + frames.size() + " t " + t + " " + z);
	    int [] colors = {Color.GREEN,Color.BLUE,Color.RED,Color.YELLOW,Color.MAGENTA,Color.CYAN,Color.LTGRAY,Color.DKGRAY};
		float yScale = 15.0f;
		int xScale = 20000000; // speed
		int yOff = canvas.getHeight() / 2;
		int x = (int)(t/xScale) % canvas.getWidth();
		if (x >= canvas.getWidth()-5) {
			clear(Color.WHITE);
		}
		for ( int i = 0; i<arg0.values.length; i++ ) {
			int y = yOff + (int)(arg0.values[i] * yScale) ;
			draw(x, y, 5, colors[i%colors.length]);
		}
	}

    private class Postit extends Thread {
        public void run() {
    		Log.w("GYRO", "f1 " + frames.size());
        	if (frames.size() < 100)
        		return;
    	    String strdata =  "{" + mSensor.getName() + ":[";
    	    for (int i=0; i<frames.size(); i++) {
    	    	GyroFrame f = frames.get(i);
    	    	strdata += "{" + f.timestamp + ":";
    	    	for (int j=0; j<f.values.length; j++) {
    	    		strdata += f.values[j] + ",";
    	    	}
    	    	strdata += "},";
    	    }
    	    strdata += "]}";
    	    frames.clear();
    	    try {
        	    HttpClient httpclient = new DefaultHttpClient();
        	    HttpPost httppost = new HttpPost("http://hook.io/berak/chili");
    	        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
    	        nameValuePairs.add(new BasicNameValuePair("who", "accel_" + (new Date())));
    	        nameValuePairs.add(new BasicNameValuePair("set", strdata));
    	        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
    	        httpclient.execute(httppost);
    	    } catch (Exception e) {
    	    	Log.e("Gyro", e.toString());
    	    }
        }
    }
} // end of GyroMic class
