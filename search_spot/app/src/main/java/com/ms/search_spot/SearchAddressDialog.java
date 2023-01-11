package com.ms.search_spot;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.HttpsURLConnection;

public class SearchAddressDialog extends BottomSheetDialogFragment {

    private View rootView;
    private WebView webView;
    private CircularProgressIndicator progress;
    private Listener listener = null;

    interface Listener {
        void onLocationResult(String address, double latitude, double longitude);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.BottomSheetStyle);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialogInterface;
            setupRatio(bottomSheetDialog);
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.dialog_search_address, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rootView = view;
        webView = view.findViewById(R.id.webView);
        webView.loadDataWithBaseURL(
                "http://t1.daumcdn.net/",
                daumWebHtml,
                "text/html",
                "UTF-8",
                null
        );
        initWebView(webView);

        progress = view.findViewById(R.id.progress);
    }

    public SearchAddressDialog setListener(Listener l) {
        this.listener = l;
        return this;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebView(WebView web) {
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setAppCacheEnabled(true);
        web.addJavascriptInterface(new JavaScriptInterface(), "Android");
    }

    private void handleGeoCode(String address) {
        rootView.post(new Runnable() {
            @Override
            public void run() {
                try {
                    String resultText = new GetGeoCodeTask().execute(address).get();
                    String[] geo = geojsonParser(resultText);
                    listener.onLocationResult(address, Double.parseDouble(geo[1]), Double.parseDouble(geo[0]));
                    dismiss();
                } catch (ExecutionException | InterruptedException ex) {
                    Log.d("SearchAddressDialog", "ERROR " + ex.getMessage());
                }
            }
        });
    }

    //API 문자열 결과 JSON으로 파싱하기
    public String[] geojsonParser(String jsonString) {
        String[] geo_array = new String[2]; //API 결과 중 필요한 값(x,y)

        try {
            JSONArray jarray = new JSONObject(jsonString).getJSONArray("documents");
            JSONObject jObject = jarray.getJSONObject(0).getJSONObject("road_address");

            geo_array[0] = (String) jObject.optString("x");  //x좌표
            geo_array[1] = (String) jObject.optString("y");  //y좌표
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return geo_array;
    }

    class JavaScriptInterface {

        @JavascriptInterface
        @SuppressWarnings("unused")
        public void onResult(String data) {
            try {
                JSONObject json = new JSONObject(data);
                // address -> 선택한 주소
                // jibunAddress -> 지번 주소
                // roadAddress -> 도로명 주소
                String address = json.getString("roadAddress");
                Log.d("JavaScriptInterface", "Address " + address);
                handleGeoCode(address);
            } catch (JSONException ex) {
            }
        }
    }

    public void setupRatio(BottomSheetDialog bottomSheetDialog) {
        View bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        assert bottomSheet != null;
        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
        ViewGroup.LayoutParams layoutParams = bottomSheet.getLayoutParams();
        int height = getDeviceHeight() - getNavigationBarHeight() - getStatusBarHeight();
        layoutParams.height = (int) (height * 0.9F);
        bottomSheet.setLayoutParams(layoutParams);
        behavior.setSkipCollapsed(true);
        behavior.setDraggable(false);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    private int getDeviceHeight() {
        WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return windowManager.getCurrentWindowMetrics().getBounds().height();
        } else {
            @SuppressWarnings("DEPRECATION")
            DisplayMetrics displayMetrics = new DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(displayMetrics);
            return displayMetrics.heightPixels;
        }
    }

    private int getNavigationBarHeight() {
        int id = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (id > 0) {
            return getResources().getDimensionPixelSize(id);
        } else {
            return 0;
        }
    }

    private int getStatusBarHeight() {
        int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (id > 0) {
            return getResources().getDimensionPixelSize(id);
        } else {
            return 0;
        }
    }

    //KaKao Geocode API 통해 주소 검색 결과 받기
    @SuppressLint("StaticFieldLeak")
    public class GetGeoCodeTask extends AsyncTask<String, Void, String> {
        private String receiveMsg = "";

        private final String auth = "KakaoAK " + getString(R.string.kakao_restAPI_key);
        private URL link = null;
        private HttpsURLConnection hc = null;

        GetGeoCodeTask() {
            super();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // AsyncTask 시작
            progress.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... params) {
            try {
                link = new URL("https://dapi.kakao.com/v2/local/search/address.json?query=" + URLEncoder.encode(params[0], "UTF-8")); //한글을 URL용으로 인코딩

                HttpsURLConnection.setDefaultHostnameVerifier((arg0, arg1) -> true);

                hc = (HttpsURLConnection) link.openConnection();
                hc.setRequestMethod("GET");
                hc.setRequestProperty("User-Agent", "Java-Client");   //https 호출시 user-agent 필요
                hc.setRequestProperty("X-Requested-With", "curl");
                hc.setRequestProperty("Authorization", auth);

                //String 형태로 결과 받기
                if (hc.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    InputStreamReader tmp = new InputStreamReader(hc.getInputStream(), StandardCharsets.UTF_8);
                    BufferedReader reader = new BufferedReader(tmp);
                    StringBuilder buffer = new StringBuilder();
                    String str;
                    while ((str = reader.readLine()) != null) {
                        buffer.append(str);
                    }
                    receiveMsg = buffer.toString();
                    Log.d("AddressNetwork", "Response Data " + receiveMsg);

                    reader.close();
                } else {
                    Log.d("AddressNetwork", hc.getResponseCode() + "에러");
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.d("AddressNetwork", e.getMessage());
            }

            return receiveMsg;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            progress.setVisibility(View.GONE);
        }
    }

    private static final String daumWebHtml = "<html>\n" +
            "\n" +
            "<head>\n" +
            "    <meta name=\"viewport\" content=\"width=device-width, height=device-height, initial-scale=1.0\">\n" +
            "    <style>\n" +
            "        #daum_layer {\n" +
            "            width: 100% !important;\n" +
            "            height: 100% !important;\n" +
            "        }\n" +
            "\n" +
            "        #__daum__layer_1 {\n" +
            "            width: 100% !important;\n" +
            "            height: 100% !important;\n" +
            "        }\n" +
            "    </style>\n" +
            "</head>\n" +
            "\n" +
            "<body>\n" +
            "    <script type=\"text/javascript\" src=\"http://dmaps.daum.net/map_js_init/postcode.v2.js\"></script>\n" +
            "    <script type=\"text/javascript\">\n" +
            "        var element_layer = document.getElementById('layer');\n" +
            "        daum.postcode.load(function () {\n" +
            "            new daum.Postcode({\n" +
            "                oncomplete: function (data) {\n" +
            "                    window.Android.onResult(JSON.stringify(data))\n" +
            "                }\n" +
            "            }).embed(element_layer);\n" +
            "        });\n" +
            "\n" +
            "    </script>\n" +
            "\n" +
            "</body>\n" +
            "\n" +
            "</html>\n";
}
