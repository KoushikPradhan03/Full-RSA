package com.heavenbites.exception;

/** Custom checked exception used for every business-rule error. */
public class RestaurantException extends Exception {
    private final int code;

    public RestaurantException(String message) {
        this(message, 400);
    }

    public RestaurantException(String message, int code) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}