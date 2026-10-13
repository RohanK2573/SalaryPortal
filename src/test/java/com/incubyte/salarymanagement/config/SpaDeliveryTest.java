package com.incubyte.salarymanagement.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.MockMvc;

class SpaDeliveryTest {
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new SpaController()).build();
    }

    @Test
    void knownUiRoutesForwardToReactEntryPoint() throws Exception {
        for (String path : new String[] {
                "/", "/signin", "/employees", "/employees/new", "/employees/2c4c9b34-2eb1-4d79-a719-00db0cb19c8e",
                "/reports", "/imports", "/imports/2c4c9b34-2eb1-4d79-a719-00db0cb19c8e"
        }) {
            mvc.perform(get(path))
                    .andExpect(status().isOk())
                    .andExpect(forwardedUrl("/index.html"));
        }
    }

    @Test
    void apiAndAssetPathsDoNotFallThroughToTheSpa() throws Exception {
        mvc.perform(get("/assets/missing.js")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/employees")).andExpect(status().isNotFound());
    }
}
