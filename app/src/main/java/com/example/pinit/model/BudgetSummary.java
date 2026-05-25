package com.example.pinit.model;

import java.io.Serializable;

public class BudgetSummary implements Serializable {
    private String title;
    private int total;
    private int food;
    private int transport;
    private int accom;
    private int shopping;
    private int sightseeing;
    private int etc;

    public BudgetSummary(String title, int total, int food, int transport, int accom, int shopping, int sightseeing, int etc) {
        this.title = title;
        this.total = total;
        this.food = food;
        this.transport = transport;
        this.accom = accom;
        this.shopping = shopping;
        this.sightseeing = sightseeing;
        this.etc = etc;
    }

    public String getTitle() { return title; }
    public int getTotal() { return total; }
    public int getFood() { return food; }
    public int getTransport() { return transport; }
    public int getAccom() { return accom; }
    public int getShopping() { return shopping; }
    public int getSightseeing() { return sightseeing; }
    public int getEtc() { return etc; }
}