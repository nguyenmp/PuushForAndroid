package com.nguyenmp.puush4droid;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.nguyenmp.puush4droid.common.AlertDialogFactory;
import com.nguyenmp.puush4droid.common.HandledThread;
import com.nguyenmp.puushforjava.PuushClient;
import com.nguyenmp.puushforjava.PuushClient.LoginException;
import com.nguyenmp.puushforjava.PuushClient.SettingsPayload;
import com.nguyenmp.puushforjava.things.Account;
import com.nguyenmp.puushforjava.things.Application;
import com.nguyenmp.puushforjava.things.Pool;

public class Preferences extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		
		ProgressDialog dialog = new ProgressDialog(this);
		dialog.setTitle("Loading");
		dialog.setMessage("Please wait...");
		dialog.show();
		
		HandledThread thread = new SettingsDownloadThread();
		thread.setHandler(new SettingsDownloadHandler(dialog));
		thread.start();
	}
	
	@TargetApi(8)
	private class SettingsDownloadThread extends HandledThread {
		public void run() {
			try {
				SettingsPayload payload = PuushClient.getSettings(LoginActivity.getCookies(Preferences.this));
				dispatchMessage(payload);
			} catch (ClientProtocolException e) {
				dispatchMessage(e);
				e.printStackTrace();
			} catch (SAXNotRecognizedException e) {
				dispatchMessage(e);
				e.printStackTrace();
			} catch (SAXNotSupportedException e) {
				dispatchMessage(e);
				e.printStackTrace();
			} catch (URISyntaxException e) {
				dispatchMessage(e);
				e.printStackTrace();
			} catch (IOException e) {
				dispatchMessage(e);
				e.printStackTrace();
			} catch (TransformerFactoryConfigurationError e) {
				dispatchMessage(e);
				e.printStackTrace();
			} catch (TransformerException e) {
				dispatchMessage(e);
				e.printStackTrace();
			} catch (NullPointerException e) {
				dispatchMessage(e);
				e.printStackTrace();
			}
		}
	}
	
	private class SettingsDownloadHandler extends Handler {
		private final Dialog mDialog;
		
		SettingsDownloadHandler(Dialog dialog) {
			mDialog = dialog;
		}
		
		public void handleMessage(Message message) {
			mDialog.dismiss();
			if (message.obj instanceof SettingsPayload) {
				final SettingsPayload payload = (SettingsPayload) message.obj;
				
				Preference usernamePreference = (Preference) findPreference("username");
				usernamePreference.setSummary(payload.getAccount().getUsername());
				
				Preference capacityPreference = (Preference) findPreference("capacity");
				Account account = payload.getAccount();
				capacityPreference.setSummary(account.getCurrentSpace() + "/" + account.getMaximumSpace() + " (" + account.getPercentageSpace() + ")");

				Preference accountTypePreference = (Preference) findPreference("account_type");
				accountTypePreference.setSelectable(true);
				accountTypePreference.setSummary("");
				accountTypePreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					public boolean onPreferenceClick(Preference preference) {
						Intent intent = new Intent(Preferences.this, BrowserActivity.class);
						intent.setData(Uri.parse("http://puush.me/account/go_pro"));
						startActivity(intent);
						return true;
					}
				});
				
				Preference subHistoryPreference = (Preference) findPreference("sub_history");
				subHistoryPreference.setSelectable(true);
				subHistoryPreference.setSummary(payload.getAccount().getAccountType());
				subHistoryPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					public boolean onPreferenceClick(Preference preference) {
						Intent intent = new Intent(Preferences.this, BrowserActivity.class);
						intent.setData(Uri.parse("http://puush.me/account/subscription"));
						startActivity(intent);
						return true;
					}
				});
				
				Preference apiKeyPreference = (Preference) findPreference("api_key");
				apiKeyPreference.setSelectable(true);
				apiKeyPreference.setSummary(payload.getAPIKey());
				apiKeyPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					public boolean onPreferenceClick(Preference preference) {
						ProgressDialog dialog = new ProgressDialog(Preferences.this);
						dialog.setTitle("Resetting API Key");
						dialog.setMessage("Please wait...");
						dialog.show();
						
						HandledThread thread = new ResetApiKeyThread(LoginActivity.getCookies(Preferences.this));
						thread.setHandler(new ResetApiKeyHandler(dialog, Preferences.this));
						thread.start();
						return true;
					}
				});
				
				Preference passwordPreference = (Preference) findPreference("password");
				passwordPreference.setSelectable(true);
				passwordPreference.setSummary(payload.getPassword());
				passwordPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					public boolean onPreferenceClick(Preference preference) {
						final Dialog dialog = new Dialog(Preferences.this);
						dialog.setTitle("Change Password:");
						dialog.setContentView(R.layout.dialog_change_password);
						final EditText currentPassword = (EditText) dialog.findViewById(R.id.current_password);
						final EditText newPassword = (EditText) dialog.findViewById(R.id.new_password);
						final EditText confirmPassword = (EditText) dialog.findViewById(R.id.confirm_new_password);
						
						Button changePassword = (Button) dialog.findViewById(R.id.change_password);
						changePassword.setOnClickListener(new OnClickListener() {
							public void onClick(View v) {
								if (newPassword.getText().toString().equals(confirmPassword.getText().toString())) {
									ProgressDialog dialog = new ProgressDialog(Preferences.this);
									dialog.setTitle("Changing Password");
									dialog.setMessage("Please wait...");
									dialog.show();
									HandledThread thread = new ChangePasswordThread(currentPassword.getText().toString(), newPassword.getText().toString(), LoginActivity.getCookies(Preferences.this));
									thread.setHandler(new ChangePasswordHandler(dialog));
									thread.start();
								} else {
									AlertDialog dialog = AlertDialogFactory.newAlert("Error", "Passwords do not match", Preferences.this);
									dialog.show();
								}
							}
						});
						
						Button cancelPassword = (Button) dialog.findViewById(R.id.cancel_change_password);
						cancelPassword.setOnClickListener(new OnClickListener() {
							public void onClick(View v) {
								dialog.dismiss();
							}
						});
						
						dialog.show();
						
						return true;
					}
				});
				
				
				PreferenceCategory thirdPartySupportCategory = (PreferenceCategory) findPreference("thirdPartySupport");
				Application[] applications = payload.getApplications();
				
				for (final Application application : applications) {
					Preference preference = new Preference(Preferences.this);
					preference.setTitle(application.getTitle());
					preference.setSummary(application.getSummary());
					preference.setSelectable(true);
					preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
						public boolean onPreferenceClick(Preference preference) {
							Intent intent = new Intent(Preferences.this, BrowserActivity.class);
							intent.setData(Uri.parse(application.getURL()));
							startActivity(intent);
							return true;
						}
					});
					thirdPartySupportCategory.addPreference(preference);
				}
				
				Preference defaultPoolPreference = findPreference("defaultPool");
				defaultPoolPreference.setSelectable(true);
				defaultPoolPreference.setSummary(payload.getSelectedPool() == null ? "null" : payload.getSelectedPool().getTitle());
				defaultPoolPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					public boolean onPreferenceClick(Preference preference) {
						final List<String> list = new ArrayList<String>();
						for (Pool pool : payload.getPools()) {
							list.add(pool.getTitle());
						}
						
						AlertDialog.Builder builder = new AlertDialog.Builder(Preferences.this);
						builder.setTitle("Set Default Pool:");
						builder.setItems(list.toArray(new String[] {}), new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int position) {
								ProgressDialog progress = new ProgressDialog(Preferences.this);
								progress.setTitle("Setting Default Pool");
								progress.setMessage("Please wait...");
								progress.show();
								
								HandledThread thread = new DefaultPoolThread(payload.getPools()[position].getID(), LoginActivity.getCookies(Preferences.this));
								thread.setHandler(new DefaultPoolHandler(Preferences.this, progress, payload.getPools()[position].getTitle()));
								thread.start();
							}
						});
						
						builder.show();
						return true;
					}
					
				});
				
			} else if (message.obj instanceof NullPointerException) {
				AlertDialog.Builder builder = new AlertDialog.Builder(Preferences.this);
				builder.setTitle("Login Error");
				builder.setMessage("Please log in again.");
				builder.show();
			} else if (message.obj instanceof Exception) {
				AlertDialog.Builder builder = new AlertDialog.Builder(Preferences.this);
				builder.setTitle("Error");
				builder.setMessage(((Exception) message.obj).toString());
				builder.show();
			}
		}
	}
	
	private class ResetApiKeyThread extends HandledThread {
		private final CookieStore mCookies;
		
		private ResetApiKeyThread(CookieStore cookies) {
			mCookies = cookies;
		}
		
		public void run() {
			try {
				String apiKey = PuushClient.resetAPIKey(mCookies);
				dispatchMessage(apiKey);
			} catch (ClientProtocolException e) {
				dispatchMessage(e);
				e.printStackTrace();
			} catch (URISyntaxException e) {
				dispatchMessage(e);
				e.printStackTrace();
			} catch (IOException e) {
				dispatchMessage(e);
				e.printStackTrace();
			} catch (LoginException e) {
				dispatchMessage(e);
				e.printStackTrace();
			}
		}
	}
	
	private class ResetApiKeyHandler extends Handler {
		private final Dialog mDialog;
		private final Context mContext;
		
		ResetApiKeyHandler(Dialog dialog, Context context) {
			mDialog = dialog;
			mContext = context;
		}
		
		public void handleMessage(Message message) {
			mDialog.dismiss();
			if (message.obj instanceof String) {
				Preference apiKeyPreference = findPreference("api_key");
				apiKeyPreference.setSummary((String) message.obj);
			} else if (message.obj instanceof Exception) {
				AlertDialog dialog = AlertDialogFactory.newAlert("Error", ((Exception) message.obj).toString(), mContext);
				dialog.show();
			}
		}
	}
	
	private class ChangePasswordThread extends HandledThread {
		private final String mCurrentPassword;
		private final String mNewPassword;
		private final CookieStore mCookies;
		
		private ChangePasswordThread(String currentPassword, String newPassword, CookieStore cookies) {
			mCurrentPassword = currentPassword;
			mNewPassword = newPassword;
			mCookies = cookies;
		}
		
		public void run() {
			try {
				String response = PuushClient.changePassword(mCurrentPassword, mNewPassword, mCookies);
				dispatchMessage(response);
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
	
	private class ChangePasswordHandler extends Handler {
		private final Dialog mDialog;
		
		private ChangePasswordHandler(Dialog dialog) {
			mDialog = dialog;
		}
		
		public void handleMessage(Message message) {
			mDialog.dismiss();
			if (message.obj == null) {
				AlertDialog dialog = AlertDialogFactory.newAlert("Password Changed!", "Success!", mDialog.getContext());
				dialog.show();
			} else if (message.obj instanceof String) {
				AlertDialog dialog = AlertDialogFactory.newAlert("Error:", (String) message.obj, mDialog.getContext());
				dialog.show();
			}
		}
	}
	
	protected static class DefaultPoolThread extends HandledThread {
		private final CookieStore mCookies;
		private final String mPoolID;
		
		DefaultPoolThread(String poolID, CookieStore cookies) {
			mCookies = cookies;
			mPoolID = poolID;
		}
		
		public void run() {
			try {
				dispatchMessage(PuushClient.setDefaultPool(mPoolID, mCookies));
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
	
	protected static class DefaultPoolHandler extends Handler {
		private final Dialog mDialog;
		private final String mPoolTitle;
		private final PreferenceActivity mActivity;
		
		DefaultPoolHandler(PreferenceActivity activity, Dialog dialog, String poolTitle) {
			mActivity = activity;
			mDialog = dialog;
			mPoolTitle = poolTitle;
		}
		
		public void handleMessage(Message message) {
			mDialog.dismiss();
			
			if (message.obj instanceof Boolean) {
				if ((Boolean) message.obj) {
					AlertDialogFactory.newAlert("Default Pool", "Success!", mDialog.getContext()).show();
					if (mActivity != null) {
						Preference preference = mActivity.findPreference("defaultPool");
						preference.setSummary(mPoolTitle);
					}
				} else {
					AlertDialogFactory.newAlert("Default Pool", "Failed!", mDialog.getContext()).show();
				}
			} else if (message.obj instanceof Exception) {
				AlertDialogFactory.newAlert("Error", ((Exception) message.obj).toString(), mDialog.getContext()).show();
			}
		}
	}
}
