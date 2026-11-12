package com.heavenbites.service;

import com.heavenbites.exception.RestaurantException;
import com.heavenbites.model.*;
import com.heavenbites.util.FileUtil;
import com.heavenbites.util.Text;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

public class DataStore {
    private static DataStore instance;

    public static synchronized DataStore getInstance() {
        if (instance == null) instance = new DataStore();
        return instance;
    }

    public static final String DIR = "data";
    private static final int[] TABLES = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
    private static final String[] CATEGORIES = {"Starters", "Mains", "Desserts", "Drinks"};
    private static final String[] ROLES = {"admin", "waiter", "cashier", "customer"};
    private static final String[] ORDER_STATUSES =
            {"Placed", "Preparing", "Ready", "Out for Delivery", "Served", "Delivered", "Cancelled"};
    private static final String[] BOOKING_STATUSES = {"Pending", "Confirmed", "Seated", "Completed", "Cancelled"};
    private static final String[] PAY_METHODS = {"Cash", "Card", "UPI"};

    private final List<Person> users = new ArrayList<>();
    private final List<MenuItem> menu = new ArrayList<>();
    private final List<Order> orders = new ArrayList<>();
    private final List<Booking> bookings = new ArrayList<>();

    private DataStore() {
        new File(DIR).mkdirs();
        for (String l : FileUtil.read(DIR + "/users.txt")) users.add(Person.fromLine(l));
        for (String l : FileUtil.read(DIR + "/menu.txt")) menu.add(MenuItem.fromLine(l));
        for (String l : FileUtil.read(DIR + "/orders.txt")) orders.add(Order.fromLine(l));
        for (String l : FileUtil.read(DIR + "/bookings.txt")) bookings.add(Booking.fromLine(l));
        if (users.isEmpty()) { seedUsers(); saveUsers(); }
        if (menu.isEmpty()) { seedMenu(); saveMenu(); }
    }

    // ---------- seed data ----------
    private void seedUsers() {
        users.add(new Admin("Restaurant Admin", "admin@heavenbites.com", "Admin@123"));
        users.add(new Waiter("Ravi Waiter", "waiter@heavenbites.com", "Waiter@123"));
        users.add(new Cashier("Priya Cashier", "cashier@heavenbites.com", "Cashier@123"));
        users.add(new Customer("Demo Customer", "customer@heavenbites.com", "Customer@123"));
    }

    private void seed(String id, String name, String cat, double price, String desc, int max, boolean avail) {
        menu.add(new MenuItem(id, name, cat, price, desc, "", avail, max));
    }

    private void seedMenu() {
        seed("m1", "Paneer Tikka", "Starters", 260, "Tandoor-grilled cottage cheese, mint chutney, onion rings", 5, true);
        seed("m2", "Hara Bhara Kabab", "Starters", 200, "Spinach, peas & potato patties, tamarind dip", 5, false);
        seed("m3", "Samosa Chaat", "Starters", 160, "Crispy samosas, yogurt, sev, chutneys, pomegranate", 6, true);
        seed("m4", "Dahi Puri", "Starters", 140, "Hollow puris, spiced potato, sweet curd, chaat masala", 4, true);
        seed("m5", "Butter Chicken", "Mains", 320, "Slow-cooked chicken in a velvety tomato-cream sauce with warm spices", 4, true);
        seed("m6", "Dal Makhani", "Mains", 240, "Black lentils simmered overnight with butter and cream", 4, true);
        seed("m7", "Paneer Butter Masala", "Mains", 280, "Cottage cheese cubes in rich tomato-cashew gravy", 4, true);
        seed("m8", "Hyderabadi Biryani", "Mains", 300, "Fragrant basmati layered with spiced masala and saffron", 4, true);
        seed("m9", "Garlic Naan", "Mains", 90, "Soft leavened flatbread brushed with garlic butter, baked in our clay tandoor", 8, true);
        seed("m10", "Gulab Jamun", "Desserts", 120, "Warm milk dumplings in cardamom sugar syrup", 6, true);
        seed("m11", "Rasmalai", "Desserts", 150, "Soft cheese discs in saffron-pistachio milk", 6, true);
        seed("m12", "Kulfi Falooda", "Desserts", 130, "Traditional Indian ice cream with vermicelli and rose syrup", 4, true);
        seed("m13", "Masala Chai", "Drinks", 60, "Spiced Assam tea with ginger and cardamom", 8, true);
        seed("m14", "Mango Lassi", "Drinks", 100, "Thick yogurt shake with Alphonso mango", 6, true);
        seed("m15", "Fresh Lime Soda", "Drinks", 80, "Sweet, salted or mixed", 6, true);
        seed("m16", "Cold Coffee", "Drinks", 120, "Chilled coffee blended with ice cream", 5, true);
    }

