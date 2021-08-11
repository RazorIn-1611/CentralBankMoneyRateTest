package com.example.centralbankmoneyratetest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Класс основного Activity
 */
public class MainActivity
        extends AppCompatActivity
            implements View.OnClickListener {

    SharedPreferences preferences;//Настройки для сохранения состояния приложения

    private TextView dialog_summ_textview, convert_res;//TextView с вводом суммы для конвертации и итоговым значением.

    private Spinner start_spinner;//Выпадающий список с валютами для конвертации
    private RecyclerView recycler_list_valute;//View-список для отображения нашего списка валют.
    private Dialog connection_lost_dialog, convert_dialog;//Диалоги, Потеряно соединение и Конвертер валют.

    private Button close_connection_lost_dialog, dialog_convert_button;//Кнопки для создания диалога конвертации, закрытия диалогов, обновления списка валют.
    private Button convert_value_button, refresh_list_button, download_button;

    private Context context;
    private LinearLayoutManager layoutManager;
    private ListValutesAdapter taskAdapter;

    private final int delay_mils = 7000;//= секунд задержки для обновления курса валют

    public static String LOG_TAG = "json_log";//LOG_TAG

    private ArrayList<ValuteItem> list;//список обьектов-валют для отображения и работы с ними.
    private JsonReaderClass task;//обьект класса для чтения json

    private DBHelper dbHelper;//Обьект от SQLhelper.

    final String jsonUrl = "https://www.cbr-xml-daily.ru/daily_json.js"; //адрес для получения json-данных
    final String prefButtonState = "isButtonInvisible";//key - для preferences

    double value_conv;//Переменная для конвертации валюты
    String name_conv;//Название конвертируемой валюты (Пример - EUR)

    private Handler hRefresh = new Handler();//Обработчик для доп. потока обновления списка
    private Runnable listRefresher;//Обьект Reunnable для создания периодического обновления


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        list = new ArrayList<>();

        initView();

        //Если данные уже были загружены - периодически обновляем список.
        if(!(download_button.getVisibility() == View.VISIBLE)){
            listRefresher = new Runnable() {
                @Override
                public void run() {
                    if(isOnline(MainActivity.this)){
                        try {
                            //Получаем данные по курсу и заполняем список.
                            downloadAndShowJson();
                        }catch (Exception e){
                            Toast.makeText(getBaseContext(), "Данные курса недоступны. ", Toast.LENGTH_SHORT).show();
                        }
                        Toast.makeText(getBaseContext(), "Данные курса обновлены автоматически. ", Toast.LENGTH_SHORT).show();
                    }
                    hRefresh.removeCallbacks(listRefresher);
                    hRefresh.postDelayed(this, delay_mils);
                }
            };
        }
    }

    /**
     * Функция инициализации основных элементов
     */
    private void initView() {
        //Получаем внутренние настройки приложения
        preferences = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor ed =  preferences.edit();

        //Если приложение уже было успешно загружено ранее и
        // в нем хранится набор данных с курсом, кнопку
        // для загрузки данных мы не показываем. В противном же случае - не
        // показываем кнопки Конвертации и Обновления.
        if(!preferences.getString(prefButtonState, "").equals("1")){
            //Кнопка загрузки списка валют
            download_button = findViewById(R.id.download_button);
            download_button.setOnClickListener(this);

            //Создаем кнопку конвертации.
            convert_value_button = findViewById(R.id.convert_button);
            convert_value_button.setVisibility(View.GONE);

            //Кнопка обновления
            refresh_list_button = findViewById(R.id.refresh_button);
            refresh_list_button.setVisibility(View.GONE);

        }else{

            //Создаем кнопку конвертации.
            convert_value_button = findViewById(R.id.convert_button);
            convert_value_button.setOnClickListener(this);

            //Кнопка обновления
            refresh_list_button = findViewById(R.id.refresh_button);
            refresh_list_button.setOnClickListener(this);

            download_button = findViewById(R.id.download_button);
            download_button.setVisibility(View.GONE);
        }
    }

    /**
     * Функция проверки наличия интернет-соединения на устройстве
   */
    private boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting())
        {
            return true;
        }
        return false;
    }

    /**
     * Функция, обрабатывающая результат проверки интернет-соединения. Выдает диалоговое окно в случае потери соединения.
    */
    private void checkConnectionOnline() {
        if(!isOnline(this)){

            connection_lost_dialog = new Dialog(MainActivity.this);
            connection_lost_dialog.setTitle("Потеряно соединение.");
            connection_lost_dialog.setContentView(R.layout.dialog_connection_lost);

            close_connection_lost_dialog = connection_lost_dialog.findViewById(R.id.dialog_close_button);
            close_connection_lost_dialog.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    connection_lost_dialog.dismiss();
                    //finish();
                }
            });
            connection_lost_dialog.show();
            hRefresh.removeCallbacks(listRefresher);
        }
    }


    /**
     * Функция загрузки и чтения Json-текста.
    */
    public void downloadAndShowJson() {

        //checkConnectionOnline();//Проверяем, есть ли интернет соединение..

        task = new JsonReaderClass();
        task.execute(jsonUrl);
        try{
            String result = "";
            result = task.get();
            createList(result);

        }catch (Exception e){

        }

    }

    /**
     * Функция "слушатель" для обработки взаимодействия с функциональными элементами.
     * @param v - view на которую нажали
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()){

            //Отслеживаем нажатие кнопки "Конвертировать".
            case R.id.convert_button:
                //checkConnectionOnline();//Проверим на подключение к интернету

                //Останавливаем обновление, если оно нам не нужно.
                hRefresh.removeCallbacks(listRefresher);

                try {
                    //Если список не пустой :
                    if(list!=null){
                        if(list.size()>0)
                            convertValue();//Функция создания диалогового окна и последующей конвертации
                    }
                }catch (Exception e){
                    Toast.makeText(getBaseContext(), "Данные курса недоступны. ", Toast.LENGTH_SHORT).show();
                    Log.i(LOG_TAG, e.getMessage() + "");
                }
                break;




            //Отслеживаем нажатие кнопки "Обновить".
            case R.id.refresh_button:
                checkConnectionOnline();//Проверяем подключение к интернету
                if(isOnline(MainActivity.this)){
                    try {
                        if(isOnline(this)){

                            //Получаем новые данные по курсу и заполняем ими список
                            downloadAndShowJson();
                            Toast.makeText(getBaseContext(), "Данные курса обновлены. ", Toast.LENGTH_SHORT).show();
                        }
                    }catch (Exception e){
                        Toast.makeText(getBaseContext(), "Данные курса недоступны. ", Toast.LENGTH_SHORT).show();
                    }
                }
                break;





            //Отслеживаем нажатие кнопки "Загрузить данные".
            case R.id.download_button:

                checkConnectionOnline();
                context = this;
                recycler_list_valute = findViewById(R.id.recycler_list_valute);


                try {

                    //Получаем новые данные по курсу и заполняем ими список
                    downloadAndShowJson();
                }catch (Exception e){
                    Toast.makeText(getBaseContext(), "Данные курса недоступны. ", Toast.LENGTH_SHORT).show();
                }

                listRefresher = new Runnable() {
                    @Override
                    public void run() {
                        if(isOnline(MainActivity.this)){
                            try {
                                //Получаем данные по курсу и заполняем список.
                                downloadAndShowJson();
                            }catch (Exception e){
                                Toast.makeText(getBaseContext(), "Данные курса недоступны. ", Toast.LENGTH_SHORT).show();
                            }
                            Toast.makeText(getBaseContext(), "Данные курса обновлены автоматически. ", Toast.LENGTH_SHORT).show();
                            hRefresh.removeCallbacks(listRefresher);
                            hRefresh.postDelayed(this, delay_mils);
                        }
                    }
                };

                //Создаем автоматическое обновление курса периодически.
                hRefresh.removeCallbacks(listRefresher);
                hRefresh.postDelayed(listRefresher, delay_mils);


                //Показываем кнопки для работы с данными списка.
                refresh_list_button.setVisibility(View.VISIBLE);
                refresh_list_button.setOnClickListener(this);
                convert_value_button.setVisibility(View.VISIBLE);
                convert_value_button.setOnClickListener(this);

                //Устанавливаем настройки так, что бы кнопка "Загрузить" больше не показывалась.
                download_button.setVisibility(View.GONE);
                preferences = getPreferences(MODE_PRIVATE);
                SharedPreferences.Editor ed =   preferences.edit();
                ed.putString(prefButtonState, "1");
                ed.apply();

                break;
        }
    }

    /**
     * Функция для обработки полученных данных и заполнения списка.
     * @param json_text полученный json текст.
     */
    private void createList(String json_text){
        if (list!=null){
            if(list.size()>0){
                list.removeAll(list);
            }
        }
        try{
            JSONObject jsonRoot = new JSONObject(json_text);
            JSONObject jsonValutes = jsonRoot.getJSONObject("Valute");
            JSONArray arrayNames = jsonValutes.names();

            for(int i=0; i<arrayNames.length(); i++){
                JSONObject bufferObj = jsonValutes.getJSONObject(arrayNames.get(i).toString());
                list.add(new ValuteItem(bufferObj.getString("Name"), bufferObj.getString("CharCode"), bufferObj.optDouble("Value"), bufferObj.getInt("NumCode") ));
            }


            //Создание менеджера для управления списком.
            layoutManager = new LinearLayoutManager(context);
            recycler_list_valute.setLayoutManager(layoutManager);

            //Создание адаптера и установка его на recyclerView
            taskAdapter = new ListValutesAdapter(list, context);
            recycler_list_valute.setAdapter(taskAdapter);

        }catch (Exception e){
            Log.i(LOG_TAG, e.getMessage());
        }
    }

    /**
     * Функция сохранения данных по курсу в бд.
     */
    private void saveData() {

        try {
            dbHelper = new DBHelper(this);
            ContentValues cv = new ContentValues();
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            for(int i=0; i<list.size(); i++){
                cv.put("name", list.get(i).getName());
                cv.put("value", list.get(i).getValue());
                cv.put("charcode", list.get(i).getCharcode());
                cv.put("numcode", list.get(i).getNumcode());
                Log.i(LOG_TAG, "Put this: " + list.get(i).getName() + list.get(i).getValue() + list.get(i).getCharcode() + list.get(i).getNumcode());
                // вставляем запись и получаем ее ID
                long rowID = db.insert("mytable", null, cv);
            }
            dbHelper.close();
        }catch (Exception e){
            Log.i(LOG_TAG, e.getMessage() + " --------- ");
        }

    }

    /**
     * Функция чтения данных из бл. Должна вызываться при пересоздании элементов активити.
     */
    private void readData(){
        dbHelper = new DBHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor c = db.query("mytable", null, null, null, null, null, null);

        if (list!=null) {
            if (list.size() > 0) {
                list.removeAll(list);
            }
        }

            if (c.moveToFirst()) {

            // определяем номера столбцов по имени в выборке
            int idColIndex = c.getColumnIndex("id");
            int nameColIndex = c.getColumnIndex("name");
            int valueColIndex = c.getColumnIndex("value");
            int charCodeColIndex = c.getColumnIndex("charcode");
            int numCodeColIndex = c.getColumnIndex("numcode");

            do {
                list.add(new ValuteItem(c.getString(nameColIndex), c.getString(charCodeColIndex), c.getDouble(valueColIndex), c.getInt(numCodeColIndex)));
                // получаем значения по номерам столбцов и пишем все в лог
                Log.d(LOG_TAG,
                        "ID = " + c.getInt(idColIndex) +
                                ", name = " + c.getString(nameColIndex));
                // переход на следующую строку
                // а если следующей нет (текущая - последняя), то false - выходим из цикла
            } while (c.moveToNext());
        } else{
            Log.d(LOG_TAG, "0 rows");
        }
        c.close();
        if(list!=null){
            recycler_list_valute = findViewById(R.id.recycler_list_valute);
            //Создание менеджера для управления списком.
            layoutManager = new LinearLayoutManager(context);
            recycler_list_valute.setLayoutManager(layoutManager);

            //Создание адаптера и установка его на recyclerView
            taskAdapter = new ListValutesAdapter(list, context);
            recycler_list_valute.setAdapter(taskAdapter);
        }

        db.delete("mytable", null, null);
    }

    /**
     * Функция создания диалогового окна и последующей конвертации.
     */
    private void convertValue() {

        //Создаем диалог для конвертирования
        convert_dialog = new Dialog(MainActivity.this);
        convert_dialog.setTitle("Convert Valute.");
        convert_dialog.setContentView(R.layout.convert_dialog);

        //Слушатель сворачивания диалога, что бы восстановить периодическое обновление
        convert_dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                hRefresh.removeCallbacks(listRefresher);
                hRefresh.postDelayed(listRefresher, delay_mils);
            }
        });

        //Инициализируем окошко для ввода суммы
        dialog_summ_textview = convert_dialog.findViewById(R.id.dialog_summ_textview);

        //Инициализация окна с итоговым значением
        convert_res = convert_dialog.findViewById(R.id.result_conv);

        //Два выпадающих списка с доступными валютами.
        start_spinner = convert_dialog.findViewById(R.id.start_spinner);

        //Массив наименований валют для отображения в "выпадающем" списке
        String[] array = new String[list.size()];
        for(int i = 0; i < list.size(); i++){
            array[i] = list.get(i).getName();
        }

        //Адаптер для выпадающего списка
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, array );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        //Spinner spinner = (Spinner) findViewById(R.id.spinner);
        start_spinner.setAdapter(adapter);
        // заголовок
        start_spinner.setPrompt("Title");
        // выделяем элемент
        start_spinner.setSelection(0);
        start_spinner.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                hideKeyboardFrom(getApplicationContext(), dialog_summ_textview);
                return false;
            }
        });
        //Обработчик нажатия для элемента выпадающего списка.
        start_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                hideKeyboardFrom(getApplicationContext(), dialog_summ_textview);
                value_conv = list.get(position).getValue();
                name_conv = list.get(position).getCharcode();
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        //Кнопка конвертации внутри диалога
        dialog_convert_button = convert_dialog.findViewById(R.id.dialog_convert_button);
        dialog_convert_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboardFrom(getApplicationContext(), dialog_summ_textview);
                try{
                    double convert_result = Double.valueOf(dialog_summ_textview.getText().toString());
                    convert_result/=value_conv;
                    String text = String.format("%.1f", convert_result);
                    convert_res.setText(text + " " + name_conv);

                }catch (Exception e){
                    Toast.makeText(getBaseContext(), "Ошибка конвертации. Неверные параметры.", Toast.LENGTH_SHORT).show();
                }

            }
        });

        convert_dialog.show();
    }

    @Override
    public void onStart() {
        try{
            readData();
        }catch (Exception e){ Toast.makeText(getBaseContext(), "Восстановления не произошло, возможная ошибка.", Toast.LENGTH_SHORT).show();}
        hRefresh.postDelayed(listRefresher, delay_mils);
        super.onStart();
    }

    @Override
    public void onPause() {
        hRefresh.removeCallbacks(listRefresher);
        super.onPause();
    }

    @Override
    public void onStop() {
        try {
            saveData();
            hRefresh.removeCallbacks(listRefresher);
        }catch (Exception e){
            Toast.makeText(getBaseContext(), "Ничего не сохранилось.", Toast.LENGTH_SHORT).show();
            Log.i(LOG_TAG, e.getMessage() + "");
        }
        super.onStop();
    }

    @Override
    public void onDestroy() {

        super.onDestroy();
    }

    @Override
    public void onResume() {
        hRefresh.postDelayed(listRefresher, delay_mils);
        super.onResume();
    }

    /**
     * Функция скрытия клавиатуры, используется после ввода информации.
     * @param context - контекст,
     * @param view - view нужного элемента.
     */
    public static void hideKeyboardFrom(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }


    class DBHelper extends SQLiteOpenHelper {

        public DBHelper(Context context) {
            // конструктор суперкласса
            super(context, "myDB", null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.d(LOG_TAG, "--- onCreate database ---");
            // создаем таблицу с полями
            db.execSQL("create table mytable ("
                    + "id integer primary key autoincrement,"
                    + "name text,"
                    + "value double,"
                    + "numcode integer,"
                    + "charcode text" + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }
}


