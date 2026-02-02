package com.example.fleamarketsystem.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.fleamarketsystem.entity.User;
import com.example.fleamarketsystem.entity.UserComplaint;
import com.example.fleamarketsystem.service.ReportService;
import com.example.fleamarketsystem.service.UserService;

@Controller
@RequestMapping("/report")
public class ReportController {
	private final ReportService reportService;
	private final UserService userService;

	public ReportController(ReportService reportService, UserService userService) {
		this.reportService = reportService;
		this.userService = userService;
	}

	@PostMapping("/{id}")
	public String reportUser(
			@PathVariable("id") Long reportedUserId,
			@RequestParam("description") String description,
			@AuthenticationPrincipal UserDetails userDetails,
			RedirectAttributes redirectAttributes) {
		User reporter = userService.getUserByEmail(userDetails.getUsername())
				.orElse(null);
		if (reporter == null) {
			redirectAttributes.addFlashAttribute("errorMessage", "ログインしてください。");
			return "redirect:/login";
		}
		UserComplaint complaint = new UserComplaint();
		complaint.setReportedUserId(reportedUserId);
		complaint.setReporterUserId(reporter.getId());
		complaint.setReason(description != null ? description : "");
		reportService.saveUserComplaint(complaint);
		redirectAttributes.addFlashAttribute("successMessage", "通報を受け付けました。");
		return "redirect:/items";
	}
}