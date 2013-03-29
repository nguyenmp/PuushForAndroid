package com.nguyenmp.puush4droid;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.nguyenmp.puush4droid.common.AlertDialogFactory;
import com.nguyenmp.puush4droid.common.HandledThread;
import com.nguyenmp.puushforjava.PuushClient;
import com.nguyenmp.puushforjava.PuushClient.DisplayPayload;
import com.nguyenmp.puushforjava.things.Pool;

public class PoolActivity extends SherlockFragmentActivity {
	private String mErrorTitle = null;
	private String mErrorMessage = null;
	private DisplayPayload mDisplayPayload = null;
	public static final String EXTRA_POOL_ID = "extra+pool_id";
	public static final String EXTRA_PAGE = "extra+page";
	static final int LOGIN_ACTIVITY = 5;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		super.requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		
		setContentView(R.layout.activity_pool);
		
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		Bundle arguments;
		
		if (savedInstanceState == null) arguments = new Bundle();
		else arguments = new Bundle(savedInstanceState);
		
		
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			arguments.putInt(PoolFragment.ARGUMENT_PAGE, extras.getInt(EXTRA_PAGE, 0));
			arguments.putString(PoolFragment.ARGUMENT_POOL_ID, extras.getString(EXTRA_POOL_ID));
		}
			
		FragmentTransaction ft = super.getSupportFragmentManager().beginTransaction();
		ft.add(R.id.content, Fragment.instantiate(this, PoolFragment.class.getName(), arguments), "TAG");
		ft.commit();
	}
	
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString("error_title", mErrorTitle);
		outState.putString("error_message", mErrorMessage);
		outState.putSerializable("display_payload", mDisplayPayload);
	}
	
	public void onRestoreInstanceState(Bundle inState) {
		super.onRestoreInstanceState(inState);
		
		mErrorTitle = inState.getString("error_title");
		mErrorMessage = inState.getString("error_message");
		mDisplayPayload = (DisplayPayload) inState.getSerializable("display_payload");
		
		setDisplayPayload(mDisplayPayload);
		
    	if (mErrorTitle != null && mErrorMessage != null) {
    		showError(mErrorTitle, mErrorMessage, this);
    	}
	}

	void setLoading(boolean isLoading) {
		super.setSupportProgressBarIndeterminateVisibility(isLoading);
	}
	
	void setDisplayPayload(DisplayPayload payload) {
		mDisplayPayload = payload;
		getSupportActionBar().setTitle(payload.getDisplayedPool().getTitle() + " - " + payload.getDisplayedPool().getSize() + " items");
	}
	
	static void showError(final String title, final String message, final PoolActivity activity) {
		activity.mErrorTitle = title;
		activity.mErrorMessage = message;
		
		AlertDialog dialog = AlertDialogFactory.newAlert(title, message, activity);
		dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
			public void onDismiss(DialogInterface dialog) {
				activity.mErrorTitle = null;
				activity.mErrorMessage = null;
			}
		});
		dialog.show();
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.clear();
		getSupportMenuInflater().inflate(R.menu.activity_main, menu);
		menu.add("Refresh");
//		if (mDisplayPayload != null) {
			menu.add("Set As Default");
			menu.add("View Gallery");
			menu.add("Share Gallery");
