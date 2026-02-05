package com.example.fleamarketsystem.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fleamarketsystem.entity.AppOrder;
import com.example.fleamarketsystem.entity.Item;
import com.example.fleamarketsystem.entity.User;
import com.example.fleamarketsystem.repository.AppOrderRepository;
import com.example.fleamarketsystem.repository.ItemRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;

@Service
public class AppOrderService {

    private final AppOrderRepository appOrderRepository;
    private final ItemRepository itemRepository;
    private final ItemService itemService;
    private final StripeService stripeService;
    private final EmailService emailService;
    // ★追加: コイン付与のために必要
    private final UserMoneyService userMoneyService;

    // ★修正: コンストラクタに UserMoneyService を追加
    public AppOrderService(AppOrderRepository appOrderRepository, ItemRepository itemRepository, 
                           ItemService itemService, StripeService stripeService, 
                           EmailService emailService, UserMoneyService userMoneyService) {
        this.appOrderRepository = appOrderRepository;
        this.itemRepository = itemRepository;
        this.itemService = itemService;
        this.stripeService = stripeService;
        this.emailService = emailService;
        this.userMoneyService = userMoneyService;
    }

    @Transactional
    public PaymentIntent initiatePurchase(Long itemId, User buyer) throws StripeException, RuntimeException {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found"));

        if (!"出品中".equals(item.getStatus())) {
            throw new IllegalStateException("Item is not available for purchase.");
        }

        // Create a PaymentIntent with Stripe
        PaymentIntent paymentIntent = stripeService.createPaymentIntent(item.getPrice(), "jpy", "購入: " + item.getName());

        // Save a pending order with PaymentIntent ID
        AppOrder appOrder = new AppOrder();
        appOrder.setItem(item);
        appOrder.setBuyer(buyer);
        appOrder.setPrice(item.getPrice());
        appOrder.setStatus("決済待ち"); // New status for pending payment
        appOrder.setPaymentIntentId(paymentIntent.getId()); // Store PaymentIntent ID
        appOrder.setCreatedAt(LocalDateTime.now()); // Set creation time
        appOrderRepository.save(appOrder);

        return paymentIntent;
    }

    @Transactional
    public AppOrder completePurchase(String paymentIntentId) throws StripeException {
        PaymentIntent paymentIntent = stripeService.retrievePaymentIntent(paymentIntentId);

        if ("succeeded".equals(paymentIntent.getStatus())) {
            // Find the order associated with this payment intent using paymentIntentId
            AppOrder appOrder = appOrderRepository.findByPaymentIntentId(paymentIntentId)
                    .orElseThrow(() -> new IllegalStateException("No order found for payment intent: " + paymentIntentId));

            appOrder.setStatus("購入済");
            itemService.markItemAsSold(appOrder.getItem().getId());
            AppOrder savedOrder = appOrderRepository.save(appOrder);

            // ★★★ ここに追加: フリマコイン付与処理 ★★★
            // AppOrder.getPrice() が BigDecimal の場合を想定して計算
            BigDecimal priceBD = savedOrder.getPrice();
            // UserMoneyService は int を要求するため変換 (小数は切り捨て)
            int priceInt = priceBD.intValue(); 

            // 1. 売った側 (Seller): 10%
            int sellerCoin = (int) (priceInt * 0.10);
            if (sellerCoin > 0) {
                userMoneyService.addFleaCoin(savedOrder.getItem().getSeller(), sellerCoin);
            }

            // 2. 買った側 (Buyer): 5%
            int buyerCoin = (int) (priceInt * 0.05);
            if (buyerCoin > 0) {
                userMoneyService.addFleaCoin(savedOrder.getBuyer(), buyerCoin);
            }
            // ★★★ 追加終了 ★★★


            // Send email notification to seller
            if (savedOrder.getItem().getSeller().getEmail() != null && !savedOrder.getItem().getSeller().getEmail().isEmpty()) {
                String subject = "商品が購入されました";
                String message = String.format("商品が購入されました！\n\n商品名: %s\n購入者: %s\n価格: ¥%s\n\nフリマコインを獲得しました！",
                        savedOrder.getItem().getName(),
                        savedOrder.getBuyer().getName(),
                        savedOrder.getPrice());
                emailService.sendEmail(savedOrder.getItem().getSeller().getEmail(), subject, message);
            }

            return savedOrder;
        } else {
            throw new IllegalStateException("Payment not succeeded. Status: " + paymentIntent.getStatus());
        }
    }

    public List<AppOrder> getAllOrders() {
        return appOrderRepository.findAll();
    }

    public List<AppOrder> getOrdersByBuyer(User buyer) {
        return appOrderRepository.findByBuyer(buyer);
    }

    public List<AppOrder> getOrdersBySeller(User seller) {
        return appOrderRepository.findByItem_Seller(seller);
    }

    @Transactional
    public void markOrderAsShipped(Long orderId) {
        AppOrder appOrder = appOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        appOrder.setStatus("発送済");
        AppOrder savedOrder = appOrderRepository.save(appOrder);

        // Send email notification to buyer
        if (savedOrder.getBuyer().getEmail() != null && !savedOrder.getBuyer().getEmail().isEmpty()) {
            String subject = "購入した商品が発送されました";
            String message = String.format("購入した商品が発送されました！\n\n商品名: %s\n出品者: %s",
                    savedOrder.getItem().getName(),
                    savedOrder.getItem().getSeller().getName());
            emailService.sendEmail(savedOrder.getBuyer().getEmail(), subject, message);
        }
    }

    public Optional<AppOrder> getOrderById(Long orderId) {
        return appOrderRepository.findById(orderId);
    }

    // Method to get the latest completed order ID for redirection
    public Optional<Long> getLatestCompletedOrderId() {
        return appOrderRepository.findAll().stream()
                .filter(o -> "購入済".equals(o.getStatus()))
                .map(AppOrder::getId)
                .max(Long::compare);
    }

    public BigDecimal getTotalSales(LocalDate startDate, LocalDate endDate) {
        return appOrderRepository.findAll().stream()
                .filter(order -> order.getStatus().equals("購入済") || order.getStatus().equals("発送済"))
                .filter(order -> order.getCreatedAt().toLocalDate().isAfter(startDate.minusDays(1)) && order.getCreatedAt().toLocalDate().isBefore(endDate.plusDays(1))) // Use order.getCreatedAt()
                .map(AppOrder::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Map<String, Long> getOrderCountByStatus(LocalDate startDate, LocalDate endDate) {
        return appOrderRepository.findAll().stream()
                .filter(order -> order.getCreatedAt().toLocalDate().isAfter(startDate.minusDays(1)) && order.getCreatedAt().toLocalDate().isBefore(endDate.plusDays(1))) // Use order.getCreatedAt()
                .collect(Collectors.groupingBy(AppOrder::getStatus, Collectors.counting()));
    }
}