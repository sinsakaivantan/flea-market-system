package com.example.fleamarketsystem.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.fleamarketsystem.entity.User;
import com.example.fleamarketsystem.entity.UserInventory;
import com.example.fleamarketsystem.service.ShopService;
import com.example.fleamarketsystem.service.UserMoneyService;
import com.example.fleamarketsystem.service.UserService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/shop")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;
    private final UserService userService;
    private final UserMoneyService userMoneyService;

    @GetMapping
    public String showShop(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.getUserByEmail(userDetails.getUsername()).orElseThrow();
        
        // 所持金
        Integer balance = userMoneyService.getBalance(user);
        model.addAttribute("token", balance);
        
        // 全アイテム一覧
        model.addAttribute("items", shopService.getAllItems());
        
        // インベントリをListからMapに変換
        List<UserInventory> inventoryList = shopService.getUserInventory(user);
        Map<Long, UserInventory> inventoryMap = inventoryList.stream()
            .collect(Collectors.toMap(inv -> inv.getItem().getId(), inv -> inv));
            
        model.addAttribute("inventoryMap", inventoryMap); 
        
        return "shop";
    }

    @PostMapping("/buy/{itemId}")
    public String buyItem(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long itemId) {
        User user = userService.getUserByEmail(userDetails.getUsername()).orElseThrow();
        try {
            shopService.buyItem(user, itemId);
        } catch (Exception e) {
            // エラーハンドリング
        }
        return "redirect:/shop";
    }

    @PostMapping("/equip/{itemId}")
    public String equipItem(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long itemId) {
        User user = userService.getUserByEmail(userDetails.getUsername()).orElseThrow();
        shopService.equipItem(user, itemId);
        return "redirect:/my-page/loadout";
    }

    // ★★★★★ 追加箇所 ★★★★★
    // これがないと 404 エラーになります
    @PostMapping("/unequip/{itemId}")
    public String unequipItem(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long itemId) {
        User user = userService.getUserByEmail(userDetails.getUsername()).orElseThrow();
        shopService.unequipItem(user, itemId);
        
        // 装備ページへ戻る
        return "redirect:/my-page/loadout";
    }
    // ★★★★★★★★★★★★★★★
}