package com.berak.mjpg;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.highgui.Highgui;

import com.berak.mjpg.R;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.Menu;
import android.widget.ImageView;

public class MainActivity extends Activity {

	MjpgTask task;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onPause() {
		super.onPause();
		task.doRun = false;
		task.cancel(true);
	}

	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS:
				break;
			default:
				super.onManagerConnected(status);
				break;
			}
		}
	};

	@Override
	public void onResume() {
		super.onResume();

		boolean r = OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_5,this, mLoaderCallback);
		Log.e("MJPG", "opencv onboard : " + r);

		task = new MjpgTask();
		task.execute("http://82.135.240.133/mjpg/video.mjpg?resolution=352x288");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		task.doRun = false;
		task.cancel(true);
	}

	private class MjpgTask extends AsyncTask<String, Integer, Long> {
		byte[] arr = new byte[100000];
		boolean doRun = false;

		protected Long doInBackground(String... urls) {
			long totalSize = 0;
			try {
				URL url = new URL(urls[0]);
				HttpURLConnection con = (HttpURLConnection) url.openConnection();
				InputStream in = con.getInputStream();
				doRun = in.available() > 0;
				while (doRun) {
					int i = 0;
					// check for jpeg soi sequence: ff d8
					for (; i < 1000; i++) {
						int b = in.read();
						if (b == 0xff) {
							int b2 = in.read();
							if (b2 == 0xd8)
								break;
						}
					}
					if (i > 999) {
						Log.e("MJPG", "bad head!");
						continue;
					}
					arr[0] = (byte) 0xff;
					arr[1] = (byte) 0xd8;
					i = 2;
					// check for jpeg eoi sequence: ff d9
					for (; i < 100000; i++) {
						int b = in.read();
						arr[i] = (byte) b;
						if (b == 0xff) {
							i++;
							int b2 = in.read();
							arr[i] = (byte) b2;
							if (b2 == 0xd9)
								break;
						}
					}
					i++; // total bytecount
					publishProgress(i);
				}
			} catch (Exception e) {
				Log.e("MJPG", e.toString());
			}
			return totalSize;
		}

		// This is called each time you call publishProgress()
		protected void onProgressUpdate(Integer... progress) {
			int nBytes = progress[0];
			Log.e("MJPG", "got an image, " + nBytes + " bytes!");
			// Bitmap bm = BitmapFactory.decodeByteArray(arr, 0, nBytes);
			Mat ocvImg = Highgui.imdecode(new MatOfByte(arr),Highgui.IMREAD_UNCHANGED);

			// .. your processing here ..

			// convert back:
			Bitmap bm = Bitmap.createBitmap(ocvImg.cols(), ocvImg.rows(),Bitmap.Config.ARGB_8888);
			Utils.matToBitmap(ocvImg, bm);
			if (bm != null && bm.getHeight() > 0) {
				ImageView v = (ImageView) findViewById(R.id.imageView1);
				v.setImageBitmap(bm);
			}
		}

		// This is called when doInBackground() is finished
		protected void onPostExecute(Long result) {
		}
	}
}
