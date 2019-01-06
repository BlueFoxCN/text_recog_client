package com.yjf.myapplication;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;


import android.content.pm.PackageManager;

import java.io.FileReader;
import java.io.FileWriter;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;

import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;


import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener{

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    public String[] data = { "Apple", "Banana", "Orange", "Watermelon",
            "Pear", "Grape", "Pineapple", "Strawberry", "Cherry", "Mango" };

    private TextView server_state;
    public String ip_address;
    private String file_video_pah = "/sdcard/sysvideocamera.mp4";
    private String ip_file_path = "/sdcard/ip_address.txt";
    private String result_root_path = "/sdcard/AnalysisResult";
    private String save_video_path = "/sdcard/AnalysisVideo";
    public String process_show;
    public int process_show_value=0;
    public String sub_save_path;
    private int video_size;
    private ProgressDialog dialog;
    private Spinner spinner;
    private List<String> data_list;
    private ArrayAdapter<String> arr_adapter;

    private ListView listView;
    private ArrayAdapter<String> adapter;
    private byte languageSelectionFlag = 0;
    private boolean is_selected = false;

    public ThreadClass threadClass;
    public int receive_over_count = 0;
    public File result_f;
    public boolean error_flage = false;

    public NetworkThread net_th;
    public Object sync_token;

    public boolean done;
    public Handler waiting_handler;

    public Handler ip_handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message message) {
            set_ip();
        }
    };

    public Handler progress_handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message message) {
            dialog.setProgress(message.arg1);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        threadClass = new ThreadClass();
        Button button = (Button) findViewById(R.id.bt_video);
        button.setOnClickListener(this);

        dialog = new ProgressDialog(MainActivity.this);
        spinner = (Spinner) findViewById(R.id.spinner);
        listView = (ListView) findViewById(R.id.list_view);

        Button button_clearAll = (Button) findViewById(R.id.button_clearAll);
        button_clearAll.setOnClickListener(this);
        Button button_flush = (Button) findViewById(R.id.button_flush);
        button_flush.setOnClickListener(this);
        Button button_select_video = (Button) findViewById(R.id.select_video);
        button_select_video.setOnClickListener(this);

        sync_token = new Object();

        //检查用于保存结果的根目录是否存在
        result_f = new File(result_root_path);
        if (!result_f.exists()){
            result_f.mkdirs();
        }
        File result_f1 = new File(save_video_path);
        if (!result_f1.exists()){
            result_f1.mkdirs();
        }

        data = result_f.list();
        if (data.length <=0){
            Toast.makeText(this, "历史记录为空",
                Toast.LENGTH_SHORT).show();
        }else {
            reverse1(data);
        }
        adapter = new ArrayAdapter<String>(
                MainActivity.this, android.R.layout.simple_list_item_1, data);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);
        listView.setOnItemLongClickListener(this);

        //数据
        data_list = new ArrayList<String>();
        data_list.add("中文");
        data_list.add("英文");
        data_list.add("韩文");
        data_list.add("日文");

        //适配器
        arr_adapter= new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, data_list);
