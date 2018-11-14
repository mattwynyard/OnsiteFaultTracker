package com.onsite.onsitefaulttracker.activity.previous;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.onsite.onsitefaulttracker.R;
import com.onsite.onsitefaulttracker.model.Record;
import com.onsite.onsitefaulttracker.util.CalculationUtil;
import com.onsite.onsitefaulttracker.util.RecordUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by hihi on 6/25/2016.
 *
 * The List adapter which displays the previous records to the user
 */
public class PreviousRecordsAdapter extends BaseAdapter {

    // The tag name for this adapter
    private static final String TAG = PreviousRecordsAdapter.class.getSimpleName();

    // The record items which will populate the records list
    private ArrayList<Record> mItems;

    // The record item listener
    private RecordItemListener mRecordItemListener;

    // A valid context context
    private Context mContext;

    /**
     * The View Holder which stores each items view
     */
    private static class ViewHolder {

        // The name text view which displays the records name
        TextView mNameTextView;

        // The date text view which displayes the records creation date
        TextView mDateTextView;

        // The text view that displayes the image count so far for a record
        TextView mImageCountTextView;

        // The text view which displays the record size in storage in KB
        TextView mRecordSizeTextView;

        // The text view which displays the total recording time
        TextView mRecordingTimeTextView;

        // The progress text view, displayes the current progress of the record
        TextView mProgressTextView;

        // More options button for each item
        FloatingActionButton mMoreOptionsButton;

        // The upload button for each item, visible if the state is ready to submit
        FloatingActionButton mUploadButton;

        // The record button to continue recording for that item
        FloatingActionButton mRecordButton;

        // The delete record button for finalized items
        FloatingActionButton mDeleteButton;

    }

    /**
     * Constructor, takes in a list of records
     *
     * @param recordList
     */
    public PreviousRecordsAdapter(final ArrayList<Record> recordList, final Context context) {
        mItems = recordList;
        mContext = context;
    }

    /**
     * Set the record item listener
     *
     * @param recordItemListener
     */
    public void setRecordItemListener(final RecordItemListener recordItemListener) {
        mRecordItemListener = recordItemListener;
    }

    /**
     * returns the number of items in the adapter
     *
     * @return
     */
    @Override
    public int getCount() {
        return mItems != null ? mItems.size() : 0;
    }

    /**
     * returns the item at the specified position
     *
     * @param position
     * @return
     */
    @Override
    public Object getItem(int position)
    {
        return mItems.get(position);
    }

    /**
     * returns an items position as its id.
     *
     * @param position
     * @return
     */
    @Override
    public long getItemId(int position)
    {
        return position;
    }

    /**
     * Initializes the list item view that will then be displayed in the list.
     *
     * @param position
     * @param convertView
     * @param parent
     * @return
     */
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View vi = convertView;
        final ViewHolder holder;

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            vi = inflater.inflate(R.layout.record_list_item, parent, false);

            holder = new ViewHolder();
            holder.mNameTextView = (TextView) vi.findViewById(R.id.record_name_text_view);
            holder.mDateTextView = (TextView) vi.findViewById(R.id.record_date_text_view);
            holder.mImageCountTextView = (TextView) vi.findViewById(R.id.image_count_text_view);
            holder.mRecordSizeTextView = (TextView) vi.findViewById(R.id.record_size_text_view);
            holder.mRecordingTimeTextView = (TextView) vi.findViewById(R.id.recording_time_text_view);
            holder.mProgressTextView = (TextView) vi.findViewById(R.id.progress_text_view);
            holder.mRecordButton = (FloatingActionButton) vi.findViewById(R.id.record_button);
            holder.mUploadButton = (FloatingActionButton) vi.findViewById(R.id.upload_button);
            holder.mDeleteButton = (FloatingActionButton) vi.findViewById(R.id.delete_button);
            holder.mMoreOptionsButton = (FloatingActionButton) vi.findViewById(R.id.more_button);

            holder.mRecordButton.setBackgroundTintList(new ColorStateList(new int[][]{new int[]{0}}, new int[]{ContextCompat.getColor(mContext, R.color.record_red)}));
            holder.mUploadButton.setBackgroundTintList(new ColorStateList(new int[][]{new int[]{0}}, new int[]{ContextCompat.getColor(mContext, R.color.submit_blue)}));
            holder.mDeleteButton.setBackgroundTintList(new ColorStateList(new int[][]{new int[]{0}}, new int[]{ContextCompat.getColor(mContext, R.color.record_red)}));

