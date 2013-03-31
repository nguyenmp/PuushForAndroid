package com.nguyenmp.puush4droid;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import com.nguyenmp.puushforjava.PuushClient;
import org.apache.http.client.CookieStore;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;


public class UploadService extends Service {
	private static final String PREFERENCE_KEY_NOTIF_ID = "com.nguyenmp.puush4droid.UploadService.notifID";
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}
	
	public synchronized void upload(File[] files) throws SAXNotSupportedException, IOException, SAXNotRecognizedException, URISyntaxException, TransformerException {
		CookieStore cookies = LoginActivity.getCookies(this);
		PuushClient.SettingsPayload settings = PuushClient.getSettings(cookies);
		String apiKey = settings.getAPIKey();
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		int notifID = prefs.getInt(PREFERENCE_KEY_NOTIF_ID, 0) + 1;
		prefs.edit().putInt(PREFERENCE_KEY_NOTIF_ID, notifID).commit();
		
		ProgressListener progressListener = new BasicProgressListener(files, notifID, this);
		UploadThread uploadThread = new UploadThread(files, apiKey, progressListener);
		uploadThread.start();
	}
}

class UploadThread extends Thread {
	private final File[] files;
	private final String apiKey;
	private final ProgressListener listener;
	private final ProgressHandler progressHandler;

	public UploadThread(File[] files, String apiKey, ProgressListener listener) {
		this.apiKey = apiKey;
		this.files = files;
		this.listener = listener;
		progressHandler = new ProgressHandler(listener);
	}

	@Override
	public void run() {
		super.run();
		
		for (File file : files) {
			try {
				String result = PuushClient.upload(file, apiKey);
				Progress progress = new Progress(result, file, null);
				int what = ProgressHandler.WHAT_RESULT;
				Message message = progressHandler.obtainMessage(what, progress);
				progressHandler.sendMessage(message);
			} catch (Exception e) {
				e.printStackTrace();
				Progress progress = new Progress(null, file, e);
				int what = ProgressHandler.WHAT_ERROR;
				Message message = progressHandler.obtainMessage(what, progress);
				progressHandler.sendMessage(message);
			}
		}
	}
	
	private class ProgressHandler extends Handler {
		public static final int WHAT_ERROR = 1;
		public static final int WHAT_RESULT = 2;
		private final ProgressListener listener;

		private ProgressHandler(ProgressListener listener) {
			this.listener = listener;
		}

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			
			Progress progress = (Progress) msg.obj;
			switch (msg.what) {
				case WHAT_ERROR:
					listener.onError(progress.file, progress.exception);
					break;
				case WHAT_RESULT:
					listener.onUploaded(progress.file, progress.result);
					break;
			}
		}
	}
	
	private class Progress {
		private final String result;
		private final File file;
		private final Exception exception;

		private Progress(String result, File file, Exception exception) {
			this.result = result;
			this.file = file;
			this.exception = exception;
		}
	}
}

interface ProgressListener {
	public void onUploaded(File file, String response);
	public void onError(File file, Exception e);
}

class BasicProgressListener implements ProgressListener {
	private int uploaded = 0;
	private int errored = 0;
	private final Context context;
	private final File[] files;
	private final int notificationID;
	
	BasicProgressListener(File[] files, int notificationID, Context context) {
		this.files = files;
		this.context = context;
		this.notificationID = notificationID;
		updateNotification();
	}
	
	@Override
	public void onUploaded(File file, String response) {
		
		if (response.contains("http")) uploaded += 1;
		else errored += 1;
		
		updateNotification();
	}
	
	@Override
	public void onError(File file, Exception e) {
		errored += 1;
		updateNotification();
	}

	private void updateNotification() {
		//Retrieve the notification manager and create the notification builder
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
		
		//If there are files in the queue, list the number of remaining uploads. Notify as finished if otherwise.
		int filesRemaining = files.length - (uploaded + errored);
		String contentTitle =  filesRemaining == 0 ? "Finished uploading files" : "Uploading " + filesRemaining + " more files...";
		builder.setContentTitle(contentTitle);
		
		//If there is something in the queue, the notification is ongoing. False otherwise.
		builder.setOngoing(filesRemaining != 0);
		
		//Set icons to be the app's icon
		builder.setSmallIcon(R.drawable.ic_launcher);
		builder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher));
		
		//Create and update the notification
		Notification notification = builder.build();
		notificationManager.notify(notificationID, notification);
	}
}