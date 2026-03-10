package com.taxworkbench.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local-seed")
public class LargeSeedCliRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(LargeSeedCliRunner.class);

    private final LargeSeedLoader largeSeedLoader;
    private final ConfigurableApplicationContext applicationContext;

    public LargeSeedCliRunner(
            LargeSeedLoader largeSeedLoader,
            ConfigurableApplicationContext applicationContext
    ) {
        this.largeSeedLoader = largeSeedLoader;
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Starting large seed in one-shot mode.");
        boolean started = largeSeedLoader.runSync("cli-oneshot");
        LargeSeedLoader.SeedStatus status = largeSeedLoader.getStatus();
        if (!started) {
            log.warn("Large seed did not start. state={}, message={}", status.state(), status.message());
        }

        int exitCode = "COMPLETED".equals(status.state()) ? 0 : 1;
        log.info("Large seed one-shot finished. state={}, message={}", status.state(), status.message());
        SpringApplication.exit(applicationContext, () -> exitCode);
        System.exit(exitCode);
    }
}
