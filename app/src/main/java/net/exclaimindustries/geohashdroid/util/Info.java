/**
 * Info.java
 * Copyright (C)2009 Nicholas Killewald
 * 
 * This file is distributed under the terms of the BSD license.
 * The source package should have a LICENSE file at the toplevel.
 */
package net.exclaimindustries.geohashdroid.util;

import java.util.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.maps.GeoPoint;

/**
 * <p>
 * An <code>Info</code> object holds all the relevant info that involves the 
 * map. This, for the most part, involves the final destination, the current
 * date, and the graticule. It also includes utility methods for calculating
 * data from this information, most importantly the distance between some
 * location and the final destination.
 * </p>
 * 
 * <p>
 * <code>Info</code> objects are immutable and are meant to be generated from
 * HashBuilder as the last step once it has all the data it needs.  It can,
 * however, be built from anything else as need be.
 * </p>
 * 
 * @author Nicholas Killewald
 * 
 */
public class Info implements Parcelable {
    /** The earliest date at which the 30W Rule is used. */
    private static final Calendar LIMIT_30W = new GregorianCalendar(2008, Calendar.MAY, 26);

    private double mLatitude;
    private double mLongitude;
    private Graticule mGraticule;
    private Calendar mDate;
    private boolean mRetroHash;
    private boolean mValid;

    /**
     * Creates an Info object with the given data. That's it.  If making a
     * globalhash, give the latitude and longitude as the hash, not the full
     * coordinates (and make sure the graticule is null).
     * 
     * TODO: I've really got to find a better way to do globalhashes...
     * 
     * @param latitude
     *            the destination's latitude, as a double
     * @param longitude
     *            the destination's longitude, as a double
     * @param graticule
     *            the graticule
     * @param date
     *            the date
     */
    public Info(double latitude, double longitude, Graticule graticule,
            Calendar date) {
        mLatitude = latitude;
        mLongitude = longitude;
        mGraticule = graticule;
        setDate(date);
        mValid = true;
    }
    
    /**
     * Creates an Info object with the given graticule and date, but which is
     * invalid (i.e. has no valid latitude/longitude data).  This is used when
     * StockRunner reports an error; this way, any Handler can at least figure
     * out what was going on in the first place.
     * 
     * @param graticule the graticule
     * @param date the date
     */
    public Info(Graticule graticule, Calendar date) {
        mLatitude = 0;
        mLongitude = 0;
        mGraticule = graticule;
        setDate(date);
        mValid = false;
    }

    /**
     * Deparcelizes an Info object.  Obviously, this is used internally when we
     * need to rebuild Info from a Parcel, such as during Service operations.
     * 
     * @param in the parcel to deparcelize
     */
    private Info(Parcel in) {
        readFromParcel(in);
    }


    /**
     * Gets the latitude of the final destination.
     * 
     * @return the latitude
     */
    public double getLatitude() {
        if(mGraticule != null)
            return mLatitude;
        else
            return mLatitude * 180 - 90;
    }

    /**
     * Gets the longitude of the final destination.
     * 
     * @return the longitude
     */
    public double getLongitude() {
        if(mGraticule != null)
            return mLongitude;
        else
            return mLongitude * 360 - 180;
    }

    /**
     * Gets the fractional part of the latitude of the final destination.  That
     * is, the part determined by the hash.
     *
     * @return the fractional part of the latitude
     */
    public double getLatitudeHash() {
        if(mGraticule != null)
            return Math.abs(mLatitude) - mGraticule.getLatitude();
        else
            return mLatitude;
    }

    /**
     * Gets the fractional part of the longitude of the final destination.  That
     * is, the part determined by the hash.
     *
     * @return the fractional part of the longitude
     */
    public double getLongitudeHash() {
        if(mGraticule != null)
            return Math.abs(mLongitude) - mGraticule.getLongitude();
        else
            return mLongitude;
    }

