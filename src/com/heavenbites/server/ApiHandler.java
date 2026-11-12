package com.heavenbites.server;

import com.heavenbites.exception.RestaurantException;
import com.heavenbites.model.Jsonable;
import com.heavenbites.model.Person;
import com.heavenbites.service.DataStore;
import com.heavenbites.util.Text;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ApiHandler implements HttpHandler {
    private static final String OK = "{\"ok\":true}";
    private final DataStore db = DataStore.getInstance();
    private final Map<String, Person> sessions = new ConcurrentHashMap<>();

    @Override
    public void handle(HttpExchange ex) throws IOException {
        int code = 200;
        String json;
        try {
            json = route(ex);
        } catch (RestaurantException e) {
            code = e.getCode();
            json = "{\"error\":" + Text.q(e.getMessage()) + "}";
        } catch (Exception e) {
            e.printStackTrace();
            code = 500;
            json = "{\"error\":\"Server error\"}";
        }
        byte[] out = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, out.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(out);
        }
    }

    private String route(HttpExchange ex) throws Exception {
        String path = ex.getRequestURI().getPath();
        Map<String, String> p = parse(ex.getRequestURI().getRawQuery());
        if ("POST".equals(ex.getRequestMethod())) {
            p.putAll(parse(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
        }
        String token = ex.getRequestHeaders().getFirst("X-Token");
        Person user = token == null ? null : sessions.get(token);

        switch (path) {
            // ----- public -----
            case "/api/login":
                return session(db.login(p.get("email"), p.get("password"), p.get("role")));
            case "/api/register":
                return session(db.register(p.get("name"), p.get("email"), p.get("password"), "customer"));
            case "/api/logout":
                if (token != null) sessions.remove(token);
                return OK;
            case "/api/menu":
                return items(db.getMenu());

            // ----- admin -----
            case "/api/menu/add":
                need(user, "admin");
                return db.addItem(p).toJson();
            case "/api/menu/update":
                need(user, "admin");
                return db.updateItem(p.get("id"), p).toJson();
            case "/api/menu/delete":
                need(user, "admin");
                db.deleteItem(p.get("id"));
                return OK;
            case "/api/stats":
                need(user, "admin");
                return db.stats();
            case "/api/users":
                need(user, "admin");
                return items(db.getUsers());
            case "/api/staff/add":
                need(user, "admin");
                db.register(p.get("name"), p.get("email"), p.get("password"), p.get("role"));
                return OK;

            // ----- orders -----
            case "/api/orders/place":
                need(user, "customer", "waiter", "cashier", "admin");
                return db.placeOrder(user, p.get("customerName"), p.get("type"), p.get("info"), p.get("items")).toJson();
            case "/api/orders/my":
                need(user, "customer");
                return items(db.getOrders(user));
            case "/api/orders":
                need(user, "admin", "waiter", "cashier");
                return items(db.getOrders(user));
            case "/api/orders/status":
                need(user, "admin", "waiter", "cashier");
                return db.updateOrderStatus(user, p.get("id"), p.get("status")).toJson();
            case "/api/orders/cancel":
                need(user, "customer");
                return db.cancelOrder(user, p.get("id")).toJson();

            // ----- billing -----
            case "/api/bills/generate":
                need(user, "cashier", "admin");
                return db.generateBill(p.get("id")).toJson();
            case "/api/bills/pay":
                need(user, "cashier", "admin");
                return db.payBill(p.get("id"), p.get("method")).toJson();

            // ----- bookings -----
            case "/api/bookings/create":
                need(user, "customer");
                return db.book(user, p.get("phone"), p.get("date"), p.get("time"), p.get("guests")).toJson();
            case "/api/bookings/my":
                need(user, "customer");
                return items(db.getBookings(user));
            case "/api/bookings/all":
                need(user, "admin", "waiter", "cashier");
                return items(db.getBookings(user));
            case "/api/bookings/status":
                need(user, "admin", "waiter", "cashier");
                return db.updateBookingStatus(p.get("id"), p.get("status")).toJson();
            case "/api/bookings/cancel":
                need(user, "customer");
                return db.cancelBooking(user, p.get("id")).toJson();

            default:
                throw new RestaurantException("Unknown API: " + path, 404);
        }
    }

    private Person need(Person u, String... roles) throws RestaurantException {
        if (u == null) throw new RestaurantException("Please login again", 401);
        for (String r : roles) if (r.equals(u.getRole())) return u;
        throw new RestaurantException("You are not allowed to do this", 403);
    }

    private String session(Person u) {
        String t = UUID.randomUUID().toString();
        sessions.put(t, u);
        return "{\"token\":" + Text.q(t) + ",\"user\":" + u.toJson() + "}";
    }

    private String items(List<? extends Jsonable> list) {
        synchronized (db) {   // same lock DataStore uses, so we never read half-updated data
            StringBuilder sb = new StringBuilder("{\"items\":[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(list.get(i).toJson());
            }
            return sb.append("]}").toString();
        }
    }

    private Map<String, String> parse(String s) {
        Map<String, String> m = new HashMap<>();
        if (s == null || s.isEmpty()) return m;
        for (String pair : s.split("&")) {
            int i = pair.indexOf('=');
            String k = i < 0 ? pair : pair.substring(0, i);
            String v = i < 0 ? "" : pair.substring(i + 1);
            m.put(URLDecoder.decode(k, StandardCharsets.UTF_8), URLDecoder.decode(v, StandardCharsets.UTF_8));
        }
        return m;
    }
}