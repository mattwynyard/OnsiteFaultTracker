package com.onsite.onsitefaulttracker.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.onsite.onsitefaulttracker.connectivity.TcpConnection;
import com.onsite.onsitefaulttracker.model.Record;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Created by hihi on 6/23/2016.
 *
 * RecordUtil,  Utility class for handling saving and retrieving record details.
 */
public class RecordUtil {

    // The tag name of this utility class
    private static final String TAG = Record.class.getSimpleName();

    // The folder where all records will be stored
    private static final String RECORD_STORAGE_FOLDER = "/OnSite";

    // The record preferences name
    private static final String RECORD_PREFERENCES = "record_prefs";

    // The current record keys
    private static final String CURRENT_RECORD_KEY = "current_record";

    // The format of folder names when converted from a date
    private static final String FOLDER_DATE_FORMAT = "yy_MM_dd";

    // The static instance of this class which will be initialized once then reused
    // throughout the app
    private static RecordUtil sRecordUtil;

    // The number of records in storage
    private int mStoredRecordCount;

    // The current active record
    private Record mCurrentRecord;

    // Gson object which converts json strings to objects and vice versa
    private Gson mGson;

    // The application context
    private Context mContext;

    /**
     * initializes RecordUtil.
     *
     * @param context
     */
    public static void initialize(final Context context) {
        sRecordUtil = new RecordUtil(context);
    }

    /**
     * return the shared instance of Record Util
     *
     * @return
     */
    public static RecordUtil sharedInstance() {
        if (sRecordUtil != null) {
            return sRecordUtil;
        } else {
            throw new RuntimeException("RecordUtil must be initialized in the Application class before use");
        }
    }

    /**
     * The constructor for RecordUtil, called internally
     *
     * @param context
     */
    private RecordUtil(final Context context) {
        mContext = context;
        mGson = new Gson();

        loadRecordDetails();
    }

    /**
     * Load the record details by checking the state of the devices storage
     */
    private void loadRecordDetails() {
        updateRecordCount();
        loadCurrentRecord();
    }

    /**
     * load the current (last) record details from shared preferences
     */
    private void loadCurrentRecord() {
        SharedPreferences recordPreferences = mContext.getSharedPreferences(RECORD_PREFERENCES, Context.MODE_PRIVATE);
        String currentRecordJson = recordPreferences.getString(CURRENT_RECORD_KEY, null);
        if (!TextUtils.isEmpty(currentRecordJson)) {
            mCurrentRecord = mGson.fromJson(currentRecordJson, Record.class);
        }
    }

    /**
     * Returns true if the specified record is the current record
     *
     * @param recordToCheck
     * @return
     */
    public boolean isRecordCurrent(final Record recordToCheck) {
        // if current record is null any record is not current
        if (mCurrentRecord == null) {
            return false;
        }

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HHddss");
        String curDate = simpleDateFormat.format(mCurrentRecord.creationDate);
        String checkDate = simpleDateFormat.format(recordToCheck.creationDate);
        return curDate.equals(checkDate) &&
                recordToCheck.recordName.equals(mCurrentRecord.recordName);
    }

    /**
     * Retrieves the record with the specified id
     * @param recordId
     * @return
     */
    public Record getRecordWithId(final String recordId) {
        ArrayList<Record> allRecords = getAllSavedRecords();
        for (final Record eachRecord : allRecords) {
            if (eachRecord.recordId.equals(recordId)) {
                return eachRecord;
            }
        }
        return null;
    }

    /**
     * Create a new record with the specified record name.
     *
     * @param recordName
     */
    public boolean createNewRecord(final String recordName) {
        Record newRecord = new Record();
        newRecord.recordName = recordName;
        newRecord.creationDate = new Date();
        newRecord.recordId = UUID.randomUUID().toString();
        newRecord.photoCount = 0;
        newRecord.recordSizeKB = 0;
        newRecord.uploadedSizeKB = 0;
        newRecord.fileUploadCount = 0;

        ArrayList<Record> todaysRecords = getRecordsForDate(newRecord.creationDate);
        String appendString = todaysRecords != null && todaysRecords.size() > 0 ? "_" + (todaysRecords.size() + 1) : "";
        SimpleDateFormat dateFormat = new SimpleDateFormat(FOLDER_DATE_FORMAT);
        newRecord.recordFolderName = dateFormat.format(newRecord.creationDate) + appendString;

        File baseFolder = getBaseFolder();
        if (baseFolder == null) {
            // TODO: Return an error
            return false;
        }
        File newRecordPath = new File(baseFolder.getAbsoluteFile() + "/" + newRecord.recordFolderName);
        if (!newRecordPath.mkdir()) {
            // TODO: Return an error
            return false;
        }
        mCurrentRecord = newRecord;
        System.out.println(mCurrentRecord.recordName);

        saveCurrentRecord();
        //TcpConnection.getSharedInstance().sendPhotoName(mCurrentRecord.recordName); //sends record name when creating a new record to client
        return true;
    }

