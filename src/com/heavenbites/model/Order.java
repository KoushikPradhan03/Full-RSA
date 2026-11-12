package com.heavenbites.model;

import com.heavenbites.util.Text;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Order implements Billable, Jsonable {

    /** One line of an order (item snapshot, so later price changes don't alter old bills). */
    public static class Line {
        private final String itemId, name;
        private final double price;
        private final int qty;

        public Line(String itemId, String name, double price, int qty) {
            this.itemId = itemId;
            this.name = name;
            this.price = price;
            this.qty = qty;
        }

        private String encode() {
            return itemId + "^" + Text.clean(name) + "^" + price + "^" + qty;
        }

        private static Line decode(String s) {
            String[] f = s.split("\\^");
            return new Line(f[0], f[1], Double.parseDouble(f[2]), Integer.parseInt(f[3]));
        }
    }

    private final String id, customerId, customerName, type, info, createdAt;
    private String status, billNo, payMethod;
    private boolean paid;
    private final List<Line> lines = new ArrayList<>();

    // Constructor for a new order
    public Order(String id, String customerId, String customerName, String type, String info) {
        this(id, customerId, customerName, type, info, "Placed", now(), false, "", "");
    }

    // Constructor used when loading from file
    private Order(String id, String customerId, String customerName, String type, String info,
                  String status, String createdAt, boolean paid, String billNo, String payMethod) {
        this.id = id;
        this.customerId = customerId;
        this.customerName = customerName;
        this.type = type;
        this.info = info;
        this.status = status;
        this.createdAt = createdAt;
        this.paid = paid;
        this.billNo = billNo;
        this.payMethod = payMethod;
    }

    private static String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH));
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    public void addLine(Line l) { lines.add(l); }
    public List<Line> getLines() { return lines; }

    public String getId() { return id; }
    public String getCustomerId() { return customerId; }
    public String getType() { return type; }
    public String getStatus() { return status; }
    public String getBillNo() { return billNo; }
    public boolean isPaid() { return paid; }

    public void setStatus(String status) { this.status = status; }
    public void setBillNo(String billNo) { this.billNo = billNo; }
    public void markPaid(String method) { this.paid = true; this.payMethod = method; }

    public boolean isFinal() {
        return status.equals("Served") || status.equals("Delivered") || status.equals("Cancelled");
    }

    @Override
    public double getSubtotal() {
        double s = 0;
        for (Line l : lines) s += l.price * l.qty;
        return round2(s);
    }

    @Override
    public double getTax() { return round2(getSubtotal() * TAX_RATE); }

    @Override
    public double getTotal() { return round2(getSubtotal() + getTax()); }

    public String toLine() {
        StringBuilder items = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) items.append("~");
            items.append(lines.get(i).encode());
        }
        return String.join("|", id, customerId, Text.clean(customerName), type, info, status,
                createdAt, String.valueOf(paid), billNo, payMethod, items.toString());
    }

    public static Order fromLine(String line) {
        String[] f = line.split("\\|", -1);
        Order o = new Order(f[0], f[1], f[2], f[3], f[4], f[5], f[6],
                Boolean.parseBoolean(f[7]), f[8], f[9]);
        if (!f[10].isEmpty()) {
            for (String part : f[10].split("~")) o.lines.add(Line.decode(part));
        }
        return o;
    }

    @Override
    public String toJson() {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"id\":").append(Text.q(id));
        sb.append(",\"customerId\":").append(Text.q(customerId));
        sb.append(",\"customerName\":").append(Text.q(customerName));
        sb.append(",\"type\":").append(Text.q(type));
        sb.append(",\"info\":").append(Text.q(info));
        sb.append(",\"status\":").append(Text.q(status));
        sb.append(",\"createdAt\":").append(Text.q(createdAt));
        sb.append(",\"paid\":").append(paid);
        sb.append(",\"billNo\":").append(Text.q(billNo));
        sb.append(",\"payMethod\":").append(Text.q(payMethod));
        sb.append(",\"subtotal\":").append(getSubtotal());
        sb.append(",\"tax\":").append(getTax());
        sb.append(",\"total\":").append(getTotal());
        sb.append(",\"lines\":[");
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) sb.append(",");
            Line l = lines.get(i);
            sb.append("{\"itemId\":").append(Text.q(l.itemId))
              .append(",\"name\":").append(Text.q(l.name))
              .append(",\"price\":").append(l.price)
              .append(",\"qty\":").append(l.qty).append("}");
        }
        return sb.append("]}").toString();
    }
}