    /**
     * Returns the final destination as a GeoPoint object, which can be so
     * unbelievably handy when plotting this on a map, given that uses GeoPoints
     * and not doubles.
     * 
     * @return a GeoPoint based on the data obtained from the connection
     */
    public GeoPoint getFinalDestination() {
        return new GeoPoint((int)(getLatitude() * 1000000),
                (int)(getLongitude() * 1000000));
    }

    /**
     * Returns the final destination as a Location object, which isn't quite as
     * useful as a GeoPoint object, but you never know, it could come in handy.
     * 
     * @return a providerless Location based on the data obtained from the
     *         connection
     */
    public Location getFinalLocation() {
        Location loc = new Location("");
        loc.setLatitude(getLatitude());
        loc.setLongitude(getLongitude());

        return loc;
    }

    /**
     * Gets the graticule.  This will be null if this is a globalhash.
     * 
     * @return the graticule
     */
    public Graticule getGraticule() {
        return mGraticule;
    }

    /**
     * Gets the Calendar used to generate this set of information.
     * 
     * @return the Calendar
     */
    public Calendar getCalendar() {
        return mDate;
    }

    /**
     * Gets the Date object from the Calendar object used to generate this set
     * of information.
     * 
     * @return the Date of the Calendar
     */
    public Date getDate() {
        return mDate.getTime();
    }
    
    /**
     * Gets the distance, in meters, from the given Location and the final
     * destination.
     * 
     * @param loc
     *            Location to compare
     * @return the distance, in meters, to the final destination
     */
    public float getDistanceInMeters(Location loc) {
        return loc.distanceTo(getFinalLocation());
    }

    /**
     * Gets the distance, in meters, from the given GeoPoint and the final
     * destination.
     * 
     * @param point
     *            GeoPoint to compare
     * @return the distance, in meters, to the final destination
     */
    public float getDistanceInMeters(GeoPoint point) {
        return locationFromGeoPoint(point).distanceTo(getFinalLocation());
    }
    
    /**
     * Returns a calendar representing the date from which the stock price was
     * pulled.  That is, back a day for the 30W Rule and rewinding to Friday if
     * it falls on a weekend.
     * 
     * @return a new adjusted Calendar
     */
    public Calendar getStockCalendar() {
        return makeAdjustedCalendar(mDate, mGraticule);
    }
    
