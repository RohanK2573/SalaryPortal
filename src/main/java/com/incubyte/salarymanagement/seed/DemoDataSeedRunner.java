package com.incubyte.salarymanagement.seed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.demo-seed", name = "enabled", havingValue = "true")
public class DemoDataSeedRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoDataSeedRunner.class);
    private final DemoDataSeedService seedService;

    public DemoDataSeedRunner(DemoDataSeedService seedService) {
        this.seedService = seedService;
    }

    @Override
    public void run(ApplicationArguments args) {
        int inserted = seedService.seed();
        if (inserted == 0) {
            log.info("Deterministic demo employee dataset is already present; no records changed.");
        } else {
            log.info("Created {} deterministic synthetic demo employees and current salary records.", inserted);
        }
    }
}
