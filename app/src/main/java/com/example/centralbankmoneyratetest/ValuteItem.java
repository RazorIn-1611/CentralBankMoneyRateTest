package com.example.centralbankmoneyratetest;

/**
 * Класс, обьект которого представляет из себя валюту, с названием, курсом
 */
public class ValuteItem {
    private int numcode;//numcode валюты
    private double value;//ее курс (в рублях)
    private String name, charcode;//название валюты и ее charcode

    /**
     * Конструктор класса валюты
     * @param name название валюты
     * @param charcode charcode
     * @param value курс валюты(в рублях)
     * @param numcode numcode валюты
     */
    public ValuteItem(String name, String charcode, double value, int numcode){
        this.charcode = charcode;
        this.name = name;
        this.numcode = numcode;
        this.value = value;
    }

    /**
     * Функция получения numcod'a валюты
     */
    public int getNumcode() {
        return numcode;
    }

    /**
     * Функция для установки numcod'a валюты
     */
    public void setNumcode(int numcode) {
        this.numcode = numcode;
    }

    /**
     * Функция получения курса (в рублях) валюты
     */
    public double getValue() {
        return value;
    }

    /**
     * Функция для установки курса (в рублях) валюты
     */
    public void setValue(double value) {
        this.value = value;
    }

    /**
     * Функция получения названия валюты
     */
    public String getName() {
        return name;
    }

    /**
     * Функция для установки названия валюты
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Функция получения charcod'a валюты
     */
    public String getCharcode() {
        return charcode;
    }

    /**
     * Функция для установки charcod'a валюты
     */
    public void setCharcode(String charcode) {
        this.charcode = charcode;
    }


}
