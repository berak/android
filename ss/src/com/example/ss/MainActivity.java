package com.example.ss;

import java.util.ArrayList;
import java.util.List;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

public class MainActivity extends Activity implements SensorEventListener {

	private SensorManager mSensorManager;
	private Sensor mSensor;
	List<Sensor>   zensor;
	List<MenuItem> mItems;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
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
		EditText ed = (EditText)findViewById(R.id.editText1);
		String txt = mSensor.getName() + " " + reg; 
		ed.setText(txt);

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
		EditText ed = (EditText)findViewById(R.id.editText1);
		ed.setText(z);
		// TODO Auto-generated method stub
		
	}

}
