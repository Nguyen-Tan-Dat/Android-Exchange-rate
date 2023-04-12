package com.dat;

public class Currency {
    private String id;
    private String name;

    public Currency(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id.toLowerCase();
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return id + " (" + name+")";
    }

    private double value = 0;

    public void setValue(double value) {
        this.value = value;
    }

    public double getValue() {
        return value;
    }
}
