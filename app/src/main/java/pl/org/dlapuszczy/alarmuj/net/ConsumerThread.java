package pl.org.dlapuszczy.alarmuj.net;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.android.volley.Request.Method;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import pl.org.dlapuszczy.alarmuj.db.Report;
import pl.org.dlapuszczy.alarmuj.db.ReportsDbHelper;

/**
 * Created by ohaleck on 02/08/2017.
 */

class ConsumerThread extends Thread {

    private static final String REQUEST_TAG = "tag";
    private static final String HOST = "http://localhost";
    private final Context context;
    private final ReportsDbHelper dbHelper;

    public ConsumerThread(Context context) {
        this.context = context;
        this.dbHelper = ReportsDbHelper.getInstance(context);
    }

    @Override
    public void run() {
        if (dbHelper.isReportPending()) {
            Cursor pendingReports = dbHelper.getPendingReports();
            HttpClient.getInstance(context).addRequest(createRequest(pendingReports), REQUEST_TAG);
        }
        // TODO notify service that the work is done
    }

    private JsonRequest createRequest(final Cursor pendingReports) {
        JSONArray body = createJsonArray(pendingReports);
        JsonArrayRequestWithStatus request = new JsonArrayRequestWithStatus(Method.POST, getCreateReportUrl(), body, new Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                markAsSent(pendingReports, System.currentTimeMillis());
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // TODO implement retrying
            }
        });
        return request;
    }

    private void markAsSent(Cursor cursor, long timeSent) {
        int rowCount = cursor.getCount();
        String[] ids = new String[rowCount];
        int i = 0;
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            ids[i] = String.valueOf(cursor.getInt(cursor.getColumnIndex(Report.ID)));
            i++;
        }
        ContentValues contentValues = new ContentValues();
        contentValues.put(Report._ID, timeSent);
        dbHelper.getWritableDatabase().update(
                Report.TABLE_NAME,
                contentValues,
                Report.ID + " IN (" + makePlaceholders(rowCount) + ")",
                ids);

    }

    /**
     * @param count
     * @return a string composed of count question marks separated with commas, e.g. 3 yields "?, ?, ?".
     */
    private String makePlaceholders(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append('?');
            if (i + 1 < count) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    private JSONArray createJsonArray(Cursor cursor) {
        JSONArray jsonArray = new JSONArray();
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            jsonArray.put(Report.fromCursor(cursor).toMap());
        }
        return jsonArray;
    }

    public String getCreateReportUrl() {
        return HOST + "/reports";
    }
}
