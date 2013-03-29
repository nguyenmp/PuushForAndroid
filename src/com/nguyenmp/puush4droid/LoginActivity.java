package com.nguyenmp.puush4droid;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.Button;
import android.widget.EditText;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.loopj.android.http.PersistentCookieStore;
import com.nguyenmp.puush4droid.common.AlertDialogFactory;
import com.nguyenmp.puush4droid.common.HandledThread;
import com.nguyenmp.puushforjava.PuushClient;

public class LoginActivity extends SherlockActivity {
	
	public static int RESULT_SUCCESS = 1, RESULT_FAIL = 2;
	private static String mErrorTitle = null;
	private static String mErrorMessage = null;
	private static LoginThread mLoginThread = null;
	public static final String EXTRA_REDIRECT = "redirect_key";
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        
        super.setContentView(R.layout.login);
        
    	EditText emailEditText = (EditText) super.findViewById(R.id.login_email_edit_text);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        emailEditText.setText(prefs.getString("email", ""));
        
        Button loginButton = (Button) super.findViewById(R.id.login_login_button);
        loginButton.setOnClickListener(new LoginClickListener());
        
        setLoading(mLoginThread != null);
        
        if (mLoginThread != null) {
        	mLoginThread.setHandler(new LoginActivity.LoginHandler(LoginActivity.this));
        } else {
        	//Do nothing
        }
    }
    
    public void onSaveInstanceState(Bundle outState) {
    	EditText emailEditText = (EditText) super.findViewById(R.id.login_email_edit_text);
    	outState.putString("email", emailEditText.getText().toString());
    	
    	EditText passwordEditText = (EditText) super.findViewById(R.id.login_password_edit_text);
    	outState.putString("password", passwordEditText.getText().toString());
    }
    
    public void onRestoreInstanceState(Bundle inState) {
    	EditText emailEditText = (EditText) super.findViewById(R.id.login_email_edit_text);
    	emailEditText.setText(inState.getString("email"));
    	
    	EditText passwordEditText = (EditText) super.findViewById(R.id.login_password_edit_text);
    	passwordEditText.setText(inState.getString("password"));
    	
    	if (mErrorTitle != null && mErrorMessage != null) {
    		AlertDialog dialog = AlertDialogFactory.newAlert(mErrorTitle, mErrorMessage, this);
    		dialog.show();
    		dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
				public void onDismiss(DialogInterface dialog) {
					mErrorTitle = null;
					mErrorMessage = null;
				}
    		});
    	}
    }
    
    private void setLoading(boolean isLoading) {
    	super.setSupportProgressBarIndeterminateVisibility(isLoading);
    	
    	EditText emailEditText = (EditText) super.findViewById(R.id.login_email_edit_text);
    	emailEditText.setEnabled(!isLoading);
    	
    	EditText passwordEditText = (EditText) super.findViewById(R.id.login_password_edit_text);
    	passwordEditText.setEnabled(!isLoading);
    	
    	Button loginButton = (Button) super.findViewById(R.id.login_login_button);
    	loginButton.setEnabled(!isLoading);
    }
    
    private class LoginClickListener implements OnClickListener {
		public void onClick(View view) {
			setLoading(true);

	    	EditText emailEditText = (EditText) findViewById(R.id.login_email_edit_text);
	    	String email = emailEditText.getText().toString();
	    	
	    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(LoginActivity.this);
	    	prefs.edit().putString("email", email).commit();
	    	
	    	EditText passwordEditText = (EditText) findViewById(R.id.login_password_edit_text);
	    	String password = passwordEditText.getText().toString();
	    	
	    	mLoginThread = new LoginThread(email, password);
	    	mLoginThread.setHandler(new LoginActivity.LoginHandler(LoginActivity.this));
	    	
	    	mLoginThread.start();
		}
    }
    
    private class LoginThread extends HandledThread {
    	private final String mEmail;
    	private final String mPassword;
    	
    	LoginThread(final String email, final String password) {
    		mEmail = email;
    		mPassword = password;
    	}
    	
    	public void run() {
    		try {
				CookieStore cookies = PuushClient.login(mEmail, mPassword);
				dispatchMessage(cookies);
			} catch (ClientProtocolException e) {
				dispatchMessage(e);
				e.printStackTrace();
			} catch (URISyntaxException e) {
				dispatchMessage(e);
				e.printStackTrace();
			} catch (IOException e) {
				dispatchMessage(e);
				e.printStackTrace();
			}
    	}
    }
    
    private static class LoginHandler extends Handler {
    	private final LoginActivity mActivity;
    	
    	private LoginHandler(final LoginActivity activity) {
    		mActivity = activity;
    	}
    	
    	public void handleMessage(Message message) {
    		mActivity.setLoading(false);
    		mLoginThread = null;
    		
    		if (message.obj == null) {
    			mErrorTitle = "Bad Login";
    			mErrorMessage = "Username or password are incorrect";
    			AlertDialog dialog = AlertDialogFactory.newAlert("Bad Login", "Username or password are incorrect", mActivity);
    			dialog.show();
    			dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
					public void onDismiss(DialogInterface dialog) {
						mErrorTitle = null;
						mErrorMessage = null;
					}
				});
    		} else if (message.obj instanceof CookieStore) {
    			LoginActivity.setCookies(mActivity, (CookieStore) message.obj);
    			
    			Bundle extras = mActivity.getIntent().getExtras();
    			
    			if (extras == null || extras.getBoolean(EXTRA_REDIRECT, true)) {
	    			Intent intent = new Intent(mActivity, PoolActivity.class);
	    			mActivity.startActivity(intent);
    			}
    			
    			mActivity.setResult(LoginActivity.RESULT_SUCCESS);
    			mActivity.finish();
    		} else if (message.obj instanceof Exception) {
    			AlertDialog dialog = AlertDialogFactory.newAlert("Error:", ((Exception) message.obj).toString(), mActivity);
    			dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
					public void onDismiss(DialogInterface dialog) {
						mErrorTitle = null;
						mErrorMessage = null;
					}
				});
    			dialog.show();
    		}
    	}
    }
	
	public static void setCookies(Context context, CookieStore cookies) {
		PersistentCookieStore store = new PersistentCookieStore(context);
		store.clear();
		
		CookieSyncManager.createInstance(context);
		CookieManager cookieManager = CookieManager.getInstance();
		cookieManager.removeAllCookie();
		cookieManager.setAcceptCookie(true);
		
		if (cookies != null) {
			for (Cookie cookie : cookies.getCookies()) {
				store.addCookie(cookie);
				cookieManager.setCookie(cookie.getDomain(), cookie.getName() + "=" + cookie.getValue());
			}
		}
		
		CookieSyncManager.getInstance().sync();
	}
	
	public static CookieStore getCookies(Context context) {
		PersistentCookieStore store = new PersistentCookieStore(context);
		return store;
	}
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
//                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
