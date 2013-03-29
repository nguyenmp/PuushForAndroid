package com.nguyenmp.puush4droid;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListFragment;
import com.nguyenmp.puush4droid.common.AlertDialogFactory;
import com.nguyenmp.puush4droid.common.HandledThread;
import com.nguyenmp.puush4droid.common.SelectableImage;
import com.nguyenmp.puushforjava.PuushClient;
import com.nguyenmp.puushforjava.PuushClient.DisplayPayload;
import com.nguyenmp.puushforjava.things.DisplayedPool;
import com.nguyenmp.puushforjava.things.Image;
import com.nguyenmp.puushforjava.things.Pool;

public class PoolFragment extends SherlockListFragment {
	public static final String ARGUMENT_POOL_ID = "pool+id";
	public static final String ARGUMENT_PAGE = "page+";
	
	private LinearLayout mButtons;
	private List<SelectableImage> mImages;
	private BaseAdapter mListAdapter;
	private PoolActivity mParent;
	private DisplayPayload mPayload = null;
	private TextView mEmptyTextView = null;
	
	private static PoolDownloaderThread mDownloaderThread = null;
	
	@Override
	public void onActivityCreated(Bundle inState) {
		super.onActivityCreated(inState);
		
		mImages = new ArrayList<SelectableImage>();
		mParent = (PoolActivity) getSherlockActivity();
		
		setListAdapter((mListAdapter = new PoolListAdapter(mImages, mParent)));
		
		setLoading(mDownloaderThread != null);
		
		
		if (mDownloaderThread == null) {
			Bundle arguments = getArguments();
			if (arguments != null && arguments.containsKey("data_list") && 
					((List<SelectableImage>) arguments.getSerializable("data_list")).size() > 0) {
				
				mImages.addAll((List<SelectableImage>) arguments.getSerializable("data_list"));
				mListAdapter.notifyDataSetChanged();
				
				updateActionButtons();
			} else {
				String poolID = arguments.getString(ARGUMENT_POOL_ID);
				int page = arguments.getInt(ARGUMENT_PAGE, 1);
				
				mDownloaderThread = new PoolDownloaderThread(poolID, page, LoginActivity.getCookies(mParent));
				mDownloaderThread.setHandler(new PoolDownloaderHandler(mParent));
				mDownloaderThread.start();
				setLoading(true);
			}
		} else {
			mDownloaderThread.setHandler(new PoolDownloaderHandler(mParent));
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
//		outState.putSerializable("data_list", (Serializable) mImages);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle inState) {
		View view = inflater.inflate(R.layout.fragment_pool, container, false);
		
		mButtons = (LinearLayout) view.findViewById(R.id.fragment_pool_buttons);
		
		Button deleteButton = (Button) view.findViewById(R.id.fragment_pool_delete);
		deleteButton.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				doDeleteSelected();
			}
		});
		
		Button moveButton = (Button) view.findViewById(R.id.fragment_pool_move);
		moveButton.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				doMoveSelected();
			}
		});
		
		Button shareButton = (Button) view.findViewById(R.id.fragment_pool_share);
		shareButton.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				doShareSelected();
			}
		});
		
		final Button leftButton = (Button) view.findViewById(R.id.fragment_pool_nav_left);
		leftButton.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				if (leftButton.isEnabled()) {
					Intent intent = new Intent(getActivity(), PoolActivity.class);
					intent.putExtra(PoolActivity.EXTRA_POOL_ID, getArguments().getString(PoolFragment.ARGUMENT_POOL_ID));
					intent.putExtra(PoolActivity.EXTRA_PAGE, getArguments().getInt(PoolFragment.ARGUMENT_PAGE) - 1);
					mParent.finish();
					mParent.startActivity(intent);
				}
			}
		});
		
		final Button rightButton = (Button) view.findViewById(R.id.fragment_pool_nav_right);
		rightButton.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				if (rightButton.isEnabled()) {
					Intent intent = new Intent(getActivity(), PoolActivity.class);
					intent.putExtra(PoolActivity.EXTRA_POOL_ID, getArguments().getString(PoolFragment.ARGUMENT_POOL_ID));
					intent.putExtra(PoolActivity.EXTRA_PAGE, getArguments().getInt(PoolFragment.ARGUMENT_PAGE, 1) + 1);
					mParent.finish();
					mParent.startActivity(intent);
				}
			}
		});
		
		final Button centerButton = (Button) view.findViewById(R.id.fragment_pool_nav_center);
		centerButton.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				if (centerButton.isEnabled()) {
					final ArrayList<String> list = new ArrayList<String>();
					for (int i = 1; i <= mPayload.getDisplayedPool().getMaxPage(); i++) {
						list.add(Integer.toString(i));
					}
					
					AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
					builder.setTitle("Go to page:");
					builder.setItems(list.toArray(new String[] {}), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int index) {
							Intent intent = new Intent(getActivity(), PoolActivity.class);
							intent.putExtra(PoolActivity.EXTRA_POOL_ID, getArguments().getString(PoolFragment.ARGUMENT_POOL_ID));
							intent.putExtra(PoolActivity.EXTRA_PAGE, Integer.valueOf(list.get(index)));
							System.out.println("Loading page: " + list.get(index));
							mParent.finish();
							mParent.startActivity(intent);
						}
					});
					builder.show();
				}
			}
		});
		
		mEmptyTextView = (TextView) view.findViewById(R.id.fragment_pool_empty);
		
		return view;
	}
	
	private List<SelectableImage> getSelectedImages() {
		List<SelectableImage> images = new ArrayList<SelectableImage>();
		
		for (SelectableImage image : mImages) {
			if (image.getSelected()) {
				images.add(image);
			}
		}
		return images;
	}
	
	private void doShareSelected() {
		List<SelectableImage> images = getSelectedImages();
		
		StringBuilder stringBuilder = new StringBuilder();
		
		for (Image image : images) {
			if (stringBuilder.length() != 0) stringBuilder.append("\n");
			stringBuilder.append(image.getUrl().toString());
		}
		
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("text/plain");
		intent.putExtra(Intent.EXTRA_TEXT, stringBuilder.toString());  
		
		startActivity(Intent.createChooser(intent, "Send via:"));
	}
	
	private void doMoveSelected() {
		if (mPayload != null) {
			final List<String> poolTitles = new ArrayList<String>();
			final Pool[] pools = mPayload.getPools();
			
			for (Pool pool : pools) {
				poolTitles.add(pool.getTitle());
			}
			
			final String[] titles = poolTitles.toArray(new String[] {});
	
			AlertDialog.Builder builder = new AlertDialog.Builder(mParent);
			builder.setTitle("Pick a pool:");
			builder.setItems(titles, new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int item) {
					mParent.setLoading(true);
					List<String> imageIDs = new ArrayList<String>();
					
					for (Image image : getSelectedImages()) {
						imageIDs.add(image.getID());
					}
					
					MoveThread thread = new MoveThread(imageIDs.toArray(new String[] {}), pools[item].getID(), LoginActivity.getCookies(mParent));
					thread.setHandler(new MoveHandler(mParent));
					thread.start();
			    }
			});
			AlertDialog alert = builder.create();
			alert.show();
		}
	}
	
	private void doDeleteSelected() {
		AlertDialog.Builder builder = new AlertDialog.Builder(mParent);
		builder.setTitle("Confirm Delete:");
		builder.setMessage("Are you really sure you want to delete these images?");
		builder.setPositiveButton("Yes. Delete these images.", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				mParent.setLoading(true);
				List<String> imageIDs = new ArrayList<String>();
				
				for (Image image : getSelectedImages()) {
					imageIDs.add(image.getID());
				}
				
				DeleteThread thread = new DeleteThread(imageIDs.toArray(new String[] {}), LoginActivity.getCookies(mParent));
				thread.setHandler(new DeleteHandler(mParent));
				thread.start();
			}
		});
		builder.setNegativeButton("No.  Don't delete!", null);
		builder.show();
	}
	
