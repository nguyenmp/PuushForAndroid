package com.nguyenmp.puush4droid;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;

public class BrowserActivity extends SherlockActivity {
	private WebView mWebView = null;
	@Override
	public void onCreate(Bundle inState) {
		super.onCreate(inState);
		this.requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		this.requestWindowFeature(Window.FEATURE_PROGRESS);

		this.getSupportActionBar().setHomeButtonEnabled(true);
		this.getSupportActionBar().setDisplayShowHomeEnabled(true);
		this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
		mWebView = new WebView(getApplicationContext());
		
		LayoutParams params = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		setContentView(mWebView, params);
	    mWebView.getSettings().setJavaScriptEnabled(true);
	    mWebView.setWebViewClient(new HelloWebViewClient());
	    mWebView.setWebChromeClient(new HelloWebChromeClient());
	    mWebView.getSettings().setBuiltInZoomControls(true);
	    mWebView.getSettings().setPluginsEnabled(true);
        mWebView.getSettings().setLoadWithOverviewMode(true);
        mWebView.getSettings().setUseWideViewPort(true);    
        mWebView.setOnLongClickListener(new OnLongClickListener() {
			public boolean onLongClick(View arg0) {
				return false;
			}
        });
        
        if (inState == null) {
        	System.out.println("Starting by loading from " + getIntent().getDataString());
        	mWebView.loadUrl(getIntent().getDataString());
        }
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			if (mWebView.canGoBack()) {
				mWebView.goBack();
			} else {
				finish();
			}
			return true;
		}
		
		return super.onOptionsItemSelected(item);
		
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
//		super.onSaveInstanceState(outState);
		mWebView.saveState(outState);
	}
	
	@Override
	public void onRestoreInstanceState(Bundle inState) {
//		super.onRestoreInstanceState(inState);
		mWebView.restoreState(inState);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		mWebView.destroy();
	}
	
	private class HelloWebChromeClient extends WebChromeClient {
		public void onProgressChanged(WebView view, int progress) {
			setSupportProgress(progress*100);

			if (progress == 100) {
				BrowserActivity.this.setSupportProgressBarVisibility(false);
				BrowserActivity.this.setSupportProgressBarIndeterminateVisibility(false);
			}
			else {
				BrowserActivity.this.setSupportProgressBarVisibility(true);
				BrowserActivity.this.setSupportProgressBarIndeterminateVisibility(true);
			}
        }
	}
	
	private class HelloWebViewClient extends WebViewClient {
	    @Override
	    public boolean shouldOverrideUrlLoading(WebView view, String url) {
	    	return super.shouldOverrideUrlLoading(view, url);
	    }
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if ((keyCode == KeyEvent.KEYCODE_BACK)) {
	    	if (mWebView.canGoBack())
	    		mWebView.goBack();
	    	else
	    		finish();
	    	
	        return true;
	    }
	    return super.onKeyDown(keyCode, event);
	}
	
	@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			Toast.makeText(this, "Exiting browser...", Toast.LENGTH_SHORT).show();
			finish();
		}
		
		return super.onKeyLongPress(keyCode, event);
	}
	
//	private class GauchoDownloaderTask extends AsyncTask<DownloadPayload, Integer, Void> {
//		private Notification mNotif = null;
//		
//		@Override
//		protected Void doInBackground(DownloadPayload... params) {
//			
//			return null;
//		}
//		
//		@Override
//		protected void onProgressUpdate(Integer... values) {
//			if (values.length > 0) {
//				mNotif.setLatestEventInfo(context, contentTitle, contentText, mNotif.contentIntent);
//			} else {
//				Log.d(this.getClass().getName(), "We did not receive enough values to publish progress.");
//			}
//		}
//	}

}
