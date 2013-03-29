package com.nguyenmp.puush4droid;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.Toast;
import com.nguyenmp.puushforjava.PuushClient;
import org.apache.http.client.CookieStore;

import java.io.File;
import java.util.List;


public class UploadService extends IntentService {
	private NotificationManager manager;
	
	public UploadService() {
		super("");
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		String action = intent.getAction();
		
		if (action.equals(Intent.ACTION_SEND)) {
			Uri image = intent.getParcelableExtra(Intent.EXTRA_STREAM);
			Toast.makeText(this, image.toString(), Toast.LENGTH_SHORT).show();
		} else if (action.equals(Intent.ACTION_SEND_MULTIPLE)) {
			List<Uri> images = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
			if (images != null) {
				for (Uri image : images) {
					Toast.makeText(this, image.toString(), Toast.LENGTH_SHORT).show();
				}
			}
		}
	}
	
}