    /**
     * Returns a calendar representing the date from which the stock price was
     * pulled from a given date/graticule pair.  That is, back a day for the 30W
     * Rule or globalhashes and rewinding to Friday if it falls on a weekend.
     * 
     * @param c date to adjust
     * @param g Graticule to use to determine if the 30W Rule is in effect (if
     *          null, assumes this is a globalhash which is always back a day)
     * @return a new adjusted Calendar
     */
    public static Calendar makeAdjustedCalendar(Calendar c, Graticule g) {
        // This adjusts the calendar for both the 30W Rule and to clamp all
        // weekend stocks to the preceding Friday.  This saves a few database
        // entries, as the weekend will always be Friday's value.  Note that
        // this doesn't account for holidays when the US stocks aren't trading.
        
        // First, clone the calendar.  We don't want to muck about with the
        // original for various reasons.
        Calendar cal = (Calendar)(c.clone());
        
        // Second, 30W Rule hackery.  If g is null, assume we're in a globalhash
        // (that is, adjustment is needed).  If the date is May 26, 2008 or
        // earlier (and this isn't a globalhash), ignore it anyway (the 30W Rule
        // only applies to non-globalhashes AFTER it was created).
        if(g == null || (cal.after(LIMIT_30W) && g.uses30WRule()))
            cal.add(Calendar.DAY_OF_MONTH, -1);
        
        // Third, if this new date is a weekend, clamp it back to Friday.
        if(cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY)
            // Saturday: Back one day
            cal.add(Calendar.DAY_OF_MONTH, -1);
        else if(cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
            // SUNDAY SUNDAY SUNDAY!!!!!!: Back two days
            cal.add(Calendar.DAY_OF_MONTH, -2);
        
        // There!  Done!
        return cal;
    }

    private static Location locationFromGeoPoint(GeoPoint point) {
        // It turns out GeoPoint doesn't have the distanceTo method that
        // Location does. So, this method converts a GeoPoint into a Location
        // that doesn't have a provider.
        Location loc = new Location("");

        loc.setLatitude((float)(point.getLatitudeE6() / 1000000.0f));
        loc.setLongitude((float)(point.getLongitudeE6() / 1000000.0f));

        return loc;
    }
    
    /**
     * Determines if this Info represents a point whose date follows the 30W
     * Rule.  Note that globalhashes always follow the 30W Rule.
     * 
     * @return true if 30W or global, false if not
     */
    public boolean uses30WRule() {
        // If mGraticule is null, this is always 30W.
        if(mGraticule == null) return true;
        
        // Otherwise, just forward it to the graticule itself.
        return mDate.after(LIMIT_30W) && mGraticule.uses30WRule();
    }
    
    /**
     * Determines if this Info represents a globalhash (and thus doesn't have
     * any sort of valid Graticule data).
     * 
     * @return true if global, false if not
     */
    public boolean isGlobalHash() {
        return mGraticule == null;
    }
    
    /**
     * Determines if this Info represents a retrohash; that is, a geohash or
     * globalhash from a date in the past.  Note that this will return false for
     * geohashes from the future (i.e. a weekend when we already have the stock
     * values).
     * 
     * @return true if a retrohash, false if a current hash
     */
    public boolean isRetroHash() {
        return mRetroHash;
    }
    
    /**
     * Determines if this Info is valid.  A valid Info has latitude and
     * longitude data and can thus be sent straight to the map.  An invalid one
     * doesn't and shouldn't be used for hashing, but CAN be used in a
     * StockRunner handler to know what the date and graticule was.
     * 
     * @return true if valid, false if not
     */
    public boolean isValid() {
        return mValid;
    }
    
    public static final Parcelable.Creator<Info> CREATOR = new Parcelable.Creator<Info>() {
        public Info createFromParcel(Parcel in) {
            return new Info(in);
        }

        public Info[] newArray(int size) {
            return new Info[size];
        }
    };

    
    @Override
    public int describeContents() {
        // We don't do anything special with this.
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // Let's make us a parcel.  Order is important, remember!
        dest.writeDouble(mLatitude);
        dest.writeDouble(mLongitude);
        dest.writeParcelable(mGraticule, 0);
        dest.writeInt(mDate.get(Calendar.YEAR));
        dest.writeInt(mDate.get(Calendar.MONTH));
        dest.writeInt(mDate.get(Calendar.DAY_OF_MONTH));
        dest.writeInt(mRetroHash ? 1 : 0);
    }
    
    /**
     * Reads an incoming Parcel and deparcelizes it.  I'm going to keep using
     * the term "deparcelize" and its most logical forms until it catches on.
     * 
     * @param in parcel to deparcelize
     */
    public void readFromParcel(Parcel in) {
        // Same order!  Go!
        mLatitude = in.readDouble();
        mLongitude = in.readDouble();
        mGraticule = (Graticule)(in.readParcelable(Graticule.class.getClassLoader()));

        mDate = Calendar.getInstance();

        // In order, this better be year, month, day-of-month.
        mDate.set(in.readInt(), in.readInt(), in.readInt());

        mRetroHash = (in.readInt() == 1);
    }
    
    private void setDate(Calendar cal) {
        // First, actually set the date.
        mDate = cal;
        
        // Then, determine if this is before or after today's date.  Since a
        // straight comparison also takes time into account, we need to force
        // today's date to midnight.
        Calendar today = Calendar.getInstance();
        
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        
        // Yes, this means that if the hash is in the future, mRetroHash will
        // be false.  The only way that can happen is if this is a weekend hash
        // and we're checking on Friday or something.
        if(cal.before(today))
            mRetroHash = true;
        else
            mRetroHash = false;
    }
}
