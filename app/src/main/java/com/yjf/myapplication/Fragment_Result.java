
package com.yjf.myapplication;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.text.method.ScrollingMovementMethod;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Array;
import java.util.Arrays;

public class Fragment_Result extends Fragment implements View.OnTouchListener {

    private ImageView img;
    private  TextView pre_txt;
    private Button pre_bt;
    private Button next_bt;
    private  TextView show_num_label;
    private Bitmap bm;
    private String path;
    private String flages;
    private File file;
    private int total_size=0;
    private int total_size_=0;
    private int current_size=0;
    private  String index;
    private  int line_count = 0;

    private String index_text;
    private  String show_text;
    public ThreadClass threadClass;
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_show_result, container, false);
        view.setOnTouchListener(this);
        img = (ImageView)view.findViewById(R.id.imageView);

        pre_txt = (TextView)view.findViewById(R.id.tv_showText);
        pre_bt = (Button)view.findViewById(R.id.previous);
        next_bt = (Button)view.findViewById(R.id.next_bt);
        show_num_label = (TextView)view.findViewById(R.id.show_num);
//        returen_bt.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                container.removeView(container);
//            }
//        });
//        String paths;
//        paths = Environment.getExternalStorageDirectory()+"/111/1.jpeg";
//        File f=new File(paths);
//        if (!f.exists()) {
//            pre_t xt.setText(paths);
//        }



        pre_bt.setEnabled(true);
        next_bt.setEnabled(true);
        pre_bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pre_bt.setEnabled(true);
                next_bt.setEnabled(true);
                if (current_size>=1) {
                    current_size -= 1;
                    show_num_label.setText(Integer.toString(current_size + 1) + "/" + Integer.toString(total_size));
                    index = path + "/" + Integer.toString(current_size) + ".jpeg";
                    bm = BitmapFactory.decodeFile(index);
                    img.setImageBitmap(bm);

                    index_text = path + "/" + Integer.toString(current_size) + ".txt";
                    File f=new File(index_text);
                    show_text = readText(f);
                    pre_txt.setMovementMethod(ScrollingMovementMethod.getInstance());
                    pre_txt.setText(show_text);

                    if (current_size==0){
                        pre_bt.setEnabled(false);
                        next_bt.setEnabled(true);
                    }
                }
                else{
                    pre_bt.setEnabled(false);
                    next_bt.setEnabled(false);
                }
            }
        });

        next_bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pre_bt.setEnabled(true);
                next_bt.setEnabled(true);
                if (current_size<(total_size-1)){
                    current_size += 1;
                    show_num_label.setText(Integer.toString(current_size + 1) + "/" + Integer.toString(total_size));
                    String index = path + "/" + Integer.toString(current_size) + ".jpeg";
                    bm = BitmapFactory.decodeFile(index);
                    img.setImageBitmap(bm);

                    index_text = path + "/" + Integer.toString(current_size) + ".txt";
                    File f=new File(index_text);
                    show_text = readText(f);
                    pre_txt.setMovementMethod(ScrollingMovementMethod.getInstance());
                    pre_txt.setText(show_text);
                    if (current_size==(total_size-1)){
                        pre_bt.setEnabled(true);
                        next_bt.setEnabled(false);
                    }
                }
                else{
                    next_bt.setEnabled(false);
                    pre_bt.setEnabled(false);
                }
            }
        });

        savedInstanceState = getArguments();
        path = savedInstanceState.getString("path");
        flages = savedInstanceState.getString("flages");


            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        file = new File(path);
        String[] flist = file.list();

        for (String st: flist){
            if (st.contains("single_line.txt")){
                line_count++;
            }
        }
//        Log.d("TAGrrrrrrrrrr-=",flist[0]);
        ///save text to single text
//        Log.d("TAGr---flage------=", flages);
//        if (flages.equals("result")){
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            if (flist.length >0 && (flist.length%2 == 0)){
//                total_size_ = (int) flist.length / 2;
//                String single_line_path = "";
//                String single_line_path_ = "";
//                for (int x = 0; x < total_size_; x++) {
//
//                    Log.d("TAGsssssssssssssssss-=", Integer.toString(x)+ Integer.toString(total_size_));
//                    single_line_path = path + "/" + Integer.toString(x) + ".txt";
//                    single_line_path_ = path + "/" + Integer.toString(x) + "_single_line.txt";
//                    save_Text_Single(new File(single_line_path), new File(single_line_path_));
//            }
//            try {
//                    Thread.sleep(500);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//        }
//
//        }



//        if (flages.equals("ok") && Arrays.asList(flist).contains("0＿single_line.txt") && flist.length>0 && flist.length%3 == 0){
//            total_size = (int)flist.length/3;
//        }
//        else if(flages.equals("result") && Arrays.asList(flist).contains("0_single_line.txt") && flist.length>0 && flist.length%3 == 0){
//
//        }

//        flist = file.list();
        if ((flist.length -line_count) >0 && ((flist.length-line_count)%2 == 0)){
            total_size = (int) (flist.length-line_count)/2;

            index = path + "/" + Integer.toString(current_size) + ".jpeg";
            Log.d("TAGuuuuuuuuuu-=",index);
            bm = BitmapFactory.decodeFile(index);
            img.setImageBitmap(bm);

            index_text = path + "/" + Integer.toString(current_size) + ".txt";
            Log.d("TAG-uuuuuuuuuuuuu=",index_text);
            File f=new File(index_text);
            show_text = readText(f);
            pre_txt.setMovementMethod(ScrollingMovementMethod.getInstance());
            pre_txt.setText(show_text);

            show_num_label.setText(Integer.toString(current_size+1)+"/"+ Integer.toString(total_size));
            pre_bt.setEnabled(false);
            if (current_size == (total_size-1)){
                next_bt.setEnabled(false);
            }

        }
        else
        {
//            bm = BitmapFactory.decodeResource(getResources(), R.drawable.aa);
//            img.setImageBitmap(bm);
//            System.out.print(bm);
            pre_txt.setText("                           file error!!");
            pre_txt.setTextSize(20);
//            pre_bt.setEnabled(false);
//            next_bt.setEnabled(false);

        }


        return view;

    }




    //读取ｔｘｔ
    public String readText(File file){

        BufferedReader br=null;
        StringBuffer sb = null;
        try {
            sb = new StringBuffer();
            br = new BufferedReader(new FileReader(file));
            String line = "";
            while((line = br.readLine())!=null){

                sb.append(line+"\n");
            }
            br.close();
        }catch (Exception e){
            e.printStackTrace();
        }
        return sb.toString();

    }



//    //save receive text to single text
//    public void save_Text_Single(File file, File save_file) {
//
//        BufferedReader br = null;
//        StringBuffer sb = null;
//        FileWriter fw = null;
//
//        try {
//
//            save_file.createNewFile();
//            fw = new FileWriter(save_file);
//            sb = new StringBuffer();
//            br = new BufferedReader(new FileReader(file));
//            String line = "";
//            while ((line = br.readLine()) != null) {
//
//                sb.append(line + "\n");
//            }
//
//            fw.write(sb.toString().replace("\n", "").replaceAll("\\s{1,}", " "));
//            br.close();
//            fw.flush();
//            fw.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//    }





    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        return true;
    }
}






