package com.example.fleamarketsystem.controller;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.fleamarketsystem.entity.Admin;
import com.example.fleamarketsystem.entity.User;
import com.example.fleamarketsystem.repository.UserRepository; // AdminRepositoryは不要なら削除
import com.example.fleamarketsystem.service.ReportService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class LoginController {
   
    private final UserRepository userRepository;
    private final ReportService reportService;

    @GetMapping("/login")
    public String login() {
        return "login"; 
    }

    // 一時利用停止画面
    @GetMapping("/banned")
    public String banned(@RequestParam(required = false) String reason, Model model, HttpServletRequest request) {
        model.addAttribute("reason", reason != null ? URLDecoder.decode(reason, StandardCharsets.UTF_8) : "BANされています。");
        
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("aoaoa");
        
        // 直打ちなどでセッションがない場合はログイン画面へ戻す
        if (user == null) {
            return "redirect:/login";
        }
        
        // セッションからは消しておく（リロード対策）
        // ※ 異議申し立て失敗時などに残したい場合は消さない方がいいかもですが、一旦元のロジック通り
        session.removeAttribute("aoaoa"); 
        
        model.addAttribute("us", user);
        return "banned"; 
    }

    // 永久BAN画面
    @GetMapping("/a")
    public String showPermanentBanPage(Model model, HttpServletRequest request) {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("aoaoa");
        
        if (user == null) {
            return "redirect:/login";
        }
        
        session.removeAttribute("aoaoa");
        model.addAttribute("us", user);
        return "a"; // templates/a.html (追放画面)
    }

    // 異議申し立て処理
    @PostMapping("/igimousitate/{id}")
    public String submitObjection(@PathVariable Long id, @RequestParam("description") String description, RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        
        Admin admin = new Admin();
        admin.setUserId(user);
        admin.setAction(2); // 2 = 異議申し立て と仮定
        admin.setDescription(description);
        
        try {
            reportService.saveAdmin(admin);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        // 完了したらログイン画面へ戻す
        return "redirect:/login?objection_sent";
    }
}

