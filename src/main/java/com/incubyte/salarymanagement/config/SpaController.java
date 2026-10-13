package com.incubyte.salarymanagement.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Forwards known browser routes to the React entry point. API and asset paths stay under their own handlers. */
@Controller
public class SpaController {

    @GetMapping({
            "/",
            "/signin",
            "/employees",
            "/employees/new",
            "/employees/{id}",
            "/reports",
            "/imports",
            "/imports/{id}"
    })
    public String forwardToApplication() {
        return "forward:/index.html";
    }
}