    // ---------- save helpers ----------
    private void saveUsers() {
        List<String> l = new ArrayList<>();
        for (Person p : users) l.add(p.toLine());
        FileUtil.write(DIR + "/users.txt", l);
    }

    private void saveMenu() {
        List<String> l = new ArrayList<>();
        for (MenuItem m : menu) l.add(m.toLine());
        FileUtil.write(DIR + "/menu.txt", l);
    }

    private void saveOrders() {
        List<String> l = new ArrayList<>();
        for (Order o : orders) l.add(o.toLine());
        FileUtil.write(DIR + "/orders.txt", l);
    }

    private void saveBookings() {
        List<String> l = new ArrayList<>();
        for (Booking b : bookings) l.add(b.toLine());
        FileUtil.write(DIR + "/bookings.txt", l);
    }

    private static boolean in(String v, String[] arr) {
        for (String s : arr) if (s.equals(v)) return true;
        return false;
    }

    private static double parsePrice(String s) throws RestaurantException {
        try {
            double v = Double.parseDouble(s.trim());
            if (v < 0 || v > 100000) throw new RestaurantException("Price out of range");
            return v;
        } catch (NumberFormatException | NullPointerException e) {
            throw new RestaurantException("Price must be a number");
        }
    }

    private static int parseInt(String s, int min, int max, String field) throws RestaurantException {
        try {
            int v = Integer.parseInt(s.trim());
            if (v < min || v > max) throw new RestaurantException(field + " must be between " + min + " and " + max);
            return v;
        } catch (NumberFormatException | NullPointerException e) {
            throw new RestaurantException(field + " must be a whole number");
        }
    }

    // ---------- users ----------
    public synchronized Person login(String email, String pw, String role) throws RestaurantException {
        if (email == null || pw == null || role == null) throw new RestaurantException("Please fill all fields");
        for (Person p : users) {
            if (p.getEmail().equalsIgnoreCase(email.trim()) && p.getRole().equals(role) && p.checkPassword(pw)) {
                return p;
            }
        }
        throw new RestaurantException("Invalid email, password or role", 401);
    }

