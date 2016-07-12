package kr.ogong.gibiyo.gibiyoforuser;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.onesignal.OneSignal;


public class MainActivity extends AppCompatActivity {
    private WebView webview;

    private double  Lat = 0.0;
    private double  Lng = 0.0;

    private String  OsId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        webview = (WebView) findViewById(R.id.webView);
        webview.setWebChromeClient(new WebChrome()); // 응룡프로그램에서 직접 url 처리
        webview.setWebViewClient(new WebClient()); // 응룡프로그램에서 직접 url 처리


        String userAgent = webview.getSettings().getUserAgentString();
        webview.getSettings().setUserAgentString(userAgent + " APP_GIBIYO_Android");
        webview.addJavascriptInterface(new AndroidBridge(), "GibiyoApp");

        WebSettings set = webview.getSettings();
        set.setJavaScriptEnabled(true);
        set.setBuiltInZoomControls(false);

        webview.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.e("gibiyu4user", consoleMessage.message() + '\n' + consoleMessage.messageLevel() + '\n' + consoleMessage.sourceId());
                return super.onConsoleMessage(consoleMessage);
            }
        });

        webview.loadUrl("http://gibiyo-ogong.rhcloud.com/app/house/");

        // OneSignal start.... hehe
        OneSignal.startInit(this).init();

        findLocation();

        /*
         아래는 OneSignal의 아이디를 가져오는 부분으로 OsId에 그 값을 세팅한다
         단말기별로 고유의 아이디를 기지며 이는 어플을 삭제해도 유지함
        */
        OneSignal.idsAvailable(new OneSignal.IdsAvailableHandler() {
            @Override
            public void idsAvailable(String userId, String registrationId) {
                OsId = userId;
                Log.d("debug", ">>>> User:" + userId);
                if (registrationId != null)
                    Log.d("debug", "registrationId:" + registrationId);
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (webview == null) return;

        webview.goBack();
    }

    /**
     * GPS로 위치 정보 가져오기
     */
    public void findLocation() {
        // Acquire a reference to the system Location Manager
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // GPS 프로바이더 사용가능여부
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        // 네트워크 프로바이더 사용가능여부
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        Log.d("Main", "isGPSEnabled=" + isGPSEnabled);
        Log.d("Main", "isNetworkEnabled=" + isNetworkEnabled);

        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                Lat = location.getLatitude();
                Lng = location.getLongitude();

                Log.d("location", "latitude: " + Lat + ", longitude: " + Lng);
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
                Log.d("location", "onStatusChanged");
            }

            public void onProviderEnabled(String provider) {
                Log.d("location", "onProviderEnabled");
            }

            public void onProviderDisabled(String provider) {
                Log.d("location", "onProviderDisabled");
            }
        };

        // Register the listener with the Location Manager to receive location updates
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        if ( isGPSEnabled ) locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        if ( isNetworkEnabled ) locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);

        // 수동으로 위치 구하기
        String locationProvider = LocationManager.GPS_PROVIDER;
        Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
        if (lastKnownLocation != null) {
            Lng = lastKnownLocation.getLongitude();
            Lat = lastKnownLocation.getLatitude();
            Log.d("Main>>manual", "longtitude=" + Lng + ", latitude=" + Lat);
        }
    }

    class WebClient extends WebViewClient {
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.startsWith("tel:")) {
                //tel:01000000000
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse(url));
                startActivity(intent);
                return true;
            }else if (url.startsWith("mailto:")) {
                //mailto:ironnip@test.com
                Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse(url));
                startActivity(i);
                return true;
            } else
                view.loadUrl(url);
            return true;
        }
    }


    class WebChrome extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            /*
            //현제 페이지 진행사항을 ProgressBar를 통해 알린다.
            if(newProgress < 100) {
                mProgressBar.setProgress(newProgress);
            } else {
                mProgressBar.setVisibility(View.INVISIBLE);
                mProgressBar.setLayoutParams(new LinearLayout.LayoutParams(0, 0));
            }
            */
        }
    }


    private final Handler handler = new Handler();

    private class AndroidBridge {
        @JavascriptInterface
        public void requestGeo(final String arg) { // must be final
            handler.post(new Runnable() {
                @Override
                public void run() {
                    // 원하는 동작
                    webview.loadUrl( "javascript:setGeoFromApp(" +  Lat + "," + Lng + ")");
                }
            });
        }

        @JavascriptInterface
        public void requestOsId(final String arg) { // must be final
            handler.post(new Runnable() {
                @Override
                public void run() {
                    // 원하는 동작
                    webview.loadUrl( "javascript:setOsIdFromApp('" + OsId + "')");
                }
            });
        }
    }
}
