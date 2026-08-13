package com.minitrello;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Application entrypoint.
 *
 * Async is enabled for non-blocking side effects (email sending, websocket
 * broadcasts) so request threads are never held up by them.
 * Scheduling is enabled for background jobs (soft-delete purge, position
 * rebalancing, expired refresh token cleanup — see Sprint 10).
 */
@EnableAsync
@EnableScheduling
@SpringBootApplication
public class MiniTrelloEnterpriseApplication {

    public static void main(String[] args) {
        SpringApplication.run(MiniTrelloEnterpriseApplication.class, args);
    }
}