            vi.setTag(holder);
        } else {
            holder = (ViewHolder) vi.getTag();
        }

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd MMMM yyyy, h:mm a");
        final Record item = (Record) getItem(position);
        Calendar now = Calendar.getInstance();
        Calendar recordCalendar = Calendar.getInstance();
        recordCalendar.setTime(item.creationDate);
        boolean isToday = now.get(Calendar.DAY_OF_YEAR) == recordCalendar.get(Calendar.DAY_OF_YEAR)
                && now.get(Calendar.YEAR) == recordCalendar.get(Calendar.YEAR);
        boolean isYesterday = now.get(Calendar.DAY_OF_YEAR) - 1 == recordCalendar.get(Calendar.DAY_OF_YEAR)
                && now.get(Calendar.YEAR) == recordCalendar.get(Calendar.YEAR);
        String prefixString = isToday ? "(Today) " :
                isYesterday ? "(Yesterday)" : "";

        holder.mNameTextView.setText(item.recordName);
        holder.mDateTextView.setText(prefixString +
                simpleDateFormat.format(item.creationDate));
        holder.mImageCountTextView.setText(String.format("%d images", item.photoCount));
        String sizeString = CalculationUtil.sharedInstance().getDisplayValueFromKB(item.recordSizeKB) + " in storage";
        holder.mRecordSizeTextView.setText(sizeString);
        String recordingTimeString = CalculationUtil.sharedInstance().getDisplayValueFromMilliseconds(item.totalRecordTime) + " recorded";
        holder.mRecordingTimeTextView.setText(recordingTimeString);

        if (item.fileUploadCount >= item.photoCount) {
            // Item Finished
            holder.mProgressTextView.setText(mContext.getString(R.string.record_progress_finalized));
            holder.mProgressTextView.setTextColor(ContextCompat.getColor(mContext, R.color.finalized_red));
            holder.mUploadButton.setVisibility(View.INVISIBLE);
            holder.mRecordButton.setVisibility(View.INVISIBLE);
            holder.mDeleteButton.setVisibility(View.VISIBLE);
        } else if (RecordUtil.sharedInstance().isRecordCurrent(item)) {
            // Current Item and not finished
            holder.mProgressTextView.setText(mContext.getString(R.string.record_progress_in_progress));
            holder.mProgressTextView.setTextColor(ContextCompat.getColor(mContext, R.color.in_progress_green));
            holder.mUploadButton.setVisibility(View.INVISIBLE);
            holder.mRecordButton.setVisibility(View.VISIBLE);
            holder.mDeleteButton.setVisibility(View.INVISIBLE);
        } else {
            // Not Current, Ready to submit
            holder.mProgressTextView.setText(mContext.getString(R.string.record_progress_ready_to_submit));
            holder.mProgressTextView.setTextColor(ContextCompat.getColor(mContext, R.color.ready_to_submit_blue));
            holder.mUploadButton.setVisibility(View.VISIBLE);
            holder.mRecordButton.setVisibility(View.INVISIBLE);
            holder.mDeleteButton.setVisibility(View.INVISIBLE);
        }

        holder.mRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRecordItemListener != null) {
                    mRecordItemListener.onRecordButtonClicked(item);
                }
            }
        });
        holder.mUploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRecordItemListener != null) {
                    mRecordItemListener.onUploadButtonClicked(item);
                }
            }
        });
        holder.mDeleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRecordItemListener != null) {
                    mRecordItemListener.onDeleteButtonClicked(item);
                }
            }
        });
        holder.mMoreOptionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRecordItemListener != null) {
                    mRecordItemListener.onMoreButtonClicked(item);
                }
            }
        });

        return vi;
    }

    /**
     *  Listener for communicating button clicks to the parent fragment
     */
    public interface RecordItemListener {
        void onRecordButtonClicked(final Record recordItem);
        void onUploadButtonClicked(final Record recordItem);
        void onDeleteButtonClicked(final Record recordItem);
        void onMoreButtonClicked(final Record recordItem);
    }

}
