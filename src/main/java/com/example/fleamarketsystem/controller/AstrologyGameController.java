package com.example.fleamarketsystem.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AstrologyGameController {

    @GetMapping("/game")
    public String showGame() {
        return "astrology_game"; // templates/astrology_game.html
    }
}