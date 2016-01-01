package babymonitor.app;

import android.content.Context;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class SendRequest {

    private static final String TAG = "SendRequest";
    private static final String URL = "https://gcm-http.googleapis.com/gcm/send";

    public static boolean send(String topic, String message, Context context) {


        Log.d(TAG,"Message is :" + message);

        RequestQueue requestQueue = Volley.newRequestQueue(context);

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("to", topic);
            JSONObject data = new JSONObject();
            data.put("message", message);
            jsonObject.put("data", data);
        } catch (JSONException e) {
            return false;
        }

        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, URL, jsonObject, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d(TAG, "The request has been sent !!!");
                Log.d(TAG, response.toString());
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "The request has been sent !!!");
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Content-Type", "application/json");
                params.put("Authorization", "key=" + "AIzaSyCyFcvyxHcvC4mZ_xfEBwKfkc_edyr1NCw");

                return params;
            }
        };

        requestQueue.add(postRequest);

        return true;
    }

}