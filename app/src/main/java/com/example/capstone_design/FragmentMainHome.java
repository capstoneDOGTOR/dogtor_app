package com.example.capstone_design;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.capstone_design.retrofit.Data;
import com.example.capstone_design.retrofit.RetrofitClient;

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FragmentMainHome extends Fragment {

    CircleImageView main_dog_profile_image;
    TextView main_dog_name;
    TextView main_dog_age;
    TextView main_dog_gender;
    TextView main_dog_type;
    TextView main_dog_weight;
    TextView main_dog_kcal;
    TextView fcm_msg;
    TextView eat_msg;

    // SharedPreferences를 통해 fcm body 데이터 가져오기
    String loadSharedName = "DB";
    String fcm_body = "";

    RetrofitClient retrofitClient = new RetrofitClient();

    String token;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_main_home, container, false);

        SharedPreferences loadShared = this.getActivity().getSharedPreferences("DB", Context.MODE_PRIVATE);
        token = loadShared.getString("token", "");

        main_dog_name = view.findViewById(R.id.main_dog_name);
        main_dog_age = view.findViewById(R.id.main_dog_age);
        main_dog_gender = view.findViewById(R.id.main_dog_gender);
        main_dog_type = view.findViewById(R.id.main_dog_kind);
        main_dog_weight = view.findViewById(R.id.main_dog_weight);
        main_dog_profile_image = view.findViewById(R.id.main_dog_profile_image);
        main_dog_kcal = view.findViewById(R.id.main_dog_kcal);
        fcm_msg = view.findViewById(R.id.fcm_msg);
        eat_msg = view.findViewById(R.id.eat_msg);

        Log.d("hyeals_bundle_fragment", "프래그먼트 실행");

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        SharedPreferences loadShared = this.getActivity().getSharedPreferences(loadSharedName, Context.MODE_PRIVATE);
        fcm_body = loadShared.getString("fcm_body", "아직 받은 알림이 없습니다.");
        fcm_msg.setText(fcm_body);

        Log.d("fcm_body", "fcm body: " + fcm_body);


        // 반려견 정보 GET 요청
        retrofitClient.retrofitGetAPI.getUser(token).enqueue(new Callback<Data>() {
            @Override
            public void onResponse(Call<Data> call, Response<Data> response) {
                if (response.isSuccessful()) {
                    Data data = response.body();

                    Log.d("hyeals_bundle", data.getDog_name());

                    main_dog_name.setText(data.getDog_name());
                    main_dog_age.setText(String.valueOf(data.getDog_birth()) + "살");
                    main_dog_gender.setText(data.getDog_gender());
                    main_dog_weight.setText( String.valueOf(data.getDog_weight()) + " kg");
                    main_dog_kcal.setText(String.valueOf("사료 칼로리 = " + data.getDog_kcal() + "kcal"));

                    switch (data.getDog_type()){
                        case "small":
                            main_dog_type.setText("소형견");
                            break;
                        case "medium":
                            main_dog_type.setText("중형견");
                            break;
                        case "big":
                            main_dog_type.setText("대형견");
                            break;
                    }

                    switch(data.getDog_iamge())
                    {
                        case 1:
                            main_dog_profile_image.setImageResource(R.drawable.dog_profile_1);
                            break;
                        case 2:
                            main_dog_profile_image.setImageResource(R.drawable.dog_profile_2);
                            break;
                        case 3:
                            main_dog_profile_image.setImageResource(R.drawable.dog_profile_3);
                            break;
                    }
                }
            }

            @Override
            public void onFailure(Call<Data> call, Throwable t) {
                t.printStackTrace();
            }
        });

        // 전날 식사량 받아 오기 위해 어제 날짜 가져오기
        SimpleDateFormat date = new SimpleDateFormat("yyyy/MM/dd");
        Calendar calendar = Calendar.getInstance();

        calendar.add(calendar.DATE, -1);
        String yesterday = date.format(calendar.getTime());

        Log.d("why", yesterday);

        // 전날 및 권장 식사량 계산 정보
        retrofitClient.retrofitGetAPI.getRecommend(token, yesterday).enqueue(new Callback<Data>() {
            @Override
            public void onResponse(Call<Data> call, Response<Data> response) {
                if(response.isSuccessful()){
                    Data data = response.body();

                    eat_msg.setText("전날 식사량은 "+data.getSumMeal() + "kcal 입니다.\n 권장 식사량은 "
                            + data.getRecommendMeal() + "kcal 입니다." );
                }
            }

            @Override
            public void onFailure(Call<Data> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }
}