//	@Override
//	public void onListItemClick(ListView listView, View view, int position, long id) {
//		Image image = mImages.get(position);
//		
//		Intent intent = new Intent(Intent.ACTION_VIEW);
//		intent.setData(Uri.parse(image.getUrl().toString()));
//		
//		startActivity(intent);
//	}
	
	private class PoolListAdapter extends BaseAdapter {
		private final List<SelectableImage> mImages;
		private final Context mContext;
		private final Map<String, Bitmap> mThumbnails = new HashMap<String, Bitmap>();
		
		PoolListAdapter(final List<SelectableImage> images, final Context context) {
			mImages = images;
			mContext = context;
		}
		
		public int getCount() {
			return mImages.size();
		}

		public Object getItem(int position) {
			return mImages.get(position);
		}

		public long getItemId(int position) {
			return 0;
		}

		public View getView(final int position, View convertView, final ViewGroup parent) {
			final SelectableImage image = mImages.get(position);
			
			convertView = LayoutInflater
				.from(mContext)
				.inflate(R.layout.list_item_image, parent, false);
			
			ImageView imageView = (ImageView) convertView.findViewById(R.id.list_item_image_thumbnail);
			ProgressBar progressBar = (ProgressBar) convertView.findViewById(R.id.list_item_image_progress);
			if (mThumbnails.containsKey(image.getThumbnail().toString())) {
				if (mThumbnails.get(image.getThumbnail().toString()) == null) {
					imageView.setVisibility(View.GONE);
					progressBar.setVisibility(View.VISIBLE);
				} else {
					imageView.setVisibility(View.VISIBLE);
					progressBar.setVisibility(View.GONE);
					imageView.setImageBitmap(mThumbnails.get(image.getThumbnail().toString()));
				}
			} else {
				mThumbnails.put(image.getThumbnail().toString(), null);
				ThumbnailDownloadThread thread = new ThumbnailDownloadThread(image.getThumbnail());
				thread.setHandler(new ThumbnailDownloadHandler(image.getThumbnail()));
				thread.start();
			}
			
			
			TextView titleTextView = (TextView) convertView.findViewById(R.id.list_item_image_title);
			titleTextView.setText(image.getTitle());
			
			TextView viewsTextView = (TextView) convertView.findViewById(R.id.list_item_image_views);
			viewsTextView.setText(image.getViews());
			
			CheckBox selectedCheckBox = (CheckBox) convertView.findViewById(R.id.list_item_image_checkbox);
			selectedCheckBox.setChecked(image.getSelected());
			
			selectedCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					image.setSelected(isChecked);
					updateActionButtons();
				}
			});
			
			
			convertView.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					Intent intent = new Intent(getActivity(), BrowserActivity.class);
					intent.setData(Uri.parse(image.getUrl().toString()));
					
					startActivity(intent);
				}
			});
			
			return convertView;
		}
		
		
		private class ThumbnailDownloadThread extends HandledThread {
			private final URI mURL;
			
			private ThumbnailDownloadThread(final URI url) {
				mURL = url;
			}
			
			public void run() {
				HttpClient client = new DefaultHttpClient();
				HttpContext context = new BasicHttpContext();;
				context.setAttribute(ClientContext.COOKIE_STORE, LoginActivity.getCookies(mContext));
				HttpGet get = new HttpGet(mURL.toString());
				
				try {
					HttpResponse response = client.execute(get, context);
					Bitmap bitmap = BitmapFactory.decodeStream(response.getEntity().getContent());
					dispatchMessage(bitmap);
					get.abort();
					client.getConnectionManager().shutdown();
				} catch (IOException e) {
					dispatchMessage(e);
					e.printStackTrace();
				}
			}
		}
		
		private class ThumbnailDownloadHandler extends Handler {
			private final URI mURL;
			
			private ThumbnailDownloadHandler(final URI url) {
				mURL = url;
			}
			
			public void handleMessage(Message message) {
				if (message.obj instanceof Bitmap) {
					mThumbnails.put(mURL.toString(), (Bitmap) message.obj);
					notifyDataSetChanged();
				} else if (message.obj instanceof IOException) {
					//Do nothing
				}
			}
		}
	}
	
	

	private class PoolDownloaderThread extends HandledThread {
		private final String mPoolID;
		private final int mPage;
		private final CookieStore mCookies;
		
		PoolDownloaderThread(final String poolID, final int page, final CookieStore cookies) {
			mPoolID = poolID;
			mPage = page;
			mCookies = cookies;
		}
		
		@SuppressLint("NewApi")
		public void run() {
			try {
				DisplayPayload payload = PuushClient.getPool(mPoolID, mPage, mCookies);
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

	private class PoolDownloaderHandler extends Handler {
		private final PoolActivity mActivity;
		
		PoolDownloaderHandler(final PoolActivity activity) {
			mActivity = activity;
		}
		
		public void handleMessage(Message message) {
			mActivity.setLoading(false);
			mDownloaderThread = null;
			
			if (message.obj == null || message.obj instanceof NullPointerException) {
				LoginActivity.setCookies(mActivity, null);
				
				Intent intent = new Intent(mActivity, LoginActivity.class);
				intent.putExtra(LoginActivity.EXTRA_REDIRECT, false);
				mActivity.startActivityForResult(intent, PoolActivity.LOGIN_ACTIVITY);
				
			} else if (message.obj instanceof DisplayPayload) {
				DisplayPayload displayPayload = (DisplayPayload) message.obj;
				mPayload = displayPayload;
				mActivity.setDisplayPayload(displayPayload);
				
				DisplayedPool displayedPool = displayPayload.getDisplayedPool();
				addImages(displayedPool.getImages());
				
				Intent intent = getActivity().getIntent();
				intent.putExtra(PoolActivity.EXTRA_PAGE, displayedPool.getCurrentPage());
				
				Button leftButton = (Button) mParent.findViewById(R.id.fragment_pool_nav_left);
				leftButton.setEnabled(displayedPool.getCurrentPage() > 1);
				
				Button centerButton = (Button) mParent.findViewById(R.id.fragment_pool_nav_center);
				centerButton.setEnabled(displayedPool.getMaxPage() > 1);
				centerButton.setText("" + displayedPool.getCurrentPage() + "/" + displayedPool.getMaxPage());
				
				Button rightButton = (Button) mParent.findViewById(R.id.fragment_pool_nav_right);
				rightButton.setEnabled(displayedPool.getCurrentPage() < displayedPool.getMaxPage());
				
				if (displayedPool.getImages().length == 0) {
					if (mEmptyTextView != null) mEmptyTextView.setVisibility(View.VISIBLE);
					else Toast.makeText(mActivity, "No Images in this pool", Toast.LENGTH_LONG).show();
				}
			} else if (message.obj instanceof Exception) {
				PoolActivity.showError("Error", ((Exception) message.obj).toString(), mActivity);
			}
		}
	}
	
	private class DeleteThread extends HandledThread {
		private final String[] mIDs;
		private final CookieStore mCookies;
		
		private DeleteThread(String[] ids, CookieStore cookies) {
			mIDs = ids;
			mCookies = cookies;
		}
		
		public void run() {
			try {
				PuushClient.deleteImages(mIDs, mCookies);
				dispatchMessage(null);
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



	private class DeleteHandler extends Handler {
		private final Context mContext;
		
		private DeleteHandler(Context context) {
			mContext = context;
		}
		
		public void handleMessage(Message message) {
			mParent.setLoading(false);
			if (message.obj == null) {
				Toast.makeText(mContext, "Delete successful!", Toast.LENGTH_LONG).show();

				startActivity(mParent.getIntent());
				mParent.finish();
			} else if (message.obj instanceof Exception) {
				AlertDialog dialog = AlertDialogFactory.newAlert("Error", ((Exception) message.obj).toString(), mContext);
				dialog.show();
			}
		}
	}



	private class MoveThread extends HandledThread {
		private final String[] mIDs;
		private final String mPoolID;
		private final CookieStore mCookies;
		
		private MoveThread(String[] ids, String poolID, CookieStore cookies) {
			mIDs = ids;
			mPoolID = poolID;
			mCookies = cookies;
		}
		
		public void run() {
			try {
				PuushClient.moveImages(mIDs, mPoolID, mCookies);
				dispatchMessage(null);
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



	private class MoveHandler extends Handler {
		private final Context mContext;
		
		private MoveHandler(Context context) {
			mContext = context;
		}
		
		public void handleMessage(Message message) {
			mParent.setLoading(false);
			if (message.obj == null) {
				Toast.makeText(mContext, "Move successful!", Toast.LENGTH_LONG).show();
				
				mParent.startActivity(mParent.getIntent());
				mParent.finish();
			} else if (message.obj instanceof Exception) {
				AlertDialog dialog = AlertDialogFactory.newAlert("Error", ((Exception) message.obj).toString(), mContext);
				dialog.show();
			}
		}
	}

	private void setLoading(boolean isLoading) {
		mParent.setLoading(isLoading);
	}
	
	private void addImages(Image[] images) {
		List<SelectableImage> imageList = new ArrayList<SelectableImage>();
		
		for (Image image : images) {
			imageList.add(new SelectableImage(image));
		}
		
		mImages.addAll(imageList);
		mListAdapter.notifyDataSetChanged();
		
		updateActionButtons();
	}
	
	private void updateActionButtons() {
		if (mButtons == null) return;
		
		for (SelectableImage image : mImages) {
			if (image.getSelected() == true) {
				mButtons.setVisibility(View.VISIBLE);
				return;
			}
		}
		mButtons.setVisibility(View.GONE);
	}
	
}
