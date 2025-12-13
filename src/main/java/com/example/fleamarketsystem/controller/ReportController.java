package com.example.fleamarketsystem.controller;

import java.io.IOException;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.fleamarketsystem.entity.Admin;
import com.example.fleamarketsystem.entity.User;
import com.example.fleamarketsystem.service.ReportService;

@Controller
@RequestMapping("/report")
public class ReportController {
	 private final ReportService reportService;
	 public ReportController(ReportService reportService) {
			this.reportService = reportService;
		}
	 
	 @PostMapping("/{id}")
	 public String sikkou(
		@PathVariable("id") User userId,
		 @RequestParam("description") String description,
		 RedirectAttributes redirectAttributes) {
		 Admin admin = new Admin();
		 admin.setUserId(userId);
		 admin.setAction(1);
		 admin.setDescription(description);
		 try {
			reportService.saveAdmin(admin);
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		 return "redirect:/items";
	 }
	 
}
