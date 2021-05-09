package com.example.capstone_design;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CalendarView;

import com.example.capstone_design.initInfo.InfoPost;
import com.example.capstone_design.initInfo.InitRetofitAPI;
import com.example.capstone_design.record.Data;
import com.example.capstone_design.record.GetData;
import com.example.capstone_design.record.RecordIntakeRetrofitAPI;
import com.example.capstone_design.record.RecordPeeRetrofitAPI;
import com.example.capstone_design.record.RecordPooRetrofitAPI;
import com.example.capstone_design.recyclerview.RecyclerViewAdapter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class FragmentRecord extends Fragment {

    CalendarView calendarView;
    RecyclerView record_recyclerview;
    Button record_intake_btn;
    Button record_pee_color_btn;
    Button record_poo_color_btn;

    Bundle bundle_record;
    Context mContext;

    String selected_date; // GET 요청으로 보낼 날짜 값
    int intake_page = 0; // GET 요청으로 보낼 페이지 값
    int pee_page = 0;
    int poo_page = 0;
    int limit = 4; // GET 요청으로 한 페이지 당 받아올 데이터 값
    String uid; // GET 요청으로 보낼 uid 값

    List<Data> record_list = new ArrayList<Data>();
    List<Data> get_record_data_list = new ArrayList<Data>();
    RecyclerViewAdapter adapter;
    private boolean isLoading = false;

    String TAG = "";

    // 레트로핏 통신 API
    Retrofit retrofit;
    private RecordIntakeRetrofitAPI recordIntakeRetrofitAPI;
    private RecordPeeRetrofitAPI recordPeeRetrofitAPI;
    private RecordPooRetrofitAPI recordPooRetrofitAPI;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_record, container, false);

        calendarView = view.findViewById(R.id.record_calendarView);
        record_intake_btn = view.findViewById(R.id.record_intake_btn);
        record_pee_color_btn = view.findViewById(R.id.record_pee_color_btn);
        record_poo_color_btn = view.findViewById(R.id.record_poo_color_btn);
        record_recyclerview = view.findViewById(R.id.record_recyclerview);

        Data dummy = new Data();
        dummy.setAmountOfMeal(10);
        record_list.add(dummy);

        bundle_record = getArguments();

        // UID 가져오는 부분
        if(bundle_record != null){
            uid = bundle_record.getString("uid");
        }

        retrofit = new Retrofit.Builder()
                .baseUrl("http://13.209.18.94:3000")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView calendarView, int year, int month, int dayOfMonth) {
                // 달력에서 선택된 날짜 받아오는 부분
                selected_date = String.valueOf(year) + "/" + String.valueOf(month+1) + "/" + String.valueOf(dayOfMonth);
                Log.d("TAG_TEST", selected_date);
            }
        });

        initAdapter();

        record_intake_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                intake_page = 0;
                TAG = "INTAKE";
                // 리사이클러뷰에 사용할 리스트 초기화
                record_list.clear();
                adapter.modifyFlag(TAG);
                getIntake();
                initScrollListener();
            }
        });

        record_pee_color_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pee_page = 0;
                TAG = "PEE";
                // 리사이클러뷰에 사용할 리스트 초기화
                record_list.clear();
                adapter.modifyFlag(TAG);
                Log.d("TAG_TEST", "태그 값: " + TAG);
                getPee();
                initScrollListener();
            }
        });

        record_poo_color_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                poo_page = 0;
                TAG = "POO";
                // 리사이클러뷰에 사용할 리스트 초기화
                record_list.clear();
                adapter.modifyFlag(TAG);
                getPoo();
                initScrollListener();
            }
        });

        return view;
    }

    private void initAdapter(){
        Log.d("TAG_TEST", "어댑터에 들어갈 리스트 RGB 데이터: " + record_list.get(0).getRGB());
        adapter = new RecyclerViewAdapter(record_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(mContext);
        record_recyclerview.setAdapter(adapter);
        record_recyclerview.setLayoutManager(layoutManager);
        Log.d("TAG_TEST", "리사이클러뷰 어댑터 연결");
    }

    private void initScrollListener(){
        record_recyclerview.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();

                if (!isLoading) {
                    if (layoutManager != null && layoutManager.findLastCompletelyVisibleItemPosition() == record_list.size() - 1) {
                        //리스트 마지막
                        loadMore();
                        isLoading = true;
                    }
                }
            }
        });
    }

    private void loadMore(){
        record_list.add(null);
        int count = record_list.size() - 1;
        adapter.notifyItemInserted(count);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d("TAG_TEST", "Load_more 시작");
                record_list.remove(count);
                int scrollPosition = record_list.size();
                adapter.notifyItemRemoved(scrollPosition);

                if(TAG.equals("INTAKE")){
                    intake_page ++;
                    getIntake();
                }else if(TAG.equals("PEE")){
                    pee_page ++;
                    Log.d("TAG_TEST", "리스트 마지막이라 추가 데이터 넣음");
                    getPee();
                }else{
                    poo_page ++;
                    getPoo();
                }

                adapter.notifyDataSetChanged();
                isLoading = false;
            }
        }, 1000);
    }

    private void getIntake(){

        recordIntakeRetrofitAPI = retrofit.create(RecordIntakeRetrofitAPI.class);

        recordIntakeRetrofitAPI.getData(uid, intake_page, limit, selected_date).enqueue(new Callback<GetData>() {
            @Override
            public void onResponse(Call<GetData> call, Response<GetData> response) {

                GetData data = response.body();

                get_record_data_list = data.getRows();

                // 리사이클러뷰에서 사용할 리스트에 GET으로 받아온 리스트 대입
                for(Data item: get_record_data_list){
                    record_list.add(item);
                }
                adapter.notifyDataSetChanged();

            }
            @Override
            public void onFailure(Call<GetData> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }

    private void getPee(){

        recordPeeRetrofitAPI = retrofit.create(RecordPeeRetrofitAPI.class);

        recordPeeRetrofitAPI.getData(uid, pee_page, limit, selected_date).enqueue(new Callback<GetData>() {
            @Override
            public void onResponse(Call<GetData> call, Response<GetData> response) {

                GetData data = response.body();

                get_record_data_list = data.getRows();

                // 리사이클러뷰에서 사용할 리스트에 GET으로 받아온 리스트 대입
                for(Data item: get_record_data_list){
                    record_list.add(item);
                    Log.d("TAG_TEST", "리스트에 데이터 넣음");
                    Log.d("TAG_TEST", record_list.get(0).getRGB());
                }
                adapter.notifyDataSetChanged();

            }
            @Override
            public void onFailure(Call<GetData> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }

    private void getPoo(){

        recordPooRetrofitAPI = retrofit.create(RecordPooRetrofitAPI.class);

        recordPooRetrofitAPI.getData(uid, poo_page, limit, selected_date).enqueue(new Callback<GetData>() {
            @Override
            public void onResponse(Call<GetData> call, Response<GetData> response) {

                GetData data = response.body();

                get_record_data_list = data.getRows();

                // 리사이클러뷰에서 사용할 리스트에 GET으로 받아온 리스트 대입
                for(Data item: get_record_data_list){
                    record_list.add(item);
                }
                adapter.notifyDataSetChanged();
            }
            @Override
            public void onFailure(Call<GetData> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

}