package net.exclaimindustries.geohashdroid;

import android.location.Location;
import net.exclaimindustries.geohashdroid.Info;

oneway interface GeohashServiceCallback {
    /**
     * A location update!  How 'bout that?  Note that this only sends the
     * Location object itself for the time being.  All calculations such as the
     * distance or whatnot have to be done on the client end or via further
     * calls to the service.  Also note that the Location can be null.
     */
    void locationUpdate(in Location location);

    /**
     * Indicates to the client that tracking has started on the given Info
     * bundle.  This is used for clients that bind to the service before any
     * sort of tracking has begin (i.e. the widget).  That'll start the service,
     * but won't start any tracking in and of itself.  This gets called when
     * that DOES happen.  See?
     */
    void trackingStarted(in Info info);
}
