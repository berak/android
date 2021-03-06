package com.gyro;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.DateTimeKeyListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
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

class Ipol {
	float [][] cache;
	int pos;
	
	public Ipol(int n) {
		cache = new float[n][3];
		pos = 0;
	}
	public int set(float[] v) {
		pos += 1;
		pos %= cache.length;
		for (int j=0; j<3; j++) {
			cache[pos][j] = v[j];
		}
		return pos;
	}
	public float[] val() {
		float[] v = {0,0,0};
		for (int i=0; i<cache.length; i++) {
			for (int j=0; j<3; j++) {
				v[j] += cache[i][j];
			}		
		}
		for (int j=0; j<3; j++) {
			v[j] /= (float)(cache.length);
		}		
		return v;
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
	float[] acc = {0,0,0};
	float[] vel = {0,0,0};
	float[] pos = {0,0,0};
	float[] gravity = {0,0,0};
	long ts = 0;
	float yScale = 45.0f;
	int xScale = 14000000; // speed
	Ipol iplacc = new Ipol(6);
	//ActivityRecognition act;
	String tags="";
	String timeStamp = "";
	
	static float eps(float f,float epsilon) {
		return (Math.abs(f) > epsilon ? f : 0.0f);
	}
	
	void reset() {
		acc[0] = vel[0] = pos[0] = gravity[0] = 
		acc[1] = vel[1] = pos[1] = gravity[1] = 
		acc[2] = vel[2] = pos[2] = gravity[2] = 0.0f;
		frames.clear();
		clear(Color.WHITE);
		ts = 0;
        timeStamp = (new Date()).toString();
        hasAlert=false;
	}

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
	    //toast("yelp!");
	}

	void clear(int c) {
	    Paint paint = new Paint();
	    paint.setColor(c);
	    canvas.drawRect(new Rect(0,0,canvas.getWidth(),canvas.getHeight()), paint);
	}
	// circle
	void circle(int x1, int y1, int r, int c) {
	    Paint paint = new Paint();
	    paint.setColor(c);
	    paint.setStrokeWidth(1);
	    canvas.drawCircle(x1,y1,r, paint);
	}
	// rect
	void rect(int x1, int y1, int x2, int y2, int c) {
	    Paint paint = new Paint();
	    paint.setColor(c);
	    paint.setStrokeWidth(1);
	    canvas.drawLine(x1,y1,x2,y2, paint);
	}
	void cross(int x1, int y1, int c) {
	    Paint paint = new Paint();
	    paint.setColor(c);
	    paint.setStrokeWidth(1);
	    int w=7;
	    canvas.drawLine(x1-w,y1-w,x1+w,y1+w, paint);
	    canvas.drawLine(x1-w,y1+w,x1+w,y1-w, paint);
	}

	@Override
	public void onRestart() {
		reset();
	}
	@Override
	public void onResume() {
		reset();
		super.onResume();
	}
	@Override
	public void onPause() {
		if (mSensor != null)
			mSensorManager.unregisterListener(this);
		mSensor = null;
		reset();
		super.onPause();
	}
	void cerr(String t,float[] v) {
		Log.w("Gyro",t + v[0] + " " + v[0] + " " + v[2]);		
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.w("ITEM", " " + item.getOrder() + " " + item.getTitleCondensed());
		if (item.getTitle()=="Save") {
			showAlert();
			return true;
		}
		if ( mSensor != null )
			mSensorManager.unregisterListener(this);
		mSensor = null;

		int id=-1;
		for (int i=0; i<zensor.size(); i++) {
			if (item.getTitle().equals(zensor.get(i).getName())) {
				id = i;
				break;
			}
		}
		mSensor = zensor.get(id);
		if (mSensor == null)
			return false;
		
        boolean reg = mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_UI);
		TextView ed = (TextView)findViewById(R.id.textView1);
		String txt = mSensor.getName() + "\n"; 
		tags += txt;
		ed.setText(tags);
		this.setTitle(mSensor.getName() +  " " + mSensor.getType());

		reset();

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
			tags += s + "\n";
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
		if (arg0.sensor.getType()!=1) {
			simple(arg0);
			return;
	    }

		linear(arg0.values);
		iplacc.set(acc);
		acc = iplacc.val();
		
		//cerr("g ", gravity);
		//cerr("a ", arg0.values);
		//cerr("l ", acc);
		long t = //System.currentTimeMillis();
		arg0.timestamp;
		long dt = t - ts;
		ts = t;
		frames.add(new GyroFrame(t, acc.clone()));

		String z = "";
		if (frames.size()>2 && arg0.sensor.getType()==1) {
			integrate(dt);
			final char [] c = {'x','y','z'};
			for (int i=0; i<3; i++) {
				z += c[i] + "\n";
				z += "" + pos[i] + "\n";
				z += "" + vel[i] + "\n";
				z += "" + acc[i] + "\n";	
			}
			/*z += "g\n";
			for (int i=0; i<3; i++) {
				z += "" + gravity[i] + "\n";
			}*/
		}
		TextView ed = (TextView)findViewById(R.id.textView1);
		ed.setText(tags + z);	
		
