package pl.org.dlapuszczy.alarmuj.net;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

/**
 * Created by ohaleck on 03/08/2017.
 */

class JsonArrayRequestWithStatus extends JsonRequest<JSONObject> {
    public static final String STATUS_META_KEY = "_status";

    public JsonArrayRequestWithStatus(int method, String url, JSONArray requestBody, Listener<JSONObject> listener, ErrorListener errorListener) {
        super(method, url, requestBody == null ? null : requestBody.toString(), listener, errorListener);
    }

    protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
        try {
            String jsonString = new String(response.data,
                    HttpHeaderParser.parseCharset(response.headers, PROTOCOL_CHARSET));
            JSONObject jsonObject = new JSONObject(jsonString);
            jsonObject.put(STATUS_META_KEY, response.statusCode);
            return Response.success(jsonObject, HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (JSONException je) {
            return Response.error(new ParseError(je));
        }
    }
}
