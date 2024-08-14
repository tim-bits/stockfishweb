package com.stockfishweb.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.logging.Logger;

@EnableAsync
@SpringBootApplication
public class StockfishWebApplication {

	private static Logger log = Logger.getLogger(StockfishWebApplication.class.getSimpleName());

	public static void main(String[] args) {

		SpringApplication application = new SpringApplication(StockfishWebApplication.class);
		application.addListeners((ApplicationListener<ContextClosedEvent>) event -> {
			log.info("Shutdown process initiated...");
			try {
				Thread.sleep(5000);//TimeUnit.MINUTES.toMillis(5));
			} catch (InterruptedException e) {
				log.severe("Exception is thrown during the ContextClosedEvent: " + e.getClass() + " "  + e.getLocalizedMessage());
			}
			log.info("Graceful Shutdown is processed successfully");
		});
		application.run(args);

	}
}
