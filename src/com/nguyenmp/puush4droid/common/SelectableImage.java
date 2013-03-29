package com.nguyenmp.puush4droid.common;

import java.io.Serializable;
import java.net.URI;

import com.nguyenmp.puushforjava.things.Image;

public class SelectableImage extends Image implements Serializable {
	private boolean mSelected = false;

	public SelectableImage(URI url, URI thumbnail, String title, String views, String id) {
		super(url, thumbnail, title, views, id);
	}
	
	public SelectableImage(Image image) {
		super(image.getUrl(), image.getThumbnail(), image.getTitle(), image.getViews(), image.getID());
	}
	
	public void setSelected(boolean selected) {
		mSelected = selected;
	}
	
	public boolean getSelected() {
		return mSelected;
	}

}
