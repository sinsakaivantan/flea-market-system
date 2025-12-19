package com.example.fleamarketsystem.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.fleamarketsystem.service.AppOrderService;
import com.example.fleamarketsystem.service.ItemService;
import com.example.fleamarketsystem.service.RenrakuService;

import jakarta.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

	private final ItemService itemService;
	private final AppOrderService appOrderService;
	private final RenrakuService renrakuService;
	
	public AdminController(ItemService itemService, AppOrderService appOrderService, RenrakuService renrakuService) {
		this.itemService = itemService;
		this.appOrderService = appOrderService;
		this.renrakuService = renrakuService;
	}

	@GetMapping("/items")
	public String manageItems(Model model) {
		model.addAttribute("items", itemService.getAllItems());
		return "admin_items";
	}
	
	@GetMapping("/renraku")
	public String renraku(Model model) {
		model.addAttribute("admin", renrakuService.getAllAdmin());
		return "renraku";
	}

	@PostMapping("/items/{id}/delete")
	public String deleteItemByAdmin(@PathVariable("id") Long itemId) {
		itemService.deleteItem(itemId);
		return "redirect:/admin/items?success=deleted";
	}

	@GetMapping("/statistics")
	public String showStatistics(
			@RequestParam(value = "startDate", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@RequestParam(value = "endDate", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
			Model model) {

		if (startDate == null)
			startDate = LocalDate.now().minusMonths(1);
		if (endDate == null)
			endDate = LocalDate.now();

		model.addAttribute("startDate", startDate);
		model.addAttribute("endDate", endDate);
		model.addAttribute("totalSales", appOrderService.getTotalSales(startDate, endDate));
		model.addAttribute("orderCountByStatus", appOrderService.getOrderCountByStatus(startDate, endDate));
		return "admin_statistics";
	}

	@GetMapping("/statistics/csv")
	public void exportStatisticsCsv(
			@RequestParam(value = "startDate", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@RequestParam(value = "endDate", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
			HttpServletResponse response) throws IOException {

		if (startDate == null)
			startDate = LocalDate.now().minusMonths(1);
		if (endDate == null)
			endDate = LocalDate.now();

		response.setContentType("text/csv; charset=UTF-8");
		response.setHeader("Content-Disposition", "attachment; filename=\"flea_market_statistics.csv\"");

		try (PrintWriter writer = response.getWriter()) {
			writer.append("統計期間: ").append(String.valueOf(startDate)).append(" から ").append(String.valueOf(endDate))
					.append("\n\n");
			writer.append("総売上: ").append(String.valueOf(appOrderService.getTotalSales(startDate, endDate)))
					.append("\n\n");
			writer.append("ステータス別注文数\n");
			appOrderService.getOrderCountByStatus(startDate, endDate)
					.forEach((status, count) -> writer.append(status).append(",").append(String.valueOf(count))
							.append("\n"));
		}
	}
}
