package com.trx.multiping;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class PingResultsAdapter extends BaseAdapter {

    private TagView tag;
    private List<PingResult> _results = new ArrayList<>();
    private LayoutInflater mAdapterInflater;
    Context _content;


    public PingResultsAdapter(Context content, List<PingResult> results) {
        _content = content;
        _results = results;
        mAdapterInflater = LayoutInflater.from(_content);
    }

    /**
     * How many items are in the data set represented by this Adapter.
     *
     * @return Count of items.
     */
    @Override
    public int getCount() {

        return _results.size ();
    }

    /**
     * Get the data item associated with the specified position in the data set.
     *
     * @param position Position of the item whose data we want within the adapter's
     *                 data set.
     * @return The data at the specified position.
     */
    @Override
    public PingResult getItem(int position) {

        return _results.get(position);
    }

    /**
     * Get the row id associated with the specified position in the list.
     *
     * @param position The position of the item within the adapter's data set whose row id we want.
     * @return The id of the item at the specified position.
     */
    @Override
    public long getItemId(int position) {
        return 0;
    }

    /**
     * Get a View that displays the data at the specified position in the data set. You can either
     * create a View manually or inflate it from an XML layout file. When the View is inflated, the
     * parent View (GridView, ListView...) will apply default layout parameters unless you use
     * {@link LayoutInflater#inflate(int, ViewGroup, boolean)}
     * to specify a root view and to prevent attachment to the root.
     *
     * @param position    The position of the item within the adapter's data set of the item whose view
     *                    we want.
     * @param convertView The old view to reuse, if possible. Note: You should check that this view
     *                    is non-null and of an appropriate type before using. If it is not possible to convert
     *                    this view to display the correct data, this method can create a new view.
     *                    Heterogeneous lists can specify their number of view types, so that this View is
     *                    always of the right type (see {@link #getViewTypeCount()} and
     *                    {@link #getItemViewType(int)}).
     * @param parent      The parent that this view will eventually be attached to
     * @return A View corresponding to the data at the specified position.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        PingResult resultItem = getItem(position);
        Log.i ("--->", position + "");
        if (convertView == null) {
            convertView = mAdapterInflater.inflate(R.layout.result_item, parent, false);
            tag = new TagView(convertView);
            convertView.setTag(tag);
        } else {
            tag = (TagView) convertView.getTag();
        }
        /*
        if (resultItem.isReachable()) {
            tag.ll.setVisibility(View.VISIBLE);
            tag.tvRemoteIP.setText(resultItem.getRemoteIP().getHostAddress());
            tag.tvEchoTime.setText(String.valueOf(resultItem.getEchoTime()) + "ms");
            convertView.setVisibility(View.VISIBLE);
        } else {
            tag.ll.setVisibility(View.GONE);
            tag.tvRemoteIP.setVisibility(View.GONE);
            tag.tvEchoTime.setVisibility(View.GONE);
            convertView.setVisibility(View.GONE);
        }*/
        tag.tvRemoteIP.setText(resultItem.getRemoteIP().getHostAddress());
        tag.tvEchoTime.setText(String.valueOf(resultItem.getEchoTime()) + "ms");
        return convertView;
    }

    public void refresh (List<PingResult> results) {
        _results = results;
        notifyDataSetChanged();
    }

    private class TagView {

        TextView tvRemoteIP, tvEchoTime;
        ImageView ivIcon;
        LinearLayout ll;

        public TagView(View item) {
            tvRemoteIP = (TextView) item.findViewById(R.id.tvRemoteIP);
            tvEchoTime = (TextView) item.findViewById(R.id.tvEchoTime);
            ivIcon = (ImageView) item.findViewById(R.id.ivIcon);
            ll = (LinearLayout) item.findViewById(R.id.itemlinearlayout);
        }
    }
}
