package com.example.MarketS.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ErrorControllerCustom {

    @GetMapping("/error")
    public String handleError() {
        return "login";
    }
}
