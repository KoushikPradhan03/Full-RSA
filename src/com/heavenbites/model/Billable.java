package com.heavenbites.model;

public interface Billable {
    double TAX_RATE = 0.05;   // 5% GST

    double getSubtotal();
    double getTax();
    double getTotal();
}