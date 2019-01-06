package com.yjf.myapplication;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;

public class Fragment_Bar extends Fragment {

    private ProgressBar bar;
    private TextView tv1;
    private Handler handler;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = inflater.inflate(R.layout.fragment_show_bar, container, false);
        bar =(ProgressBar)view.findViewById(R.id.progressBarLarge1);
        tv1 =(TextView) view.findViewById(R.id.imageView2);
        handler=new Handler(){

                    public void handleMessage(Message msg) {
                        super.handleMessage(msg);
                        int i=msg.what;
                        tv1.setText(i+"%");
                    }
                };
        class processTread extends Thread{
            public void run(){
                super.run();
                for (int i=0; i<= 10; i++){
                    bar.setProgress(i);
                            handler.sendEmptyMessage(i);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                Fragment_Result fragment = new Fragment_Result();
                FragmentManager fragmentManager = getFragmentManager();
                FragmentTransaction transaction = fragmentManager. beginTransaction();

                transaction.add(R.id.main_layout, fragment);


                transaction.commit();




            }
        }
        new processTread().start();

        return view;

    }
}