//        设置样式
        arr_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(arr_adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view,
                                       int position, long id) {
                languageSelectionFlag = (byte)position;
            }
            @Override
            public void onNothingSelected(AdapterView<?> view) {
                Log.d("TAG---","f");
            }
        });

        //检查ip地址文件是否存在
        if (threadClass.fileIsExists(new File(ip_file_path))){
            ip_address = threadClass.readIP(new File(ip_file_path));
            net_th = new NetworkThread(ip_address, sync_token);
            net_th.start();
        }
        else{
            set_ip();
        }

    }

    class Beating implements Runnable {
        OutputStream out;
        public boolean stop;
        Handler handler;
        Beating(OutputStream out, Handler handler) {
            this.out = out;
            this.stop = false;
            this.handler = handler;
        }
        public void run() {
            if (this.stop == false) {
                try {
                    this.out.write("hello".getBytes());
                    this.out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            handler.postDelayed(this, 1000);
        }
    }

    class NetworkThread extends Thread {
        private String ip;
        private Socket sock;
        private OutputStream out;
        private InputStream in;
        private Object sync_token;
        private Handler beating_handler;
        private Beating beating;

        private DataInputStream dis;
        private FileOutputStream fos;

        private int img_idx;
        private int txt_idx;

        private int[] progress_map =  {50, 55, 60, 65, 70, 75, 80, 85, 90};
        String[] message_map = {"", "(1/9)-读取视频．．．", "(2/9)-翻页动作识别．．．", "(3/9)-最优页面选择．．．",
                "(4/9)-文本区域检测．．．", "(5/9)-图表检测．．．", "(6/9)-页面分割处理．．．",
                "(7/9)-页面单行提取．．．", "(8/9)-文本单行识别．．．", "(9/9)-保存结果．．．",
                "(9/9)-返回结果数据．．．"};

        public NetworkThread (String ip, Object sync_token) {
            this.ip = ip;
            this.sync_token = sync_token;
            this.beating_handler = new Handler();
        }

        @Override
        public void run(){
            try {
                this.sock = new Socket();
                this.sock.setSoTimeout(200);
                this.sock.connect(new InetSocketAddress(this.ip, 8117), 200);
                this.sock.setSoTimeout(0);
                // this.sock = new Socket(this.ip, 8117);
                this.out = this.sock.getOutputStream();
                this.in = this.sock.getInputStream();
                this.dis = new DataInputStream(this.in);

                this.beating = new Beating(this.out, beating_handler);
                beating_handler.postDelayed(this.beating, 1000);

                int img_file_num = 0;
                int txt_file_num = 0;
                ByteBuffer wrapped;
                int buf_len = 1024;
                int file_size, remain_size, read_size;
                String img_save_path, txt_save_path;
                byte[] size_buf = new byte[4];
                byte[] content_buf = new byte[buf_len];

                while (true) {
                    img_idx = 0;
                    txt_idx = 0;

                    this.beating.stop = false;

                    synchronized (sync_token) {
                        try {
                            sync_token.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    // video file is ready, send it to the server
                    this.beating.stop = true;
                    this.out.write("ready".getBytes());

                    // 1. send the file head
                    File f = new File(file_video_pah);
                    file_size = (int)f.length();
                    ByteBuffer buf = ByteBuffer.allocate(5);
                    buf.putInt(file_size);
                    buf.put(languageSelectionFlag);
                    byte[] file_head = buf.array();
                    out.write(file_head);
                    out.flush();

                    // 2. send file content
                    FileInputStream inputStream = new FileInputStream(new File(file_video_pah));
                    byte[] file_buf = new byte[10240];
                    int cur_send = 0;
                    int len;
                    while ((len = inputStream.read(file_buf, 0, file_buf.length)) != -1) {
                        cur_send += len;
                        out.write(file_buf,0 , len);//将文件循环写入输出流
                        out.flush();
                        float tem_val = (float)cur_send / (float)file_size;
                        process_show_value = (int)(tem_val * 50);
                        dialog.setProgress(process_show_value);
                    }

                    // 3. waiting for progress info
                    int progress_idx = 0;
                    while (progress_idx < 8) {
                        byte[] bytes = new byte[1];
                        int ret = dis.read(bytes, 0, 1);
                        progress_idx = (int)bytes[0];
                        Log.d("PROGRESS_ID", String.valueOf(progress_idx));
                        process_show_value = progress_map[progress_idx];
                        Message message = progress_handler.obtainMessage(0, process_show_value, process_show_value);
                        message.sendToTarget();
                        // dialog.setProgress(process_show_value);
                    }

                    // 4. receive image data
                    dis.read(size_buf,0,4);
                    wrapped = ByteBuffer.wrap(size_buf);
                    img_file_num = wrapped.getInt();
                    for (int img_idx = 0; img_idx < img_file_num; img_idx++) {
                        Log.d("TAGSAVEPATH-img---", sub_save_path);
                        img_save_path = sub_save_path + "/" + Integer.toString(img_idx) + ".jpeg";
                        fos = new FileOutputStream(new File(img_save_path));

                        dis.read(size_buf, 0, 4);
                        wrapped = ByteBuffer.wrap(size_buf);
                        file_size = wrapped.getInt();
                        remain_size = file_size;

                        while (remain_size > 0) {
                            read_size = remain_size > buf_len ? buf_len : remain_size;
                            int ret = dis.read(content_buf, 0, read_size);
                            remain_size = remain_size - ret;
                            fos.write(content_buf, 0, ret);
                        }
                        fos.flush();
                        fos.close();
                    }
                    dialog.setProgress(95);

                    // 5. receive text data
                    dis.read(size_buf,0,4);
                    wrapped = ByteBuffer.wrap(size_buf);
                    txt_file_num = wrapped.getInt();
                    Log.d("TXT_NUM", String.valueOf(txt_file_num));
                    for (int txt_idx = 0; txt_idx < txt_file_num; txt_idx++) {
                        Log.d("TAGSAVEPATH-txt---", sub_save_path);
                        txt_save_path = sub_save_path + "/" + Integer.toString(txt_idx) + ".txt";
                        fos = new FileOutputStream(new File(txt_save_path));

                        dis.read(size_buf, 0, 4);
                        wrapped = ByteBuffer.wrap(size_buf);
                        file_size = wrapped.getInt();
                        remain_size = file_size;
                        Log.d("TXT_FILE_SIZE", String.valueOf(file_size));

                        while (remain_size > 0) {
                            read_size = remain_size > buf_len ? buf_len : remain_size;
                            int ret = dis.read(content_buf, 0, read_size);
                            remain_size = remain_size - ret;
                            fos.write(content_buf, 0, ret);
                        }
                        fos.flush();
                        fos.close();
                    }
                    dialog.setProgress(100);
                    done = true;
                }
            } catch (IOException e) {
                e.printStackTrace();
                Message message = ip_handler.obtainMessage();
                message.sendToTarget();
            }
        }
    }

    protected void deal_data(){
        receive_over_count = 0;
        File file = new File(file_video_pah);
        if (file.length() < 3576059){
            AlertDialog.Builder d  = new AlertDialog.Builder(MainActivity.this);
            d.setTitle("警告!" ) ;
            d.setMessage("视频不得少于1秒！" ) ;
            d.setPositiveButton("OK" ,  null );
            d.show();
            return;
        }
        /* 先创建保存本次结果的目录 */
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");
        String tem_dir_name =format.format(new Date());
        sub_save_path = result_root_path+"/"+tem_dir_name;
        // 检查目录是否存在
        File result_f = new File(sub_save_path);
        if (!result_f.exists()){
            result_f.mkdirs();
        }
        error_flage=false;
        dialog.setTitle("提示信息");
        dialog.setMessage("正在处理，请稍候...");
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);//设置进度条为方形的，默认为圆形
        dialog.setMax(100);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();

        synchronized (this.sync_token) {
            this.sync_token.notify();
        }

        class Waiting implements Runnable {
            Handler handler;
            Waiting(Handler handler) {
                this.handler = handler;
            }
            public void run() {
                if (done == true) {
                    process_show_value = 0;
                    dialog.cancel();

                    Bundle argz = new Bundle();
                    argz.putString("path", sub_save_path);
                    argz.putString("flages", "result");
                    Fragment_Result fragment = new Fragment_Result();
                    fragment.setArguments(argz);
                    FragmentManager fragmentManager = getFragmentManager();
                    FragmentTransaction transaction = fragmentManager. beginTransaction();
                    transaction.replace(R.id.main_layout, fragment);
                    transaction.addToBackStack(null);
                    transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                    transaction.commitAllowingStateLoss();
                    refresh();
                    done = false;
                } else {
                    handler.postDelayed(this, 1000);
                }
            }
        }

        waiting_handler = new Handler();
        Waiting waiting = new Waiting(waiting_handler);
        waiting_handler.postDelayed(waiting, 1000);
    }

    public String getFileByUri(Uri uri) {
        String path = null;
        if ("file".equals(uri.getScheme())) {
            path = uri.getEncodedPath();
            if (path != null) {
                path = Uri.decode(path);
                ContentResolver cr = this.getContentResolver();
                StringBuffer buff = new StringBuffer();
                buff.append("(").append(MediaStore.Images.ImageColumns.DATA).append("=").append("'" + path + "'").append(")");
                Cursor cur = cr.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new String[] { MediaStore.Images.ImageColumns._ID, MediaStore.Images.ImageColumns.DATA }, buff.toString(), null, null);
                int index = 0;
                int dataIdx = 0;
                for (cur.moveToFirst(); !cur.isAfterLast(); cur.moveToNext()) {
                    index = cur.getColumnIndex(MediaStore.Images.ImageColumns._ID);
                    index = cur.getInt(index);
                    dataIdx = cur.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                    path = cur.getString(dataIdx);
                }
                cur.close();
                if (index == 0) {
                } else {
                    Uri u = Uri.parse("content://media/external/images/media/" + index);
                    System.out.println("temp uri is :" + u);
                }
            }
            if (path != null) {
                return path;
            }
        } else if ("content".equals(uri.getScheme())) {
            // 4.2.2以后
            String[] proj = { MediaStore.Images.Media.DATA };
            Cursor cursor = this.getContentResolver().query(uri, proj, null, null, null);
            if (cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                path = cursor.getString(columnIndex);
            }
            cursor.close();

            return path;
        } else {
//            Log.i(TAG, "Uri Scheme:" + uri.getScheme());
        }
        return null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == 0){
            Toast.makeText(this, "拍摄取消",
                    Toast.LENGTH_SHORT).show();
            if (is_selected){
                is_selected=false;
            }
        }
        else if (resultCode == -1){
            if (is_selected){
                Uri uri = data.getData();
                file_video_pah = getFileByUri(uri);
                Toast.makeText(this, "所选文件路径："+file_video_pah, Toast.LENGTH_SHORT).show();
                String[] strarray=file_video_pah.split("_");
                if (strarray.length != 3){
                    Toast.makeText(this, "所选视频非本程序所拍摄，请重选", Toast.LENGTH_SHORT).show();
                    is_selected=false;
                    return;
                }
                languageSelectionFlag =(byte)Integer.parseInt(strarray[1]);
            }
            else {
                Toast.makeText(this, "拍摄完毕，保存路径："+file_video_pah,
                        Toast.LENGTH_SHORT).show();
            }
            deal_data();
        }
        else
            Toast.makeText(this, "拍摄或选择异常",
                    Toast.LENGTH_SHORT).show();
    }

    public void refresh() {
        data = result_f.list();
        /*
        if (data.length <=0){
            Toast.makeText(getApplicationContext(), "刷新完成，历史记录为空",Toast.LENGTH_SHORT).show();
        }else{
            reverse1(data);
            Toast.makeText(getApplicationContext(), "刷新完成",Toast.LENGTH_SHORT).show();
        }
        */
        adapter = new ArrayAdapter<String>(
                MainActivity.this, android.R.layout.simple_list_item_1, data);
        listView.setAdapter(adapter);
    }

    public void set_ip() {
        Toast.makeText(getApplicationContext(), "连接服务器失败", Toast.LENGTH_SHORT).show();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText edit = new EditText(this);
        builder.setView(edit);
        builder.setTitle("请输入服务器IP地址");
        builder.setPositiveButton("确定", null);
        class PosListener implements View.OnClickListener {
            private final Dialog dialog;

            public PosListener(Dialog dialog) {
                this.dialog = dialog;
            }
            @Override
            public void onClick(View v) {
                String input_ip = edit.getText().toString().trim();
                if (!checkInputIP(input_ip)){
                    Toast.makeText(getApplicationContext(), "请输入正确的ip地址", Toast.LENGTH_SHORT).show();
                    return;
                }
                int permission = ActivityCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);

                if (permission != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS_STORAGE,
                            REQUEST_EXTERNAL_STORAGE);
                }
                if (threadClass.fileIsExists(new File(ip_file_path))){
                    threadClass.writeIp(ip_file_path, edit.getText().toString());
                    ip_address = edit.getText().toString();
                }
                else{
                    if (threadClass.createFiles(ip_file_path)){
                        ip_address = edit.getText().toString();
                        threadClass.writeIp(ip_file_path, edit.getText().toString());
                    }
                    else {
                        Toast.makeText(getApplicationContext(), "ip地址文件创建失败！", Toast.LENGTH_SHORT).show();
                    }
                }
                dialog.dismiss();
                net_th = new NetworkThread(ip_address, sync_token);
                net_th.start();
            }
        }
        builder.setCancelable(false);    //设置按钮是否可以按返回键取消,false则不可以取消
        AlertDialog dialog = builder.create();  //创建对话框
        dialog.setCanceledOnTouchOutside(false); //设置弹出框失去焦点是否隐藏,即点击屏蔽其它地方是否隐藏
        dialog.show();
        Button btn = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        btn.setOnClickListener(new PosListener(dialog));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_video:
                Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");
                String tem_video_name =format.format(new Date());
                file_video_pah = save_video_path+"/"+tem_video_name+"_"+ Byte.toString(languageSelectionFlag) +"_.mp4";
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(file_video_pah)));
                //设置保存视频文件的质量
                intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
                startActivityForResult(intent, 0);
                break;
            case R.id.button_clearAll:
                AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
                builder1.setTitle("信息提示");
                builder1.setMessage("确定清空历史记录？");
                builder1.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (deleteALlFile()){
                            data = result_f.list();
                            if (data.length <=0){
                                Toast.makeText(getApplicationContext(), "历史记录为空",Toast.LENGTH_SHORT).show();
                            }else{
                                reverse1(data);
                            }
                            adapter = new ArrayAdapter<String>(
                                    MainActivity.this, android.R.layout.simple_list_item_1, data);
                            listView.setAdapter(adapter);

                            Toast.makeText(getApplicationContext(), "历史分析数据已清空" , Toast.LENGTH_SHORT).show();
                        }else{
                            Toast.makeText(getApplicationContext(), "历史分析数据清空失败，请重试！", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                builder1.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                });
                builder1.setCancelable(true);    //设置按钮是否可以按返回键取消,false则不可以取消
                AlertDialog dialog1 = builder1.create();  //创建对话框
                dialog1.setCanceledOnTouchOutside(true); //设置弹出框失去焦点是否隐藏,即点击屏蔽其它地方是否隐藏
                dialog1.show();
                break;
            case R.id.button_flush:
                refresh();
                break;
            case R.id.select_video:
                is_selected = true;
                Intent intent1 = new Intent(Intent.ACTION_GET_CONTENT);
                intent1.setType("video/*");
                intent1.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent1,0);
                break;
            default:
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String text= listView.getItemAtPosition(position)+"";
        Bundle argz = new Bundle();
        argz.putString("path", result_root_path+"/"+text);
        argz.putString("flages", "ok");
        Fragment_Result fragment = new Fragment_Result();
        fragment.setArguments(argz);
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction transaction = fragmentManager. beginTransaction();
        transaction.replace(R.id.main_layout, fragment);
        transaction.addToBackStack(null);
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.commitAllowingStateLoss();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
        String item = (String) listView.getItemAtPosition(i);
        if (deleteSubFile(new File(result_root_path+"/"+item))){
            data = result_f.list();
            if (data.length <=0){
                Toast.makeText(this, "历史记录为空",
                        Toast.LENGTH_SHORT).show();
            }else {
                reverse1(data);
            }
            adapter = new ArrayAdapter<String>(
                    MainActivity.this, android.R.layout.simple_list_item_1, data);
            listView.setAdapter(adapter);
            Toast.makeText(this, item + " 删除成功！",
                Toast.LENGTH_SHORT).show();
        }
        else{
            Toast.makeText(this, item + " 删除失败，请重试！",
                    Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    public boolean checkInputIP(String input_ip){
        boolean flages = true;
        int split_flages = 0;
        String[] ip_pools = {"0","1","2","3","4","5","6","7","8","9","."};
        if (input_ip.length() < 7 || input_ip.length() > 15){
            return false;
        }
        for (int i =0; i<input_ip.length(); i++){
            if (Character.toString(input_ip.charAt(i)).equals(".")){
                split_flages+=1;
            }
            if (Arrays.asList(ip_pools).contains(Character.toString(input_ip.charAt(i)))){
                continue;
            }
            else{
                flages=false;

            }
        }
        if (split_flages!=3){
            return false;
        }
        return flages;

    }//judge input ip is ok

    public boolean deleteSubFile(File item){
        if (!item.exists()){
            return false;
        }
        if (item.isFile()){
            return  item.delete();
        }else{
            for (File file_ : item.listFiles()){
                deleteSubFile(file_);
            }
        }
        return item.delete();
    }//delete history deal dir

    public boolean deleteALlFile(){
        Log.d("T--", result_root_path+"/");
        boolean del_all = true;
        File f_all = new File(result_root_path);
        String[] flist = f_all.list();
        if (flist.length <=0){
            return false;
        }
        for(int i=0; i<flist.length; i++){
            Log.d("T--", result_root_path+"/"+flist[i]);
            if (deleteSubFile(new File(result_root_path+"/"+flist[i]))){
                continue;
            }
            else{
                del_all=false;
                break;
            }
        }
        return  del_all;
    }//delete all history deal dir

    public static void reverse1(String[] arr) {
        for(int start=0,end=arr.length-1;start<end;start++,end--) {
            String temp = arr[start];
            arr[start] = arr[end];
            arr[end] = temp;
        }
    }//reverse array

}
