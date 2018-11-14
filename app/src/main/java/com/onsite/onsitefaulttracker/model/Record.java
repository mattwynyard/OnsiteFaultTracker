package com.onsite.onsitefaulttracker.model;

import java.util.Date;

/**
 * Created by hihi on 6/23/2016.
 *
 * Record which contains state information about a record that has been created
 * or is in progress
 */
public class Record {

    // Unique Identifier for the record
    public String recordId;

    // The name given to the record by the user upon creation
    public String recordName;

    // The date this record was created
    public Date creationDate;

    // Number of photos created so far
    public int photoCount;

    // The size in kb of the record
    // To be initialized by calculating size of stored files for a record
    public long recordSizeKB;

    // The total size in KB of the record
    public long totalSizeKB;

    // The number of files that have been uploaded to dropbox already
    public int fileUploadCount;

    // total size in KB that has been uploaded
    public long uploadedSizeKB;

    // The folder name of the record
    public String recordFolderName;

    // The total recording time in milliseconds
    public long totalRecordTime;

}
