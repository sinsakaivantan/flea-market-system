// src/main/java/com/example/fleamarketsystem/controller/AdminUserController.java
package com.example.fleamarketsystem.controller;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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
import com.example.fleamarketsystem.repository.BanRepository;
import com.example.fleamarketsystem.repository.UserRepository;
import com.example.fleamarketsystem.service.AdminUserService;

@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

	private final AdminUserService service;
	private final UserRepository users;
	private final BanRepository banRepository;
	public AdminUserController(AdminUserService service, UserRepository users, BanRepository banRepository) {
		this.service = service;
		this.users = users;
		this.banRepository = banRepository;
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
		default -> list;
		};

		model.addAttribute("users", list);
		model.addAttribute("q", q);
		model.addAttribute("sort", sort);
		return "admin/users/list";
	}

	@GetMapping("/{id}")
	public String detail(@PathVariable Long id, Model model) {
		User user = service.findUser(id);
		Double avg = service.averageRating(id);
		long complaints = service.complaintCount(id);
		Optional<Ban> banOpt = banRepository.findTopByUserIdOrderByEndDesc(user);
		if (banOpt.isPresent()) {
            Ban ban = banOpt.get();
            LocalDateTime now = LocalDateTime.now();

            if (now.isBefore(ban.getEnd()) || now.isEqual(ban.getEnd())) {
                LocalDateTime aaaaa = ban.getEnd();
                model.addAttribute("aaaaa",aaaaa);
            }else {
            	model.addAttribute("aaaaa","通常");
            }
        }else {
        	model.addAttribute("aaaaa","通常");
        }
		model.addAttribute("user", user);
		model.addAttribute("avgRating", avg);
		model.addAttribute("complaintCount", complaints);
		model.addAttribute("complaints", service.complaints(id));
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
			String reason = "0以下";
			Long adminId = users.findByEmailIgnoreCase(auth.getName()).map(User::getId).orElse(null);
			boolean disableLogin = true;
			service.banUser(id, adminId, reason, disableLogin);
		}else {
			int aa = damage * 2;
			LocalDateTime ao = LocalDateTime.now()
									.plusDays(aa);
			Ban kkk = new Ban();
			kkk.setDescription(description);
			kkk.setEnd(ao);
			User ag = users.findById(id)
	                .orElseThrow(() -> new IllegalArgumentException("Item not found"));
			kkk.setUserId(ag);
			kkk.setPunish(damage);
			banRepository.save(kkk);
		}
		return "redirect:/admin/renraku";
	}

	@PostMapping("/{id}/unban")
	public String unban(@PathVariable Long id) {
		service.unbanUser(id);
		return "redirect:/admin/users/" + id + "?unbanned";
	}
}
