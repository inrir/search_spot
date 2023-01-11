package com.ms.search_spot;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SearchAddressDialog.Listener {
    int num_of_place;
    FloatingActionButton fab;
    Button fab2;
    RecyclerView recyclerView;
    ArrayList<LatLng> arr;
    ArrayList<Item> arrayList;
    RecyclerAdapter recyclerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        num_of_place = 1;

        arr = new ArrayList<>();
        // Adapter 생성

        arrayList = new ArrayList<Item>();
        recyclerAdapter = new RecyclerAdapter(this, R.layout.item_cardview);

        // 리스트뷰 참조 및 Adapter달기
        recyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        recyclerView.setAdapter(recyclerAdapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);


        // 플로팅 버튼 이벤트 등록 -> "+" 버튼
        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { // 다음 지도 검색하도록 한다.
                new SearchAddressDialog()
                        .setListener(MainActivity.this)
                        .show(getSupportFragmentManager(), "SearchAddressDialog");
            }
        });

//        getAppKeyHash(); // 카카오 Hash key를 구한다.
        // fab2 => 모임장소 찾기 버튼
        fab2 = findViewById(R.id.fab2);
        fab2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 버튼 클릭시 모임위치 찾기 페이지로 이동
                Intent intent = new Intent(MainActivity.this, FindLocationActivity.class);
                intent.putExtra("arr", arr);
                startActivity(intent);
            }
        });

    }

    @Override
    public void onLocationResult(String address, double latitude, double longitude) {
        Log.d("SearchAddressDialog", "onLocationResult Latitude " + latitude + " Longitude " + longitude);
        arr.add(new LatLng(latitude,longitude));
        arrayList.add(new Item("모임원 " + arrayList.size(),"Address: " + address + "\n위도: " + latitude + "\n경도: " + longitude));
        recyclerAdapter.setDataList(arrayList);
        num_of_place++;
    }

    private void getAppKeyHash() { // hash key 받는 거
        PackageInfo packageInfo = null;
        try {
            packageInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (packageInfo == null)
            Log.e("KeyHash", "KeyHash:null");

        assert packageInfo != null;
        for (Signature signature : packageInfo.signatures) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KeyHash", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            } catch (NoSuchAlgorithmException e) {
                Log.e("KeyHash", "Unable to get MessageDigest. signature=" + signature, e);
            }
        }
    }
}
