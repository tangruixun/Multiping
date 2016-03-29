package com.trx.multiping;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link PingFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link PingFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PingFragment extends Fragment {

    private static final String PREFS_IP_ADDR = "PREFS_IP_ADDR";
    private List<PingResult> resultArray;
    private PingResultsAdapter resultsAdapter;
    private Context context;
    private Activity activity;
    private FloatingActionButton fab;
    private AdView adView;

    private EditText IpStartText;
    private EditText IpEndText;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    //private String mParam1;
    //private String mParam2;

    private OnFragmentInteractionListener mListener;

    public PingFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param //param1 Parameter 1.
     * @param //param2 Parameter 2.
     * @return A new instance of fragment PingFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static PingFragment newInstance(/*String param1, String param2*/) {
        PingFragment fragment = new PingFragment();
//        Bundle args = new Bundle();
//        args.putString(ARG_PARAM1, param1);
//        args.putString(ARG_PARAM2, param2);
//        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }*/
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_ping, container, false);
        setHasOptionsMenu(true);

        adView = (AdView) rootView.findViewById(R.id.adView);
        final AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        context = rootView.getContext();
        activity = getActivity();

        resultArray = new ArrayList<>();

        final ListView listView = (ListView) rootView.findViewById(R.id.listView);
        IpStartText = (EditText) rootView.findViewById(R.id.ip_addr_start);
        IpEndText = (EditText) rootView.findViewById(R.id.ip_addr_end);
        final TextView localIpText = (TextView) rootView.findViewById(R.id.title);

        IpStartText.setText("192.168.1.95");
        IpEndText.setText("192.168.1.100");

        resultsAdapter = new PingResultsAdapter(context, resultArray);
        listView.setAdapter(resultsAdapter);

        String strLocalIp = getLocalIpAddress();
        localIpText.setText(getString(R.string.local_ip_des) + strLocalIp);

        fab = (FloatingActionButton) rootView.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // hide IME
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

                // TODO: save user input IP for next use
                // TODO: add about fragment information
                // TODO: add cancel function

                fab.setEnabled(false);
                IpStartText.setEnabled(false);
                IpEndText.setEnabled(false);

                String strIpStart = IpStartText.getText().toString();
                String strIpEnd = IpEndText.getText().toString();
                List<Long> ipRange = getRange(strIpStart, strIpEnd);
                resultArray.clear();
                resultsAdapter.notifyDataSetChanged();
                try {
                    PingTask task = new PingTask (context);
                    resultArray = task.execute(ipRange).get(5, TimeUnit.SECONDS);
                    resultsAdapter.refresh(resultArray);
                    //resultArray.add(result);
                    //nFinished++;
                } catch (InterruptedException | ExecutionException | TimeoutException e ) {
                    e.printStackTrace();
                }
                IpStartText.setEnabled(true);
                IpEndText.setEnabled(true);
                fab.setEnabled(true);
            }
        });
        return rootView;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    /**
     * Initialize the contents of the Activity's standard options menu.  You
     * should place your menu items in to <var>menu</var>.  For this method
     * to be called, you must have first called {@link #setHasOptionsMenu}.  See
     * {@link //Activity#onCreateOptionsMenu(Menu) Activity.onCreateOptionsMenu}
     * for more information.
     *
     * @param menu     The options menu in which you place your items.
     * @param inflater
     * @see #setHasOptionsMenu
     * @see #onPrepareOptionsMenu
     * @see #onOptionsItemSelected
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        activity.getMenuInflater().inflate(R.menu.main, menu);

    }

    /**
     * This hook is called whenever an item in your options menu is selected.
     * The default implementation simply returns false to have the normal
     * processing happen (calling the item's Runnable or sending a message to
     * its Handler as appropriate).  You can use this method for any items
     * for which you would like to do processing without those other
     * facilities.
     * <p/>
     * <p>Derived classes should call through to the base class for it to
     * perform the default menu handling.
     *
     * @param item The menu item that was selected.
     * @return boolean Return false to allow normal menu processing to
     * proceed, true to consume it here.
     * @see #onCreateOptionsMenu
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(activity, SettingsActivity.class);
            startActivity(settingsIntent);

            return true;
        } else if (id == R.id.action_clear) {
            resultArray.clear();
            resultsAdapter.notifyDataSetChanged();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (adView != null) {
            adView.pause();
        }

        SharedPreferences.Editor speditor = context.getSharedPreferences(PREFS_IP_ADDR, Context.MODE_PRIVATE).edit();
        speditor.clear();
        speditor.putString("startipstring", IpStartText.getText().toString());
        speditor.putString("endipstring", IpEndText.getText().toString());
        speditor.apply();

    }

    @Override
    public void onResume() {
        super.onResume();
        if (adView != null) {
            adView.resume();
        }
        SharedPreferences ipInfo = context.getSharedPreferences(PREFS_IP_ADDR, Context.MODE_PRIVATE);
        String startIP = ipInfo.getString("startipstring", "");
        String endIP = ipInfo.getString("endipstring", "");
        IpStartText.setText(startIP);
        IpEndText.setText(endIP);
    }

    @Override
    public void onDestroy() {
        if (adView != null) {
            adView.destroy();
        }
        super.onDestroy();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    // return list<Long>
    private List<Long> getRange(String strIpStart, String strIpEnd) {
        boolean validStartIp = validIP(strIpStart);
        boolean validEndIp = validIP(strIpEnd);
        List<Long> range = new ArrayList<>();

        if (validStartIp && validEndIp) {
            long ipLongStart = 0;
            long ipLongEnd = 0;
            try {
                ipLongStart = ipToLong (InetAddress.getByName(strIpStart));
                ipLongEnd = ipToLong(InetAddress.getByName(strIpEnd));

            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            if (ipLongEnd < ipLongStart) {
                Snackbar.make(getView(), getText(R.string.prom_ip_end_less_than_begin), Snackbar.LENGTH_LONG)
                        .setAction(getText(R.string.prom_dismiss), null).show();
            } else {
                for (long i = ipLongStart; i <= ipLongEnd; i++) {
                    range.add(i);
                }
            }
        }
        return range;
    }

    public static long ipToLong(InetAddress ip) {
        byte[] octets = ip.getAddress();
        long result = 0;
        for (byte octet : octets) {
            result <<= 8;
            result |= octet & 0xff;
        }
        return result;
    }

    public static boolean validIP (String ip) {
        try {
            if ( ip == null || ip.isEmpty() ) {
                return false;
            }

            String[] parts = ip.split( "\\." );
            if ( parts.length != 4 ) {
                return false;
            }

            for ( String s : parts ) {
                int i = Integer.parseInt( s );
                if ( (i < 0) || (i > 255) ) {
                    return false;
                }
            }
            return !ip.endsWith(".");

        } catch (NumberFormatException nfe) {
            return false;
        }
    }
    private static String getLocalIpAddress() {
        String sLocalIpAddress="";
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        String sIpAddress = inetAddress.getHostAddress();
                        if(sIpAddress.startsWith("fe80:")) {
                            // Ignore IPv6 Link local address
                        } else if(sIpAddress.startsWith("::127.") || sIpAddress.startsWith("::172.")) {
                            // Ignore local loopback address
                        } else {
                            sLocalIpAddress = sLocalIpAddress + " " + sIpAddress;
                        }
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e("--->", ex.toString());
        }

        return sLocalIpAddress;
    }
}
