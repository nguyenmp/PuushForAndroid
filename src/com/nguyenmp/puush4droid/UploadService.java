package com.nguyenmp.puush4droid;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import com.nguyenmp.puushforjava.PuushClient;
import org.apache.http.client.CookieStore;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.Queue;


public class UploadService extends Service implements ProgressListener {
	private static final int NOTIFICATION_ID = 465423;
	private static Queue<File> fileQueue = null;
	private static UploadThread uploadThread = null;

	@Override
	public IBinder onBind(Intent intent) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}
	
	public synchronized void enqueue(File file) throws SAXNotSupportedException, IOException, SAXNotRecognizedException, URISyntaxException, TransformerException {
		if (fileQueue == null) fileQueue = new LinkedList<File>();
		fileQueue.add(file);
		
		if (uploadThread == null) {
			CookieStore cookies = LoginActivity.getCookies(this);
			PuushClient.SettingsPayload settings = PuushClient.getSettings(cookies);
			String apiKey = settings.getAPIKey();
			uploadThread = new UploadThread(fileQueue, apiKey, this);
			uploadThread.start();
		}
	}

	@Override
	public void onUploaded(File file, String response) {
		updateNotification();
	}

	@Override
	public void onError(File file, Exception e) {
		updateNotification();
	}
	
	private void updateNotification() {
		//Retrieve the notification manager and create the notification builder
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
		
		//If there are files in the queue, list the number of remaining uploads. Notify as finished if otherwise.
		String contentTitle = fileQueue.size() == 0 ? "Finished uploading files" : "Uploading " + fileQueue.size() + " more files...";
		builder.setContentTitle(contentTitle);
		
		//If there is something in the queue, the notification is ongoing. False otherwise.
		builder.setOngoing(fileQueue.size() != 0);
		
		//Set icons to be the app's icon
		builder.setSmallIcon(R.drawable.ic_launcher);
		builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));
		
		//Create and update the notification
		Notification notification = builder.build();
		notificationManager.notify(NOTIFICATION_ID, notification);
	}
}

class UploadThread extends Thread {
	private final Queue<File> fileQueue;
	private final String apiKey;
	private final ProgressListener listener;
	private final ProgressHandler progressHandler;

	public UploadThread(Queue<File> fileQueue, String apiKey, ProgressListener listener) {
		this.apiKey = apiKey;
		this.fileQueue = fileQueue;
		this.listener = listener;
		progressHandler = new ProgressHandler(listener);
	}

	@Override
	public void run() {
		super.run();

		File file;
		while ((file = fileQueue.remove()) != null) {
			try {
				String result = PuushClient.upload(file, apiKey);
				listener.onUploaded(file, result);
			} catch (Exception e) {
				e.printStackTrace();
				listener.onError(file, e);
			}
		}
	}
	
	private class ProgressHandler extends Handler {
		public final int WHAT_ERROR = 1;
		public final int WHAT_RESULT = 2;
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