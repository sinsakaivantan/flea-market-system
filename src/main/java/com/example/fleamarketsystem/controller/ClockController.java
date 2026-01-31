package com.example.fleamarketsystem.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ClockController {
	@GetMapping("/clock")
    public String showClock() {
        return "clock"; // templates/clock.html を表示
    }
}
