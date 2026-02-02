package com.zoffcc.applications.trifa;

public enum ROUTE_TYPE
{
    ROUTE_TYPE_DEFAULT(0),
    ROUTE_TYPE_BY_CAR(1),
    ROUTE_TYPE_BY_FOOT(2);

    public final int value;

    ROUTE_TYPE(int value)
    {
        this.value = value;
    }
}
