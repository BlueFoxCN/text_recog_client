package com.yjf.myapplication;

import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;


public class ThreadClass {
    //判断文件是否存在
    public boolean fileIsExists(File f) {
        try
        {
            if(!f.exists())
            {
                return false;
            }

        }
        catch (Exception e)
        {
            return false;
        }

        return true;
    }


    //读取ｉｐ地址
    public String readIP(File file) {

        InputStream fileInputStream = null;
        String line = "";
        try {
            fileInputStream = new FileInputStream(file);

            if (fileInputStream != null) {
                InputStreamReader inputreader = new InputStreamReader(fileInputStream);
                BufferedReader buffread = new BufferedReader(inputreader);

                line = buffread.readLine();
                fileInputStream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return  line;
    }



    //写ip到ｔｘｔ
    public  void writeIp(String file, String con){

        try {
            FileWriter fw = new FileWriter(file);
            fw.write(con);
            fw.flush();
            fw.close();
        }catch (Exception e){
            e.printStackTrace();
        }

    }


    //创建文件
    public boolean createFiles(String filename){
        Log.d("TAGSs",filename);
        File fileName = new File(filename);
        boolean flag=false;
        try{
            if(!fileName.exists()){
                fileName.createNewFile();
                flag=true;
                Log.d("TAGS", "F");
            }
            else {
                Log.d("TAGS--", "F");
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return flag;
    }
}
