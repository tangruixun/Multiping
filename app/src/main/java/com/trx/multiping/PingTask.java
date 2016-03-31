package com.trx.multiping;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

class PingTask extends AsyncTask<List<Long> , Integer, List<PingResult>> {

    private final WeakReference<Context> contextReference;
    static final int TIMEOUT = 3000;
    private int nListSize = 0;
    private ProgressDialog progressDlg = null;
    private Context context;
    private SharedPreferences sharedPreferences;
    private PingTask myTask = this;
    private boolean bMethod;
    private boolean bBeep;
    private TextView StatText;

    public PingTask(Context c) {
        contextReference = new WeakReference<>(c);
        context = contextReference.get();

        myTask = this;
        StatText = (TextView) ((Activity) context).findViewById(R.id.stats);
        progressDlg = new ProgressDialog(context);
        progressDlg.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDlg.setIndeterminate(false);
        progressDlg.setCancelable(true);
        progressDlg.setCanceledOnTouchOutside(false);
        progressDlg.setProgressNumberFormat(null);
        progressDlg.setButton(DialogInterface.BUTTON_NEGATIVE, context.getString (R.string.cancel), new DialogInterface.OnClickListener() {

                    /**
                     * This method will be invoked when a button in the dialog is clicked.
                     *
                     * @param dialog The dialog that received the click.
                     * @param which  The button that was clicked (e.g.
                     *               {@link DialogInterface#BUTTON1}) or the position
                     */
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (myTask!=null) {
                            myTask.cancel(true);
                            cancel(true);
                        }
                    }
                });

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        bMethod = sharedPreferences.getBoolean(context.getString(R.string.pref_scan_method_key), false);
        bBeep = sharedPreferences.getBoolean(context.getString(R.string.pref_sound_key), false);
    }

    /**
     * Override this method to perform a computation on a background thread. The
     * specified parameters are the parameters passed to {@link #execute}
     * by the caller of this task.
     * <p/>
     * This method can call {@link #publishProgress} to publish updates
     * on the UI thread.
     *
     * @param params The parameters of the task.
     * @return A result, defined by the subclass of this task.
     * @see #onPreExecute()
     * @see #onPostExecute
     * @see #publishProgress
     */
    @SafeVarargs
    @Override
    protected final List<PingResult> doInBackground(List<Long>... params) {
    List<PingResult> resultList = new ArrayList<>();
        Log.i("--->", params[0] + "");
        nListSize = params[0].size();

        progressDlg.setMax(nListSize);

        InetAddress ip;
        int i = 0;
        for (long longip: params[0]) {
            if (myTask.isCancelled()) {
                break;
            }
            publishProgress(0, i);
            try {
                ip = longToIp(longip);
                progressDlg.setMessage(context.getString(R.string.pinging) + ip);
                PingResult result;
                if (!bMethod) {
                    result = pingHost (ip);
                } else {
                    result = startPing(ip);
                }
                resultList.add(result);
                i++;
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            publishProgress(100, i);
        }
        return resultList;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        progressDlg.setProgress(0);
        progressDlg.show();
        // prevent screen sleep
        ((Activity) context).getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        final int i = values [0];
        final int j = values [1];
        progressDlg.setProgress(j);
        progressDlg.setSecondaryProgress(i);
    }

    /**
     * <p>Runs on the UI thread after {@link #doInBackground}. The
     * specified result is the value returned by {@link #doInBackground}.</p>
     * <p/>
     * <p>This method won't be invoked if the task was cancelled.</p>
     *
     * @param pingResults The result of the operation computed by {@link #doInBackground}.
     * @see #onPreExecute
     * @see #doInBackground
     * @see #onCancelled(Object)
     */
    @Override
    protected void onPostExecute(List<PingResult> pingResults) {
        super.onPostExecute(pingResults);
        ((Activity) context).getWindow().clearFlags (WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        progressDlg.dismiss();
        StatText.setText(StatisticResultInfo (pingResults));
    }

    private String StatisticResultInfo(List<PingResult> resultArray) {
        List<PingResult> reachableArray = new ArrayList<>();
        String stats = "";
        double lostPrecents = 0;
        int n = 0; // reachable number
        int size = resultArray.size();
        long reachableTime = 0;
        long totalTime = 0, totalReachableTime = 0;
        long minTime = 0;
        long maxTime = 0;

        for (PingResult resultItem: resultArray) {
            if (resultItem.isReachable()) {
                n++;
                reachableArray.add (resultItem);
                totalReachableTime += resultItem.getEchoTime();
            }
            totalTime += resultItem.getEchoTime();
        }

        reachableArray = quickSort (reachableArray, 0, reachableArray.size());

        if (size > 0) {
            lostPrecents = (1.0 - n / size) * 100;
        }
        if (n > 0) {
            reachableTime = totalReachableTime / n;
        }

        minTime = reachableArray.get(0).getEchoTime();
        maxTime = reachableArray.get(reachableArray.size()).getEchoTime();
        stats = "Total=" + size
                + "Reachable=" + n
                + ", Lost=" + lostPrecents
                + "; Average Time=" + reachableTime
                + ", Min Time=" + minTime
                + ", Max Time=" + maxTime
                + ", Total Time=" + totalTime;
        return stats;
    }

    private List<PingResult> quickSort (List<PingResult> array, int left, int right) {
        int i = left;
        int j = right;
        long pivotValue = array.get(i).getEchoTime();
        PingResult pivotItem = array.get(i);

        while (i<j) {
            while (array.get(j).getEchoTime() > pivotValue && i<j) {
                j--;
            }
            while (array.get(i).getEchoTime() < pivotValue && i<j) {
                i++;
            }

            //swap
            PingResult temp = array.get(j);
            array.set(j, array.get(i));
            array.set(i, temp);
        }
        array.set(left, array.get(i));
        array.set(i, pivotItem);
        quickSort(array, left, i-1);
        quickSort(array, i+1, right);
        return array;
    }

    /**
     * <p>Runs on the UI thread after {@link #cancel(boolean)} is invoked and
     * {@link #doInBackground(Object[])} has finished.</p>
     * <p/>
     * <p>The default implementation simply invokes {@link #onCancelled()} and
     * ignores the result. If you write your own implementation, do not call
     * <code>super.onCancelled(result)</code>.</p>
     *
     * @param pingResults The result, if any, computed in
     *                    {@link #doInBackground(Object[])}, can be null
     * @see #cancel(boolean)
     * @see #isCancelled()
     */
    @Override
    protected void onCancelled(List<PingResult> pingResults) {
        super.onCancelled(pingResults);
    }

    /**
     * <p>Applications should preferably override {@link #onCancelled(Object)}.
     * This method is invoked by the default implementation of
     * {@link #onCancelled(Object)}.</p>
     * <p/>
     * <p>Runs on the UI thread after {@link #cancel(boolean)} is invoked and
     * {@link #doInBackground(Object[])} has finished.</p>
     *
     * @see #onCancelled(Object)
     * @see #cancel(boolean)
     * @see #isCancelled()
     */
    @Override
    protected void onCancelled() {
        super.onCancelled();
    }

    // use isReachable instead of ping command
    private PingResult startPing(InetAddress remoteIp) {
        PingResult result = new PingResult();
        long dt = -1;
        long t1 = System.nanoTime();
        result.setRemoteIP(remoteIp);
        result.setReachable(false);

        try {
            if(remoteIp.isReachable(TIMEOUT)) {
                long t2 = System.nanoTime();
                dt = (t2-t1)/1000000; // nano to ms
                result.setReachable(true);
                if (bBeep) {
                    makeBeep(0);
                }
            } else {
                result.setReachable(false);
                if (bBeep) {
                    makeBeep(1);
                }
            }
            result.setEchoTime(dt);
        } catch (IOException e) {
            Log.v("--->","PingerAv " + e.toString());
        }
        return result;
    }


    /**
     * Ping a host and return an int value of 0 or 1 or 2 0=success, 1=fail,
     * 2=error
     *
     * Does not work in Android emulator and also delay by '1' second if host
     * not pingable In the Android emulator only ping to 127.0.0.1 works
     *
     * param //String host in dotted IP address format
     * !return PingResultArray
     */
    public PingResult pingHost(InetAddress host) {
        PingResult result = new PingResult();
        result.setRemoteIP(host);
        result.setReachable(false);

        String strHost = host.getHostAddress();
        long dt = -1;
        long t1 = System.nanoTime();
        long t2;
        int timeout;
        Runtime runtime = Runtime.getRuntime();
        timeout = TIMEOUT / 1000;

        try {
            String cmd = "ping -c 1 -W " + timeout + " " + strHost;
            Process proc = runtime.exec(cmd);
            Log.d("--->", cmd);
            proc.waitFor();
            t2 = System.nanoTime();
            int exit = proc.exitValue();
            if (exit == 0) {
                result.setReachable(true);
                dt = (t2-t1)/1000000; // nano to ms
                if (bBeep) {
                    makeBeep(0);
                }
            } else {
                if (bBeep) {
                    makeBeep(1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        result.setEchoTime(dt);
        return result;
    }

    private InetAddress getRemoteIpAddress(String hostname) {
        Log.v("--->","NameResolver "+hostname);
        InetAddress ia = null;
        try {
            ia = InetAddress.getByName(hostname);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ia;
    }

    public static InetAddress longToIp (long in) throws UnknownHostException {
        byte[] bytes = {
                (byte)((in >>> 24) & 0xff),
                (byte)((in >>> 16) & 0xff),
                (byte)((in >>>  8) & 0xff),
                (byte)((in       ) & 0xff)};
        return InetAddress.getByAddress(bytes);
    }

    public static void makeBeep (int type) {
        final ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
        if (type == 0) {
            toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 300);
        } else {
            toneG.startTone(ToneGenerator.TONE_PROP_BEEP, 100);
        }
        toneG.release();
    }
}