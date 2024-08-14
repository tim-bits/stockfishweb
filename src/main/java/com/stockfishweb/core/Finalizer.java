package com.stockfishweb.core;

import com.stockfishweb.core.engine.StockfishClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;

@Component
public class Finalizer {

    @Autowired
    private StockfishClient client;

    @PreDestroy
    public void destroy() {
        System.out.println("Callback triggered - @PreDestroy.");
        client.close();
    }
}
