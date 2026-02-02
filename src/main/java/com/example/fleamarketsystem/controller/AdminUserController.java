// src/main/java/com/example/fleamarketsystem/controller/AdminUserController.java
package com.example.fleamarketsystem.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.fleamarketsystem.entity.Ban;
import com.example.fleamarketsystem.entity.User;
import com.example.fleamarketsystem.entity.UserComplaint;
import com.example.fleamarketsystem.repository.AdminRepository;
import com.example.fleamarketsystem.repository.BanRepository;
import com.example.fleamarketsystem.repository.UserRepository;
import com.example.fleamarketsystem.service.AdminUserService;
import com.example.fleamarketsystem.service.EmailService;

@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

	private final AdminUserService service;
	private final UserRepository users;
	private final BanRepository banRepository;
	private final AdminRepository adminRepository;
	private final EmailService emailService;

	public AdminUserController(AdminUserService service, UserRepository users, BanRepository banRepository,
			AdminRepository adminRepository, EmailService emailService) {
		this.service = service;
		this.adminRepository = adminRepository;
		this.users = users;
		this.banRepository = banRepository;
		this.emailService = emailService;
	}

	@GetMapping
	public String list(@RequestParam(value = "q", required = false) String q,
			@RequestParam(value = "sort", required = false, defaultValue = "id") String sort,
			Model model) {
		List<User> list = service.listAllUsers();

		if (StringUtils.hasText(q)) {
			String qq = q.toLowerCase();
			list = list.stream().filter(u -> (u.getName() != null && u.getName().toLowerCase().contains(qq)) ||
					(u.getEmail() != null && u.getEmail().toLowerCase().contains(qq))).toList();
		}

		list = switch (sort) {
		case "name" -> list.stream().sorted(Comparator.comparing(User::getName,
				Comparator.nullsLast(String::compareToIgnoreCase))).toList();
		case "email" -> list.stream().sorted(Comparator.comparing(User::getEmail,
				Comparator.nullsLast(String::compareToIgnoreCase))).toList();
		case "banned" -> list.stream().sorted(Comparator.comparing(User::isBanned).reversed()).toList();
		case "id" -> list.stream().sorted(Comparator.comparing(User::getId)).toList();
		default -> list.stream().sorted(Comparator.comparing(User::getId)).toList();
		};

		model.addAttribute("users", list);
		model.addAttribute("q", q);
		model.addAttribute("sort", sort);
		return "admin/users/list";
	}

	@GetMapping("/complaints")
	public String complaintsList(@RequestParam(value = "reportedUserId", required = false) Long reportedUserId,
			Model model) {
		List<UserComplaint> list = service.getAllComplaintsOrderByCreatedAtDesc();
		if (reportedUserId != null) {
		    list = list.stream()
		               .filter(c -> reportedUserId.equals(c.getReportedUserId().getId()))
		               .toList();
		}

		Set<Long> userIds = new HashSet<>();
		for (UserComplaint c : list) {
			userIds.add(c.getReporterUserId().getId());
			userIds.add(c.getReportedUserId().getId());
		}
		Map<Long, User> userMap = new HashMap<>();
		for (Long id : userIds) {
			users.findById(id).ifPresent(u -> userMap.put(id, u));
		}
		User detailUser = null;
		if (reportedUserId != null) {
			detailUser = users.findById(reportedUserId).orElse(null);
		}
		model.addAttribute("complaints", list);
		model.addAttribute("userMap", userMap);
		model.addAttribute("detailUser", detailUser);
		return "admin/complaints";
	}

	@GetMapping("/{id}")
	public String detail(@PathVariable Long id, Model model) {
		User user = service.findUser(id);
		Double avg = service.averageRating(id);
		long complaints = service.complaintCount(user);
		List<Ban> aiueo = banRepository.findAllByUserId(user);
		if (aiueo == null) {
			aiueo = java.util.Collections.emptyList();
		}
		Optional<Ban> banOpt = banRepository.findTopByUserIdOrderByEndDesc(user);
		if (banOpt.isPresent()) {
			Ban ban = banOpt.get();
			LocalDateTime now = LocalDateTime.now();

			if (now.isBefore(ban.getEnd()) || now.isEqual(ban.getEnd())) {
				LocalDateTime aaaaa = ban.getEnd();
				model.addAttribute("aaaaa", aaaaa);
			} else {
				model.addAttribute("aaaaa", "通常");
			}

			// 最新のパニッシュ値（罰の強さ）も画面に渡す
			model.addAttribute("lastPunish", ban.getPunish());
		} else {
			model.addAttribute("aaaaa", "通常");
			model.addAttribute("lastPunish", null);
		}
		List<UserComplaint> complaintList = service.complaints(user);
		Set<Long> reporterIds = new HashSet<>();
		for (UserComplaint c : complaintList) {
			reporterIds.add(c.getReporterUserId().getId());
		}
		Map<Long, User> reporterMap = new HashMap<>();
		for (Long rid : reporterIds) {
			users.findById(rid).ifPresent(u -> reporterMap.put(rid, u));
		}
		model.addAttribute("rireki", aiueo);
		model.addAttribute("user", user);
		model.addAttribute("avgRating", avg);
		model.addAttribute("complaintCount", complaints);
		model.addAttribute("complaints", complaintList);
		model.addAttribute("reporterMap", reporterMap);
		return "admin/users/detail";
	}

	@PostMapping("/{id}/ban")
	public String ban(@PathVariable Long id,
			@RequestParam("reason") String reason,
			@RequestParam(value = "disableLogin", defaultValue = "true") boolean disableLogin,
			Authentication auth) {
		Long adminId = users.findByEmailIgnoreCase(auth.getName()).map(User::getId).orElse(null);
		service.banUser(id, adminId, reason, disableLogin);
		return "redirect:/admin/users/" + id + "?banned";
	}
	
	@PostMapping("/{id}/punish")
	public String punish(@PathVariable Long id,
			@RequestParam("description") String description,
			@RequestParam("a") int aaaa,
			Authentication auth,
			RedirectAttributes redirectAttributes) {
		User aiu = service.findUser(id);
		int trustrunk = aiu.getTrust();
		int damage = (10+aaaa)-trustrunk;
		int newtrustrunk = trustrunk - damage;
		aiu.setTrust(newtrustrunk);
		users.save(aiu);
		if (newtrustrunk<1) {
			String reason = "お客様のアカウントにおいて、複数の重大な規約違反行為が繰り返し確認されたため、無期限の利用停止措置をとらせていただきました。";
			Long adminId = users.findByEmailIgnoreCase(auth.getName()).map(User::getId).orElse(null);
			boolean disableLogin = false;
			service.banUser(id, adminId, reason, disableLogin);
		} else {
			int aa = damage * 2;
			LocalDateTime ao = LocalDateTime.now().plusDays(aa);
			Ban kkk = new Ban();
			kkk.setDescription(description);
			kkk.setEnd(ao);
			User ag = users.findById(id)
					.orElseThrow(() -> new IllegalArgumentException("Item not found"));
			kkk.setUserId(ag);
			kkk.setPunish(damage);
			banRepository.save(kkk);
			// 執行（一時停止）のメール通知
			String endFormatted = ao.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm"));
			String subject = "【フリマ】アカウントが一時停止されました";
			String body = ag.getName() + " 様\n\n"
					+ "お客様のアカウントは運営により一時停止されました。\n\n"
					+ "【理由】\n" + (description != null ? description : "") + "\n\n"
					+ "【アカウント復旧日時】\n" + endFormatted + "\n\n"
					+ "上記日時以降、アカウントは自動的に利用可能になります。";
			emailService.sendEmail(ag.getEmail(), subject, body);
		}
		return "redirect:/admin/users/" + id;
	}

	@PostMapping("/{id}/unban")
	public String unban(@PathVariable Long id) {
		service.unbanUser(id);
		return "redirect:/admin/users/" + id + "?unbanned";
	}
}
