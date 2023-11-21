package com.mingzz__.h26x.projection;

import android.media.projection.MediaProjection;
import android.os.Binder;

public abstract class ProjectionCallback extends Binder {

    public MediaProjection getProjection() {
        return null;
    }

}
