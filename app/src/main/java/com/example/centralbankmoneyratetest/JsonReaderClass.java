package com.example.centralbankmoneyratetest;

import android.os.AsyncTask;
import android.util.Log;

import androidx.recyclerview.widget.LinearLayoutManager;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Класс для чтения и передачи данных с сайта в Json'е.
 */
public class JsonReaderClass
        extends AsyncTask<String, Void, String > {

    public static String LOG_TAG = "json_log";//LOG_TAG
    private ArrayList<ValuteItem> items;//Список с валютами
    private LinearLayoutManager layoutManager;
    ListValutesAdapter taskAdapter;

    /**
     * Функция геттер для списка валют
     * @return
     */
    public ArrayList<ValuteItem> getItems() {
        return items;
    }

    /**
     * Конструктор класса для чтения
     */
    public JsonReaderClass()  {
    }

    @Override
    protected String doInBackground(String... params) {
        String textUrl = params[0];

        InputStream in = null;
        BufferedReader br= null;
        try {
            URL url = new URL(textUrl);
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();

            httpConn.setAllowUserInteraction(false);
            httpConn.setInstanceFollowRedirects(true);
            httpConn.setRequestMethod("GET");
            httpConn.connect();
            int resCode = httpConn.getResponseCode();

            if (resCode == HttpURLConnection.HTTP_OK) {
                in = httpConn.getInputStream();
                br= new BufferedReader(new InputStreamReader(in));

                StringBuilder sb= new StringBuilder();
                String s= null;
                while((s= br.readLine())!= null) {
                    sb.append(s);
                    sb.append("\n");
                }
                return sb.toString();
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
            }catch (Exception e) {

            }
            try {
                br.close();
            }catch (Exception e) {

            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(String result) {

        if(result  != null){
            //this.textView.setText(result);
            //createList(result);
        } else{
            Log.e(LOG_TAG, "Error!");
        }
    }

}
