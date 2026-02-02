package com.example.fleamarketsystem.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ClockController {
	@GetMapping("/clock")
    public String showClock() {
        return "clock"; // templates/clock.html を表示
    }
	
	@GetMapping("/play")
    public String showPortal() {
        return "game_portal"; // templates/game_portal.html
    }
	
	@GetMapping("/rules/penalty")
    public String showPenaltyRules() {
        return "penalty_rules"; // templates/penalty_rules.html
    }
}
