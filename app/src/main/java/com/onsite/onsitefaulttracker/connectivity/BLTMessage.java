package com.onsite.onsitefaulttracker.connectivity;

public class BLTMessage {

    private String mHeader ;

    private String mTime;

    private String mPhoto;

    private String mAccuracy;

    private String mSatellites;

    public BLTMessage() {

    }

    public void setLength(String n) {
        mHeader = n;
    }

    public void setTime(String time) {
        mTime = time;
    }

    public void setPhoto(String photo) {
        mPhoto = photo;
    }

    public void setAccuracy(String accuracy) {
        mAccuracy = accuracy;
    }

    public void setSatellites(String satellites) {
        mSatellites = satellites;
    }

    public String toString() {

        StringBuilder s = new StringBuilder();

        s.append(mHeader);
        s.append("T:" + mTime +";");
        s.append("C:" + mPhoto + ";");
        s.append("A:" + mAccuracy +";");
        s.append("S:" + mSatellites);
        return s.toString();

    }
}