    public synchronized Person register(String name, String email, String pw, String role) throws RestaurantException {
        name = Text.clean(name);
        email = Text.clean(email).toLowerCase();
        if (name.isEmpty()) throw new RestaurantException("Name is required");
        if (!in(role, ROLES)) throw new RestaurantException("Invalid role");
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) throw new RestaurantException("Enter a valid email");
        if (pw == null || pw.length() < 6 || !pw.matches(".*[^A-Za-z0-9].*")) {
            throw new RestaurantException("Password needs 6+ characters and at least one special character");
        }
        for (Person p : users) {
            if (p.getEmail().equalsIgnoreCase(email)) throw new RestaurantException("Email already registered");
        }
        Person p = Person.createNew(name, email, pw, role);
        users.add(p);
        saveUsers();
        return p;
    }

    public synchronized List<Person> getUsers() { return new ArrayList<>(users); }

    // ---------- menu ----------
    public synchronized List<MenuItem> getMenu() { return new ArrayList<>(menu); }

    private MenuItem findItem(String id) throws RestaurantException {
        for (MenuItem m : menu) if (m.getId().equals(id)) return m;
        throw new RestaurantException("Menu item not found", 404);
    }

    public synchronized MenuItem addItem(Map<String, String> p) throws RestaurantException {
        String name = Text.clean(p.get("name"));
        String cat = Text.clean(p.get("category"));
        if (name.isEmpty()) throw new RestaurantException("Item name is required");
        if (!in(cat, CATEGORIES)) throw new RestaurantException("Choose a valid category");
        MenuItem m = new MenuItem(name, cat, parsePrice(p.get("price")),
                Text.clean(p.get("description")), Text.clean(p.get("image")));
        if (p.get("maxPerOrder") != null) m.setMaxPerOrder(parseInt(p.get("maxPerOrder"), 1, 20, "Max per order"));
        menu.add(m);
        saveMenu();
        return m;
    }

    public synchronized MenuItem updateItem(String id, Map<String, String> p) throws RestaurantException {
        MenuItem m = findItem(id);
        if (p.containsKey("name")) {
            String n = Text.clean(p.get("name"));
            if (n.isEmpty()) throw new RestaurantException("Item name is required");
            m.setName(n);
        }
        if (p.containsKey("category")) {
            String c = Text.clean(p.get("category"));
            if (!in(c, CATEGORIES)) throw new RestaurantException("Choose a valid category");
            m.setCategory(c);
        }
        if (p.containsKey("price")) m.setPrice(parsePrice(p.get("price")));
        if (p.containsKey("description")) m.setDescription(Text.clean(p.get("description")));
        if (p.containsKey("image")) m.setImage(Text.clean(p.get("image")));
        if (p.containsKey("maxPerOrder")) m.setMaxPerOrder(parseInt(p.get("maxPerOrder"), 1, 20, "Max per order"));
        if (p.containsKey("available")) m.setAvailable(Boolean.parseBoolean(p.get("available")));
        saveMenu();
        return m;
    }

    public synchronized void deleteItem(String id) throws RestaurantException {
        menu.remove(findItem(id));
        saveMenu();
    }

    // ---------- orders ----------
    private Order findOrder(String id) throws RestaurantException {
        for (Order o : orders) if (o.getId().equals(id)) return o;
        throw new RestaurantException("Order not found", 404);
    }

    public synchronized Order placeOrder(Person by, String customerName, String type, String info,
                                         String itemsParam) throws RestaurantException {
        if (type == null || !(type.equals("Dine-in") || type.equals("Takeaway") || type.equals("Delivery"))) {
            throw new RestaurantException("Choose an order type");
        }
        info = Text.clean(info);
        if (type.equals("Dine-in") && info.isEmpty()) throw new RestaurantException("Select a table number");
        if (type.equals("Delivery") && info.isEmpty()) throw new RestaurantException("Delivery address is required");
        if (type.equals("Takeaway")) info = "";

        boolean isCustomer = by.getRole().equals("customer");
        String cid = isCustomer ? by.getId() : "walk-in";
        String cname = isCustomer ? by.getName()
                : (Text.clean(customerName).isEmpty() ? "Walk-in" : Text.clean(customerName));

        Order o = new Order(String.valueOf(1001 + orders.size()), cid, cname, type, info);
        if (itemsParam == null) itemsParam = "";
        for (String part : itemsParam.split(",")) {
            String[] kv = part.split(":");
            if (kv.length != 2) continue;
            MenuItem m = findItem(kv[0]);
            int qty = parseInt(kv[1], 1, 50, "Quantity");
            if (!m.isAvailable()) throw new RestaurantException(m.getName() + " is currently unavailable");
            if (qty > m.getMaxPerOrder()) {
                throw new RestaurantException("Max " + m.getMaxPerOrder() + " per order for " + m.getName());
            }
            // price always comes from the server, never from the browser
            o.addLine(new Order.Line(m.getId(), m.getName(), m.getPrice(), qty));
        }
        if (o.getLines().isEmpty()) throw new RestaurantException("Your cart is empty");
        orders.add(o);
        saveOrders();
        return o;
    }

    public synchronized List<Order> getOrders(Person by) {
        List<Order> out = new ArrayList<>();
        for (Order o : orders) {
            if (!by.getRole().equals("customer") || o.getCustomerId().equals(by.getId())) out.add(o);
        }
        Collections.reverse(out);   // newest first
        return out;
    }

    public synchronized Order updateOrderStatus(Person by, String id, String status) throws RestaurantException {
        Order o = findOrder(id);
        if (!in(status, ORDER_STATUSES)) throw new RestaurantException("Unknown status");
        if (by.getRole().equals("waiter")) {
            boolean ok = o.getType().equals("Dine-in")
                    && (status.equals("Preparing") || status.equals("Ready") || status.equals("Served"));
            if (!ok) throw new RestaurantException("Waiters can only update dine-in orders (Preparing / Ready / Served)", 403);
        }
        if (o.isFinal() && !by.getRole().equals("admin")) throw new RestaurantException("This order is already closed");
        o.setStatus(status);
        saveOrders();
        return o;
    }

    public synchronized Order cancelOrder(Person by, String id) throws RestaurantException {
        Order o = findOrder(id);
        if (!o.getCustomerId().equals(by.getId())) throw new RestaurantException("Not your order", 403);
        if (!o.getStatus().equals("Placed")) throw new RestaurantException("Order is already being prepared");
        o.setStatus("Cancelled");
        saveOrders();
        return o;
    }

    // ---------- billing ----------
    public synchronized Order generateBill(String id) throws RestaurantException {
        Order o = findOrder(id);
        if (o.getStatus().equals("Cancelled")) throw new RestaurantException("Cancelled orders cannot be billed");
        if (o.getBillNo().isEmpty()) {
            int n = 1000;
            for (Order x : orders) if (!x.getBillNo().isEmpty()) n++;
            o.setBillNo("HB-" + (n + 1));
            saveOrders();
        }
        return o;
    }

    public synchronized Order payBill(String id, String method) throws RestaurantException {
        Order o = findOrder(id);
        if (o.getBillNo().isEmpty()) throw new RestaurantException("Generate the bill first");
        if (o.isPaid()) throw new RestaurantException("Already paid");
        if (!in(method, PAY_METHODS)) throw new RestaurantException("Choose Cash, Card or UPI");
        o.markPaid(method);
        saveOrders();
        return o;
    }

    // ---------- bookings ----------
    public synchronized Booking book(Person by, String phone, String date, String time, String guestsStr)
            throws RestaurantException {
        phone = Text.clean(phone);
        if (!phone.matches("[0-9+ -]{8,15}")) throw new RestaurantException("Enter a valid phone number");
        LocalDate d;
        try {
            d = LocalDate.parse(date);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new RestaurantException("Choose a valid date");
        }
        if (d.isBefore(LocalDate.now())) throw new RestaurantException("Date cannot be in the past");
        if (time == null || !time.matches("\\d{2}:\\d{2}")) throw new RestaurantException("Choose a time slot");
        int guests = parseInt(guestsStr, 1, 10, "Guests");

        int chosen = 0;
        for (int t : TABLES) {
            boolean taken = false;
            for (Booking b : bookings) {
                if (!b.getStatus().equals("Cancelled") && b.getDate().equals(date)
                        && b.getTime().equals(time) && b.getTableNo() == t) {
                    taken = true;
                    break;
                }
            }
            if (!taken) { chosen = t; break; }
        }
        if (chosen == 0) throw new RestaurantException("No tables free at that time. Please try another slot.");

        Booking b = new Booking("B" + (101 + bookings.size()), by.getId(), by.getName(),
                phone, date, time, guests, chosen, "Pending");
        bookings.add(b);
        saveBookings();
        return b;
    }

    public synchronized List<Booking> getBookings(Person by) {
        List<Booking> out = new ArrayList<>();
        for (Booking b : bookings) {
            if (!by.getRole().equals("customer") || b.getCustomerId().equals(by.getId())) out.add(b);
        }
        out.sort((a, b) -> (a.getDate() + a.getTime()).compareTo(b.getDate() + b.getTime()));
        return out;
    }

    private Booking findBooking(String id) throws RestaurantException {
        for (Booking b : bookings) if (b.getId().equals(id)) return b;
        throw new RestaurantException("Booking not found", 404);
    }

    public synchronized Booking updateBookingStatus(String id, String status) throws RestaurantException {
        Booking b = findBooking(id);
        if (!in(status, BOOKING_STATUSES)) throw new RestaurantException("Unknown status");
        b.setStatus(status);
        saveBookings();
        return b;
    }

    public synchronized Booking cancelBooking(Person by, String id) throws RestaurantException {
        Booking b = findBooking(id);
        if (!b.getCustomerId().equals(by.getId())) throw new RestaurantException("Not your booking", 403);
        if (!(b.getStatus().equals("Pending") || b.getStatus().equals("Confirmed"))) {
            throw new RestaurantException("This booking can no longer be cancelled");
        }
        b.setStatus("Cancelled");
        saveBookings();
        return b;
    }

    // ---------- stats ----------
    public synchronized String stats() {
        int active = 0, pendingBookings = 0;
        double revenue = 0;
        for (Order o : orders) {
            if (!o.isFinal()) active++;
            if (o.isPaid()) revenue += o.getTotal();
        }
        for (Booking b : bookings) if (b.getStatus().equals("Pending")) pendingBookings++;
        return "{\"totalOrders\":" + orders.size() + ",\"activeOrders\":" + active
                + ",\"revenue\":" + revenue + ",\"pendingBookings\":" + pendingBookings
                + ",\"menuItems\":" + menu.size() + ",\"users\":" + users.size() + "}";
    }
}