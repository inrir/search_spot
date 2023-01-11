package com.ms.search_spot;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import net.daum.mf.map.api.MapCircle;
import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapPolyline;
import net.daum.mf.map.api.MapReverseGeoCoder;
import net.daum.mf.map.api.MapView;

import java.util.ArrayList;

// 카카오 API를 이용하여 카카오 지도에 친구들의 위치를 파란색 마커로 중간지점을 빨간색 마커로 표시하는 엑티비티.
public class FindLocationActivity extends AppCompatActivity implements MapView.POIItemEventListener, MapReverseGeoCoder.ReverseGeoCodingResultListener {

    private GeoDataClient mGeoDataClient;
    private MapView mapView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_location);
        //LatLng 객체 꺼내기
        Intent intent = getIntent();
        ArrayList<LatLng> arr = (ArrayList<LatLng>) intent.getSerializableExtra("arr");
        Log.d("Location","LatLng Arr " + arr);
        mGeoDataClient = Places.getGeoDataClient(this, null);

        mapView = new MapView(this);
        ViewGroup mapViewContainer = (ViewGroup) findViewById(R.id.map_view);
        mapViewContainer.addView(mapView);
        // 친구의 수 만큼 반복하면서 지도에 마커를 표시.
        mapView.setPOIItemEventListener(this);
        if (arr != null) {
            int i = 0;
            for (LatLng point : arr) {
                MapPOIItem marker = new MapPOIItem();
                marker.setItemName("모임원" + i);
                marker.setTag(i + 1);
                marker.setMapPoint(MapPoint.mapPointWithGeoCoord(point.latitude, point.longitude));
                marker.setMarkerType(MapPOIItem.MarkerType.BluePin); // 기본으로 제공하는 BluePin 마커 모양.
                marker.setSelectedMarkerType(MapPOIItem.MarkerType.BluePin); // 마커를 클릭했을때, 기본으로 제공하는 RedPin 마커 모양.
                marker.setShowAnimationType(MapPOIItem.ShowAnimationType.DropFromHeaven);
                mapView.addPOIItem(marker);
                i++;
            }
            /* 거리 계산 알고리즘 */
            //중간점 계산 다각형의 무게중심.
            double sum_lat = 0;
            double sum_lng = 0;
            //중간지점 계산

            System.out.println(arr.size()); // 전체 거리
            for (LatLng point : arr) {
                sum_lat += point.latitude;
                sum_lng += point.longitude;
            }
            sum_lat = sum_lat / arr.size();
            sum_lng = sum_lng / arr.size();

            if(arr.size() > 3){ // 중심점이 3이상일 경우 다각형 무게중심 계산
                double area = 0; // 넓이
                sum_lat = 0; // 초기화
                sum_lng = 0;

                for(int a = 0; a<arr.size(); a++){
                    int b = (a+1) % arr.size();

                    LatLng pt1 = arr.get(a);
                    LatLng pt2 = arr.get(b);

                    double x1 = pt1.latitude;
                    double y1 = pt1.longitude;
                    double x2 = pt2.latitude;
                    double y2 = pt2.longitude;

                    area += x1 * y2;
                    area -= y1 * x2;

                    sum_lat += ((x1 + x2) * ((x1 * y2) - (x2 * y1)));
                    sum_lng += ((y1 + y2) * ((x1 * y2) - (x2 * y1)));

                }

                area /= 2;
                area = Math.abs(area);

                sum_lat = sum_lat / (6.0 * area);
                sum_lng = sum_lng / (6.0 * area);

                sum_lat = Math.abs(sum_lat);
                sum_lng = Math.abs(sum_lng);
            }

            //중간지점으로 이동
            mapView.setMapCenterPointAndZoomLevel(MapPoint.mapPointWithGeoCoord(sum_lat, sum_lng), 6, true);

            //중간지점 마커추가
            MapPOIItem marker_center = new MapPOIItem();
            marker_center.setItemName("모임지점 주변장소 선택");
            marker_center.setTag(0);
            marker_center.setMapPoint(MapPoint.mapPointWithGeoCoord(sum_lat, sum_lng));
            marker_center.setMarkerType(MapPOIItem.MarkerType.RedPin); // 기본으로 제공하는 BluePin 마커 모양.
            marker_center.setShowAnimationType(MapPOIItem.ShowAnimationType.SpringFromGround);
            mapView.addPOIItem(marker_center);
            //중간지점에 원 그리기
            MapCircle circle1 = new MapCircle(
                    MapPoint.mapPointWithGeoCoord(sum_lat, sum_lng), // center
                    500, // radius
                    Color.argb(128, 255, 0, 0), // strokeColor
                    Color.argb(128, 0, 255, 0) // fillColor
            );
            circle1.setTag(1234);
            mapView.addCircle(circle1);

            //폴리라인 그리기
            for (LatLng point : arr) {
                MapPolyline polyline = new MapPolyline();
                polyline.setTag(1000);
                polyline.setLineColor(Color.argb(128, 255, 51, 0)); // Polyline 컬러 지정.
                // Polyline 좌표 지정.
                polyline.addPoint(MapPoint.mapPointWithGeoCoord(point.latitude, point.longitude));
                polyline.addPoint(MapPoint.mapPointWithGeoCoord(sum_lat, sum_lng));

                // Polyline 지도에 올리기.
                mapView.addPolyline(polyline);
            }
        }
    }

    @Override
    public void onPOIItemSelected(MapView mapView, MapPOIItem mapPOIItem) {
//        MapReverseGeoCoder reverseGeoCoder = new MapReverseGeoCoder(getString(R.string.kakao_app_key), mapPOIItem.getMapPoint(), this, this);
//        reverseGeoCoder.startFindingAddress();
    }

    @Override
    public void onCalloutBalloonOfPOIItemTouched(MapView mapView, MapPOIItem mapPOIItem) {

    }

    // 중간지점 마커를 선택하면 구글 PlacePicker를 실행 => 주변 맛집 검색으로 변경해야 한다.
    @Override
    public void onCalloutBalloonOfPOIItemTouched(MapView mapView, MapPOIItem mapPOIItem, MapPOIItem.CalloutBalloonButtonType calloutBalloonButtonType) {
        PlacePicker.IntentBuilder builder;
        builder = new PlacePicker.IntentBuilder();
        LatLng latLng = new LatLng(mapPOIItem.getMapPoint().getMapPointGeoCoord().latitude, mapPOIItem.getMapPoint().getMapPointGeoCoord().longitude);
        LatLng latLng1 = new LatLng(latLng.latitude - 0.01, latLng.longitude - 0.01);
        LatLng latLng2 = new LatLng(latLng.latitude + 0.01, latLng.longitude + 0.01);

        try {
            builder.setLatLngBounds(new LatLngBounds(latLng1, latLng2));
            startActivityForResult(builder.build(FindLocationActivity.this), 2);
        } catch (GooglePlayServicesRepairableException e) {
            e.printStackTrace();
        } catch (GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDraggablePOIItemMoved(MapView mapView, MapPOIItem mapPOIItem, MapPoint mapPoint) {

    }

    @Override
    public void onReverseGeoCoderFoundAddress(MapReverseGeoCoder mapReverseGeoCoder, String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onReverseGeoCoderFailedToFindAddress(MapReverseGeoCoder mapReverseGeoCoder) {
        Toast.makeText(this, "주소를 찾을 수 없습니다.", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 2) {
            if (resultCode == RESULT_OK) {
                Place place = PlacePicker.getPlace(data, this);
                String address = place.getAddress().toString();
                String name = place.getName().toString();
                String id = place.getId();
//                // kakaomap://search?q=%EB%A7%9B%EC%A7%91&p=37.537229,127.005515
//                String url = "kakaomap://search?q=맛집&p="+place.getLatLng().latitude+","+place.getLatLng().longitude;
//
//                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
//                startActivity(intent);

                /******************** 추가 *****************************************************/
//                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://search.naver.com/search.naver?where=nexearch&sm=top_hty&fbm=1&ie=utf8&query="+address+"+"+name)));
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://searchmyview.netlify.app/?a="+place.getLatLng().latitude+"&b="+place.getLatLng().longitude)));
            }
        }
    }
}
