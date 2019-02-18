package com.onsite.onsitefaulttracker.connectivity;

public class BLTMessage {

    private String mTime;

    private String mPhoto;

    private String mAccuracy;

    private String mSatellites;

    public String toString() {

        StringBuilder s = new StringBuilder();

        s.append("T:" + mTime);
        s.append("C" + mPhoto);
        s.append("A:" + mAccuracy);
        s.append("S:" + mSatellites);
        return s.toString();

    }
}
