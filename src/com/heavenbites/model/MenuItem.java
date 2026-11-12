package com.heavenbites.model;

import com.heavenbites.util.Text;
import java.util.UUID;

public class MenuItem implements Jsonable {
    private final String id;
    private String name, category, description, image;
    private double price;
    private boolean available;
    private int maxPerOrder;

    public MenuItem(String id, String name, String category, double price,
                    String description, String image, boolean available, int maxPerOrder) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.description = description;
        this.image = image;
        this.available = available;
        this.maxPerOrder = maxPerOrder;
    }

    // Overloaded constructor for new items (id generated, available, max 5)
    public MenuItem(String name, String category, double price, String description, String image) {
        this("m" + UUID.randomUUID().toString().substring(0, 6),
                name, category, price, description, image, true, 5);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public double getPrice() { return price; }
    public boolean isAvailable() { return available; }
    public int getMaxPerOrder() { return maxPerOrder; }

    public void setName(String name) { this.name = name; }
    public void setCategory(String category) { this.category = category; }
    public void setPrice(double price) { this.price = price; }
    public void setDescription(String description) { this.description = description; }
    public void setImage(String image) { this.image = image; }
    public void setAvailable(boolean available) { this.available = available; }
    public void setMaxPerOrder(int maxPerOrder) { this.maxPerOrder = maxPerOrder; }

    public String toLine() {
        return String.join("|", id, name, category, String.valueOf(price), description,
                image, String.valueOf(available), String.valueOf(maxPerOrder));
    }

    public static MenuItem fromLine(String line) {
        String[] f = line.split("\\|", -1);
        return new MenuItem(f[0], f[1], f[2], Double.parseDouble(f[3]), f[4], f[5],
                Boolean.parseBoolean(f[6]), Integer.parseInt(f[7]));
    }

    @Override
    public String toJson() {
        return new StringBuilder("{")
                .append("\"id\":").append(Text.q(id))
                .append(",\"name\":").append(Text.q(name))
                .append(",\"category\":").append(Text.q(category))
                .append(",\"description\":").append(Text.q(description))
                .append(",\"image\":").append(Text.q(image))
                .append(",\"price\":").append(price)
                .append(",\"available\":").append(available)
                .append(",\"maxPerOrder\":").append(maxPerOrder)
                .append("}").toString();
    }
}