//		}
		menu.add("Log out");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				displayPoolDialog();
				return true;
		}
		
		String title = (String) item.getTitle();
		if (title != null) {
			if (title.equalsIgnoreCase("Refresh")) {
				refresh();
			} else if (title.equalsIgnoreCase("log out")) {
				ProgressDialog dialog = new ProgressDialog(this);
				dialog.setTitle("Logging out");
				dialog.setMessage("Please wait...");
				dialog.show();
				
				LogoutThread thread = new LogoutThread(LoginActivity.getCookies(this));
				thread.setHandler(new LogoutHandler(dialog));
				thread.start();
			} else if (title.equalsIgnoreCase("Settings")) {
				Intent intent = new Intent(this, Preferences.class);
				startActivity(intent);
			} else if (title.equalsIgnoreCase("Share Gallery") && mDisplayPayload != null) {
				Intent intent = new Intent(Intent.ACTION_SEND);
				intent.putExtra(Intent.EXTRA_TEXT, "http://puush.me/" + mDisplayPayload.getAccount().getUsername() + "/Gallery");
				intent.setType("text/plain");
				startActivity(Intent.createChooser(intent, "Share with:"));
			} else if (title.equalsIgnoreCase("View Gallery") && mDisplayPayload != null) {
				Intent intent = new Intent(this, BrowserActivity.class);
				intent.setData(Uri.parse("http://puush.me/" + mDisplayPayload.getAccount().getUsername() + "/Gallery"));
				startActivity(intent);
			} else if (title.equalsIgnoreCase("Set As Default")) {
				ProgressDialog dialog = new ProgressDialog(this);
				dialog.setTitle("Setting As Default");
				dialog.setMessage("Please wait...");
				dialog.show();
				
				HandledThread thread = new Preferences.DefaultPoolThread(mDisplayPayload.getDisplayedPool().getID(), LoginActivity.getCookies(this));
				thread.setHandler(new Preferences.DefaultPoolHandler(null, dialog, mDisplayPayload.getDisplayedPool().getTitle()));
				thread.start();
			}
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	private void displayPoolDialog() {
		if (mDisplayPayload != null) {
			final List<String> poolTitles = new ArrayList<String>();
			final Pool[] pools = mDisplayPayload.getPools();
			
			for (Pool pool : pools) {
				poolTitles.add(pool.getTitle());
			}
			
			final String[] titles = poolTitles.toArray(new String[] {});
	
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Pick a pool:");
			builder.setItems(titles, new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int item) {
			        Pool pool = pools[item];
			        
			        Intent intent = new Intent(PoolActivity.this, PoolActivity.class);
			        Bundle extras = new Bundle();
			        extras.putString(EXTRA_POOL_ID, pool.getID());
			        
			        intent.putExtras(extras);
			        startActivity(intent);
			        finish();
			    }
			});
			AlertDialog alert = builder.create();
			alert.show();
		} else {
			Toast.makeText(this, "Please wait... Loading!", Toast.LENGTH_LONG).show();
		}
	}
	
	public void refresh() {
		Intent intent = new Intent(this, PoolActivity.class);
		if (getIntent().getExtras() != null) intent.putExtras(getIntent().getExtras());
		
		finish();
		startActivity(intent);
	}
	
	private class LogoutThread extends HandledThread {
		private final CookieStore mCookies;
		
		LogoutThread(final CookieStore cookies) {
			mCookies = cookies;
		}
		
		public void run() {
			try {
				PuushClient.logout(mCookies);
				dispatchMessage(null);
			} catch (ClientProtocolException e) {
				dispatchMessage(e);
				e.printStackTrace();
			} catch (IOException e) {
				dispatchMessage(e);
				e.printStackTrace();
			} catch (URISyntaxException e) {
				dispatchMessage(e);
				e.printStackTrace();
			}
		}
	}
	
	private class LogoutHandler extends Handler {
		private final ProgressDialog mDialog;
		
		LogoutHandler(final ProgressDialog dialog) {
			mDialog = dialog;
		}
		
		public void handleMessage(Message message) {
			mDialog.dismiss();
			if (message.obj == null) {
				Intent intent = new Intent(PoolActivity.this, LoginActivity.class);
				startActivityForResult(intent, PoolActivity.LOGIN_ACTIVITY);
			} else if (message.obj instanceof Exception) {
				AlertDialog dialog = AlertDialogFactory.newAlert("Error", ((Exception) message.obj).toString(), PoolActivity.this);
				dialog.show();
			}
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		System.out.println("Result recieved: " + requestCode + "  " + resultCode);
		if (requestCode == LOGIN_ACTIVITY) {
			System.out.println("From login activity");
			if (resultCode == LoginActivity.RESULT_CANCELED) {
				System.out.println("Canceled");
				finish();
			} else if (resultCode == LoginActivity.RESULT_SUCCESS) {
				System.out.println("Success");
				refresh();
			} else {
				System.out.println("Unknown: " + resultCode);
			}
		}
	}
}
