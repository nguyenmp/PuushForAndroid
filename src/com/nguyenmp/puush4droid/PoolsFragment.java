package com.nguyenmp.puush4droid;

import java.util.List;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.nguyenmp.puushforjava.things.Image;

public class PoolsFragment extends SherlockListFragment {
	
	@Override
	public void onActivityCreated(Bundle inState) {
		super.onActivityCreated(inState);
		
		
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle inState) {
		//TODO: Return inflated view
		
		return null;
	}
	
	@Override
	public void onListItemClick(ListView listView, View view, int position, long id) {
		
	}
	
	private class PoolListAdapter extends BaseAdapter {
		private final List<Image> mImages;
		
		PoolListAdapter(final List<Image> images) {
			mImages = images;
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

		public View getView(int position, View convertView, ViewGroup parent) {
			return null;
		}
		
	}
}