    /**
     * Return the current record
     *
     * @return
     */
    public Record getCurrentRecord() {
        return mCurrentRecord;
    }

    /**
     * Returns the current record count
     *
     * @return
     */
    public int getCurrentRecordCount() {
        return mStoredRecordCount;
    }

    /**
     * Set the current record
     *
     * @param record
     */
    public void setCurrentRecord(final Record record) {
        mCurrentRecord = record;
        saveCurrentRecord();
    }

    /**
     * Retrieves and returns a list of all saved records
     *
     * @return
     */
    public ArrayList<Record> getAllSavedRecords() {
        ArrayList<Record> resultList = new ArrayList<>();

        File baseFolder = getBaseFolder();
        if (baseFolder != null) {
            String[] allRecords = baseFolder.list();
            if (allRecords == null || allRecords.length == 0) {
                return resultList;
            }

            for (String eachRecord : allRecords) {
                Record record = loadRecordFromFolderName(eachRecord);
                if (record == null) {
                    continue;
                }
                updateRecordCurrentSize(record);
                updateRecordCurrentImageCount(record);
                resultList.add(record);
            }
        }

        if (resultList.size() > 0) {
            sortRecordList(resultList);
        }
        return resultList;
    }

    /**
     * Return all the files for a record
     *
     * @param recordId
     * @return
     */
    public File[] getRecordFiles(final String recordId) {
        Record record = getRecordWithId(recordId);
        if (record != null) {
            String recordPath = getPathForRecord(record);
            File recordFolder = !TextUtils.isEmpty(recordPath) ? new File(recordPath) : null;
            return recordFolder != null ? recordFolder.listFiles() : null;
        }
        return null;
    }

    /**
     * Sorts a list of records from newest to oldest
     *
     * @param recordList
     */
    public void sortRecordList(List<Record> recordList) {
        Collections.sort(recordList, new Comparator<Record>() {
            @Override
            public int compare(Record lhs, Record rhs) {
                return rhs.creationDate.compareTo(lhs.creationDate);
            }
        });
    }

    /**
     * Delete the specified record from storage
     *
     * @param record
     */
    public void deleteRecord(final Record record) {
        if (isRecordCurrent(record)) {
            mCurrentRecord = null;
            saveCurrentRecord();
        }

        final String path = getPathForRecord(record);
        File file = new File(path);
        if (file.exists()) {
            deleteRecursive(file);
            mStoredRecordCount--;

        }
    }