		//Log.w("GYRO", "f " + frames.size() + " t " + t + " " + z);
	    int [] colors = {Color.GREEN,Color.BLUE,Color.RED,Color.YELLOW,Color.MAGENTA,Color.CYAN,Color.LTGRAY,Color.DKGRAY};
		int yOff = canvas.getHeight() / 2;
		int x = (int)(t/xScale) % canvas.getWidth();
		if (x >= canvas.getWidth()-20) {
			clear(Color.WHITE);
		}
		for (int i = 0; i<acc.length; i++) {
			int y = yOff + (int)(acc[i] * yScale) ;
			circle(x, y-i*40, 3, colors[i%colors.length]);
			cross(x, yOff-i*40 + (int)(vel[i] * 6*yScale), colors[(i)%colors.length]);
		}
	}
	void simple(SensorEvent arg0) {
		long t = arg0.timestamp;
		String z = "";
		for (int i=0; i<arg0.values.length; i++) {
			z += arg0.values[i] + "\n";
		}
		TextView ed = (TextView)findViewById(R.id.textView1);
		ed.setText(z);	
		
		//Log.w("GYRO", "f " + frames.size() + " t " + t + " " + z);
	    int [] colors = {Color.GREEN,Color.BLUE,Color.RED,Color.YELLOW,Color.MAGENTA,Color.CYAN,Color.LTGRAY,Color.DKGRAY};
		int yOff = canvas.getHeight() / 2;
		int x = (int)(t/xScale) % canvas.getWidth();
		if (x >= canvas.getWidth()-20) {
			clear(Color.WHITE);
		}
		for (int i = 0; i<arg0.values.length; i++) {
			int y = yOff + (int)(arg0.values[i] * yScale) ;
			circle(x, y-i*40, 3, colors[i%colors.length]);
		}
	}

	public void linear(float[] values) {
	    final float alpha = 0.6f;

        for (int i=0; i<3; i++) {
		    // Isolate the force of gravity with the low-pass filter.
		    gravity[i] = alpha * gravity[i] + (1 - alpha) * values[i];
		    // Remove the gravity contribution with the high-pass filter.
		    acc[i] = values[i] - gravity[i];	  
		    acc[i] = eps(acc[i],0.025f);
        }
	}
	void integrate(long deltat) {
    	double dt = (double)(deltat) * 1.0e-9;
    	for (int i=0; i<3; i++) {
    		vel[i] += (float)(acc[i]) * dt;;;
    		vel[i] = eps(vel[i],0.008f);
    		pos[i] += (vel[i]) * dt;
    		vel[i] *= 0.88f;
    	}
		//Log.w("Gyro","dt " + dt + " " + pos[0] + " " + vel[0]);
	}

	boolean hasAlert=false;
    void showAlert() {
    	Log.w("ALERT", "" + hasAlert);
    	if (hasAlert) return;
    	hasAlert = true;
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Your Name Here");
        final EditText input = new EditText(this);
        alert.setView(input);    
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {        
            public void onClick(DialogInterface dialog, int whichButton) {
                String name = input.getText().toString();
                if ( (name != null) && (! name.equals("")) && (! name.equals(" ")) ) {
                    if ( name.endsWith("\n") )
                    	name = name.replace("\n", "");
                	Log.w("ALERT", "start " + name);
        			new Postit(name).start();
                	Log.w("ALERT", "finish " + name);
        	        //toast(ds);
        			hasAlert = false;
                }
            }
        });       
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
    			hasAlert = false;
            }
        });
        alert.show();
    }

    void toast(String s) {
        new AlertDialog.Builder(this)
               .setTitle(s)
               //.setView(input);    
               .setPositiveButton("Ok",null)        
               .show();    	
    }

    private class Postit extends Thread {
    	String token;
    	public Postit(String name) {
    		token=name;
    	}
        public void run() {
        	if (token.equals("")) return;
    		Log.w("GYRO", "got " + frames.size() + " frames");
        	if (frames.size() < 100)
        		return;
        	StringBuilder sb = new StringBuilder(timeStamp);
        	sb.append("\n");
        	sb.append(mSensor.getName());
        	sb.append("\n");
    	    for (int i=0; i<frames.size(); i++) {
    	    	GyroFrame f = frames.get(i);
    	    	sb.append(f.timestamp);
    	    	sb.append(",");
    	    	for (int j=0; j<f.values.length; j++) {
    	    		sb.append(f.values[j]);
    	    		sb.append(",");
    	    	}
    	    	sb.append("\n");
    	    }
    	    String strdata = sb.toString();
    	    try {
        		Log.w("GYRO", "starting post");
        	    HttpClient httpclient = new DefaultHttpClient();
        	    HttpPost httppost = new HttpPost("http://rowrow-data.appspot.com/sensor");
    	        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
    	        nameValuePairs.add(new BasicNameValuePair("who", token));
    	        nameValuePairs.add(new BasicNameValuePair("data", strdata));
    	        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
        		Log.w("GYRO", "sending post");
    	        httpclient.execute(httppost);
        		Log.w("GYRO", "finish post");
    			GyroActivity.super.finish();
    	    } catch (Exception e) {
    	    	Log.e("Gyro", e.toString());
    	    }
        }
    }
} // end of GyroMic class
