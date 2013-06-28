package com.example.speak;

import android.os.Bundle;
import android.app.Activity;
import android.speech.tts.TextToSpeech;
import android.view.Menu;
import android.widget.EditText;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {
	TextToSpeech tts;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		tts = new TextToSpeech(this.getApplicationContext(), this);
	}

	public void onSpeak() {
		EditText ed = (EditText)findViewById(R.id.editText1);
		String text = ed.toString();
		tts.speak(text, TextToSpeech.QUEUE_ADD, null);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	@Override
	public void onInit(int status) {
		int z = status;
		
	}

}
