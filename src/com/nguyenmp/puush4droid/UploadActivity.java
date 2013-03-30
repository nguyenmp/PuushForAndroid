package com.nguyenmp.puush4droid;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;
import com.actionbarsherlock.app.SherlockActivity;
import com.nguyenmp.puushforjava.PuushClient;
import org.apache.http.client.CookieStore;

import java.io.*;
import java.util.List;
import java.util.Random;


public class UploadActivity extends SherlockActivity {
	
	@Override
	public void onCreate(Bundle inState) {
		super.onCreate(inState);
		
		Intent intent = getIntent();
		String action = intent.getAction();

		//If the intent has one image
		if (action.equals(Intent.ACTION_SEND)) {
			Uri image = intent.getParcelableExtra(Intent.EXTRA_STREAM);
			Toast.makeText(this, image.toString(), Toast.LENGTH_SHORT).show();
			
			File[] files = new File[1];
			try {
				files[0] = getFileFromStream(getContentResolver().openInputStream(image));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			CookieStore cookieStore = LoginActivity.getCookies(this);
			new UploadTask(this, new Random().nextInt()).execute(new UploadTask.Input(getApplicationContext(), files, cookieStore));
		} 
		
		//If the intent has multiple images
		else if (action.equals(Intent.ACTION_SEND_MULTIPLE)) {
			List<Uri> images = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
			if (images != null) {
				File[] files = new File[images.size()];
				for (Uri image : images) {
					try {
						File file = getFileFromStream(getContentResolver().openInputStream(image));
						files[images.indexOf(image)] = file;
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				
				CookieStore cookieStore = LoginActivity.getCookies(this);
				new UploadTask(this, new Random().nextInt()).execute(new UploadTask.Input(getApplicationContext(), files, cookieStore));
			}
		}
	}
	
	private File getFileFromStream(InputStream inputStream) throws IOException {
		//Temporary output file
		File tempFile = File.createTempFile("tempFile", null);
		FileOutputStream tempOutputStream = new FileOutputStream(tempFile);
		
		//Read input stream
		byte[] buffer = new byte[1024];
		int bytesRead;
		while ((bytesRead = inputStream.read(buffer, 0, buffer.length)) != -1) {
			//Write input stream content to file
			tempOutputStream.write(buffer, 0, bytesRead);
			tempOutputStream.flush();
		}
		
		//Release resources
		inputStream.close();
		tempOutputStream.close();
		
		//Return generated file
		return tempFile;
	}

	private static class UploadTask extends AsyncTask<UploadTask.Input, UploadTask.Progress, Void> {
		private final NotificationManager notificationManager;
		private final Context context;
		private final int notifID;

		private UploadTask(Context context, int notifID) {
			this.context = context;
			this.notifID = notifID;
			this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		}

		private UploadTask(Context context) {
			this(context, new Random().nextInt());
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			
			NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
			builder.setSmallIcon(R.drawable.ic_launcher);
			builder.setContentTitle("Uploading...");
			builder.setContentText("Please wait...");
			builder.setOngoing(true);
			Notification notif = builder.build();
			notificationManager.notify(notifID, notif);
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			super.onPostExecute(aVoid);
			
			notificationManager.cancel(notifID);
		}

		@Override
		protected Void doInBackground(Input... params) {
			for (Input input : params) {
				try {
					String apiKey = PuushClient.getSettings(input.cookieStore).getAPIKey();
					for (File file : input.files) {
						try {
							String result = PuushClient.upload(file, apiKey);
							Context context = input.context;
							publishProgress(new Progress(result, file, context));
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					NotificationManager notificationManager = (NotificationManager) input.context.getSystemService(Context.NOTIFICATION_SERVICE);
					NotificationCompat.Builder builder = new NotificationCompat.Builder(input.context);
					builder.setContentTitle("Error");
					builder.setContentText("Login required");
					builder.setSmallIcon(R.drawable.ic_launcher);
					builder.setOngoing(false);
					builder.setAutoCancel(true);
					Notification notification = builder.build();
					notificationManager.notify(new Random().nextInt(), notification);
				}
			}

			return null;
		}

		@Override
		protected void onProgressUpdate(Progress... values) {
			super.onProgressUpdate(values);
			
			for (Progress value : values) {
				NotificationManager notificationManager = (NotificationManager) value.context.getSystemService(Context.NOTIFICATION_SERVICE);
				NotificationCompat.Builder builder = new NotificationCompat.Builder(value.context);
				builder.setContentTitle(value.result);
				builder.setSmallIcon(R.drawable.ic_launcher);
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inSampleSize = 4;
				Bitmap largeIcon = BitmapFactory.decodeFile(value.image.getAbsolutePath(), options);
				builder.setLargeIcon(largeIcon);
				builder.setOngoing(false);
				builder.setAutoCancel(false);
				
				if (value.result.contains("http")) {
					Intent intent = new Intent(Intent.ACTION_VIEW);
					int start = value.result.indexOf("http");
					int end = value.result.indexOf(",", start);
					String url = value.result.substring(start, end);
					builder.setContentTitle("Uploaded Image");
					builder.setContentText(url);
					intent.setData(Uri.parse(url));
					PendingIntent pendingIntent = PendingIntent.getActivity(value.context, 0, intent, 0);
					builder.setContentIntent(pendingIntent);
				} else {
					builder.setContentTitle("Error");
					builder.setContentText("Could not upload image");
					Intent intent = new Intent(value.context, PoolActivity.class);
					PendingIntent pendingIntent = PendingIntent.getActivity(value.context, 0, intent, 0);
					builder.setContentIntent(pendingIntent);
				}
				
				Notification notification = builder.build();
				notificationManager.notify(new Random().nextInt(), notification);
			}
		}
		
		

		public static class Input {
			public final Context context;
			public final File[] files;
			public final CookieStore cookieStore;

			private Input(Context context, File[] files, CookieStore cookieStore) {
				this.context = context;
				this.files = files;
				this.cookieStore = cookieStore;
			}
		}
		
		public static class Progress {
			public final String result;
			public final File image;
			public final Context context;

			public Progress(String result, File image, Context context) {
				this.result = result;
				this.image = image;
				this.context = context;
			}
		}
	}
	
}

