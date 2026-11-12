package com.heavenbites.model;

import com.heavenbites.util.Text;

public class Booking implements Jsonable {
    private final String id, customerId, customerName, phone, date, time;
    private final int guests, tableNo;
    private String status;

    public Booking(String id, String customerId, String customerName, String phone,
                   String date, String time, int guests, int tableNo, String status) {
        this.id = id;
        this.customerId = customerId;
        this.customerName = customerName;
        this.phone = phone;
        this.date = date;
        this.time = time;
        this.guests = guests;
        this.tableNo = tableNo;
        this.status = status;
    }

    public String getId() { return id; }
    public String getCustomerId() { return customerId; }
    public String getDate() { return date; }
    public String getTime() { return time; }
    public int getTableNo() { return tableNo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String toLine() {
        return String.join("|", id, customerId, Text.clean(customerName), Text.clean(phone), date, time,
                String.valueOf(guests), String.valueOf(tableNo), status);
    }

    public static Booking fromLine(String line) {
        String[] f = line.split("\\|", -1);
        return new Booking(f[0], f[1], f[2], f[3], f[4], f[5],
                Integer.parseInt(f[6]), Integer.parseInt(f[7]), f[8]);
    }

    @Override
    public String toJson() {
        return new StringBuilder("{")
                .append("\"id\":").append(Text.q(id))
                .append(",\"customerId\":").append(Text.q(customerId))
                .append(",\"customerName\":").append(Text.q(customerName))
                .append(",\"phone\":").append(Text.q(phone))
                .append(",\"date\":").append(Text.q(date))
                .append(",\"time\":").append(Text.q(time))
                .append(",\"guests\":").append(guests)
                .append(",\"tableNo\":").append(tableNo)
                .append(",\"status\":").append(Text.q(status))
                .append("}").toString();
    }
}