    /**
     * Recursively delete the files and sub folders of a specified file/directory
     * @param fileOrDirectory
     */
    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteRecursive(child);
            }
        }

        fileOrDirectory.delete();
    }

    /**
     * return all the records for a specified date
     *
     * @param date
     * @return
     */
    public ArrayList<Record> getRecordsForDate(final Date date) {
        ArrayList<Record> resultList = new ArrayList<>();

        SimpleDateFormat dateFormat = new SimpleDateFormat(FOLDER_DATE_FORMAT);
        String folderString = dateFormat.format(date);
        File baseFolder = getBaseFolder();
        if (baseFolder != null) {
            String[] allRecords = baseFolder.list();
            if (allRecords == null || allRecords.length == 0) {
                return resultList;
            }

            for (String eachRecord : allRecords) {
                if (eachRecord.contains(folderString)) {
                    Record record = loadRecordFromFolderName(eachRecord);
                    resultList.add(record);
                }
            }
        }

        return resultList;
    }

    /**
     * updates a record with its current size in KB in storage
     */
    public void updateRecordCurrentSize(final Record record) {
        String recordPath = getPathForRecord(record);
        File recordFolder = new File(recordPath);
        if (recordFolder.exists()) {
            File[] recordFiles = recordFolder.listFiles();
            long totalSizeKB = 0;
            for (File eachRecordFile : recordFiles) {
                totalSizeKB += (eachRecordFile.length() / 1024);
            }
            record.recordSizeKB = totalSizeKB;
            record.totalSizeKB = Math.max(record.totalSizeKB, record.recordSizeKB);
        }
    }

    /**
     * updates a record with its image count from storage
     *
     * @param record
     */
    public void updateRecordCurrentImageCount(final Record record) {
        String recordPath = getPathForRecord(record);
        File recordFolder = new File(recordPath);
        if (recordFolder.exists()) {
            File[] recordFiles = recordFolder.listFiles();
            record.photoCount = recordFiles != null ? recordFiles.length - 1 : 0;
        }
    }

    /**
     * Loads a record from the record name
     *
     * @param folderName
     * @return
     */
    private Record loadRecordFromFolderName(final String folderName) {
        File baseFolder = getBaseFolder();
        if (baseFolder == null) {
            return null;
        }

        String fullPath = baseFolder.getAbsolutePath() + "/" + folderName + "/record.rec";
        File recordFile = new File(fullPath);
        if (recordFile == null || !recordFile.exists()) {
            return null;
        }
        String recordJson = readStringFromFile(recordFile);
        if (TextUtils.isEmpty(recordJson)) {
            return null;
        }

        return mGson.fromJson(recordJson, Record.class);
    }

    /**
     * Saves the specified record details to storage
     *
     * @param record
     */
    public boolean saveRecord(final Record record) {
        File baseFolder = getBaseFolder();
        if (baseFolder == null) {
            // TODO: Return an error
            return false;
        }
        File newRecordPath = new File(baseFolder.getAbsoluteFile() + "/" + record.recordFolderName);
        if (!newRecordPath.exists()) {
            return false;
        }

        String jsonString = mGson.toJson(record);
        if (!TextUtils.isEmpty(jsonString)) {
            // TODO: Write to file
            File outputFile = new File(newRecordPath.getAbsolutePath() + "/record.rec");
            if (!outputFile.exists()) {
                try {
                    if (!outputFile.createNewFile()) {
                        return false;
                    }
                } catch (IOException ioEx) {
                    return false;
                }
            }

            writeToFile(jsonString, outputFile);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Save the current record
     */
    public void saveCurrentRecord() {
        ThreadUtil.executeOnNewThread(new Runnable() {
            @Override
            public void run() {
                SharedPreferences recordPreferences = mContext.getSharedPreferences(RECORD_PREFERENCES, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = recordPreferences.edit();
                if (mCurrentRecord == null) {
                    editor.putString(CURRENT_RECORD_KEY, null);
                } else {
                    String recordJson = mGson.toJson(mCurrentRecord);
                    editor.putString(CURRENT_RECORD_KEY, recordJson);
                }
                editor.apply();

                if (mCurrentRecord != null) {
                    saveRecord(mCurrentRecord);
                }
            }
        });
    }

    /**
     * Returns true if a record for the current day exists
     *
     * @return
     */
    public boolean checkRecordExistsForToday() {
        ArrayList<Record> todaysRecords = getRecordsForDate(new Date());
        return (todaysRecords != null && todaysRecords.size() > 0);
    }

    /**
     * The record to return the path for
     *
     * @param record
     * @return
     */
    public String getPathForRecord(final Record record) {
        String baseFolder = getBaseFolder().getAbsolutePath();
        return baseFolder + "/" + record.recordFolderName;
    }

    /**
     * Updates the record count variable by counting the number of records in storage
     */
    private void updateRecordCount() {
        // Set the count to 0,  if the storage can not be accessed then the stored record count
        // will be left as 0
        mStoredRecordCount = 0;
        File rootFolder = getBaseFolder();
        if (rootFolder == null) {
            return;
        }

        // Each directory in this folder represents one record.
        File[] fileList = rootFolder.listFiles();
        if (fileList != null) {
            for (File eachFile : fileList) {
                if (eachFile.isDirectory()) {
                    mStoredRecordCount++;
                }
            }
        }
    }

    /**
     * Return the base folder for storing records
     *
     * @return
     */
    public File getBaseFolder() {
        if (Environment.getExternalStorageDirectory() == null) {
            return null;
        }
        File rootFolder = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + RECORD_STORAGE_FOLDER);
        if (!rootFolder.exists()) {
            if (!rootFolder.mkdir()) {
                return null;
            }
        }
        return rootFolder;
    }

    /**
     * Reads the text from a file and returns it as a string
     *
     * @param file
     * @return
     */
    private String readStringFromFile(final File file) {
        try {
            FileInputStream fin = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fin));
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            String result = sb.toString();
            fin.close();
            return result;
        } catch (IOException ioEx) {
            Log.e(TAG, "Error reading String from a file: " + ioEx.getLocalizedMessage());
        }
        return null;
    }

    /**
     * Writes a string to a file
     *
     * @param data
     * @param outFile
     */
    private void writeToFile(final String data, final File outFile) {
        try {
            FileOutputStream foutStream = new FileOutputStream(outFile);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(foutStream);
            outputStreamWriter.write(data);
            outputStreamWriter.close();
            foutStream.close();
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

}
