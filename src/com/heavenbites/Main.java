package com.heavenbites;

import com.heavenbites.server.ApiHandler;
import com.heavenbites.server.StaticHandler;
import com.heavenbites.service.AutoBackup;
import com.heavenbites.service.DataStore;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) throws IOException {
        DataStore.getInstance();                       // load data files
        new AutoBackup().start();                      // background thread

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/api/", new ApiHandler());
        server.createContext("/", new StaticHandler());
        server.setExecutor(Executors.newFixedThreadPool(8));   // handles requests on 8 threads
        server.start();

        System.out.println("Heaven Bites running at http://localhost:8080");
    }
}