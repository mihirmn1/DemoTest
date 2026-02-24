package com.retail.checkout;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/**
 * Legacy checkout service.
 *
 * Known issues:
 *  - inconsistent rounding / scale handling
 *  - fragile promo application order
 *  - some null handling is inconsistent
 *  - shipping logic is tangled with item pricing
 *
 * This file is intentionally "legacy-ish" for demo purposes.
 */
public class CartCheckoutService {

    // In a real system these would be injected clients/services
    private final TaxRateProvider taxRateProvider = new TaxRateProvider();
    private final InventoryGateway inventoryGateway = new InventoryGateway();
    private final FraudRules fraudRules = new FraudRules();

    public CheckoutResult checkout(CheckoutRequest req) {
        if (req == null) {
            return CheckoutResult.error("NULL_REQUEST");
        }
        if (req.getCart() == null || req.getCart().getItems() == null || req.getCart().getItems().isEmpty()) {
            return CheckoutResult.error("EMPTY_CART");
        }
        if (req.getUserId() == null || req.getUserId().trim().isEmpty()) {
            return CheckoutResult.error("MISSING_USER");
        }
        if (req.getAddress() == null || req.getAddress().getCountryCode() == null) {
            return CheckoutResult.error("MISSING_ADDRESS");
        }

        // Inventory validation
        for (CartItem item : req.getCart().getItems()) {
            if (item == null || item.getSku() == null) {
                return CheckoutResult.error("INVALID_ITEM");
            }
            if (item.getQty() <= 0) {
                return CheckoutResult.error("INVALID_QTY");
            }
            boolean available = inventoryGateway.isAvailable(item.getSku(), item.getQty());
            if (!available) {
                return CheckoutResult.error("OUT_OF_STOCK:" + item.getSku());
            }
        }

        // Fraud screening (simplified)
        if (fraudRules.isHighRisk(req)) {
            return CheckoutResult.error("FRAUD_REVIEW_REQUIRED");
        }

        // Subtotal
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : req.getCart().getItems()) {
            BigDecimal price = safe(item.getUnitPrice());
            BigDecimal line = price.multiply(BigDecimal.valueOf(item.getQty()));
            subtotal = subtotal.add(line);
        }

        // Item-level discounts (legacy: applies before cart-level promos)
        BigDecimal itemDiscountTotal = BigDecimal.ZERO;
        for (CartItem item : req.getCart().getItems()) {
            if (item.getItemDiscountPct() != null) {
                BigDecimal pct = item.getItemDiscountPct();
                if (pct.compareTo(BigDecimal.ZERO) > 0 && pct.compareTo(new BigDecimal("0.80")) < 0) { // cap 80%
                    BigDecimal line = safe(item.getUnitPrice()).multiply(BigDecimal.valueOf(item.getQty()));
                    BigDecimal disc = line.multiply(pct);
                    itemDiscountTotal = itemDiscountTotal.add(disc);
                }
            }
        }

        BigDecimal discountedSubtotal = subtotal.subtract(itemDiscountTotal);

        // Cart-level promo (legacy: messy rules)
        PromoApplication promoApplication = applyPromo(req, discountedSubtotal);

        BigDecimal afterPromo = discountedSubtotal.subtract(promoApplication.getDiscount());
        if (afterPromo.compareTo(BigDecimal.ZERO) < 0) afterPromo = BigDecimal.ZERO;

        // Shipping
        BigDecimal shipping = computeShipping(req, afterPromo);

        // Tax (based on state/country; excludes shipping for some states)
        BigDecimal taxRate = taxRateProvider.getRate(req.getAddress().getCountryCode(), req.getAddress().getStateOrProvince());
        BigDecimal taxableBase = afterPromo;

        // Legacy rule: for "AK" and "DE", do not tax shipping; otherwise tax shipping.
        if (req.getAddress().getStateOrProvince() != null) {
            String st = req.getAddress().getStateOrProvince().trim().toUpperCase();
            if (!st.equals("AK") && !st.equals("DE")) {
                taxableBase = taxableBase.add(shipping);
            }
        } else {
            taxableBase = taxableBase.add(shipping);
        }

        BigDecimal tax = taxableBase.multiply(taxRate);

        // Rounding and scale (legacy inconsistency; intentional)
        BigDecimal total = afterPromo.add(shipping).add(tax);
        total = total.setScale(2, RoundingMode.HALF_UP);

        // Build result
        CheckoutResult result = CheckoutResult.success();
        result.setSubtotal(subtotal.setScale(2, RoundingMode.HALF_UP));
        result.setItemDiscount(itemDiscountTotal.setScale(2, RoundingMode.HALF_UP));
        result.setPromoCode(req.getPromoCode());
        result.setPromoDiscount(promoApplication.getDiscount().setScale(2, RoundingMode.HALF_UP));
        result.setPromoMessage(promoApplication.getMessage());
        result.setShipping(shipping.setScale(2, RoundingMode.HALF_UP));
        result.setTax(tax.setScale(2, RoundingMode.HALF_UP));
        result.setTotal(total);
        result.setCurrency(req.getCart().getCurrency() == null ? "USD" : req.getCart().getCurrency());
        return result;
    }

    private PromoApplication applyPromo(CheckoutRequest req, BigDecimal discountedSubtotal) {
        String promo = req.getPromoCode();
        if (promo == null || promo.trim().isEmpty()) {
            return PromoApplication.none("NO_PROMO");
        }

        String code = promo.trim().toUpperCase();
        BigDecimal discount = BigDecimal.ZERO;
        String msg = "PROMO_APPLIED";

        // Legacy constraints
        boolean isEmployee = req.getFlags() != null && Boolean.TRUE.equals(req.getFlags().get("employee"));
        boolean isNewCustomer = req.getFlags() != null && Boolean.TRUE.equals(req.getFlags().get("newCustomer"));
        LocalDate today = req.getRequestDate() != null ? req.getRequestDate() : LocalDate.now();

        // Example promos:
        //  - SAVE10: 10% off orders >= $50 (not combinable with employee)
        //  - FREESHIP: free shipping for US only; if subtotal >= $25
        //  - WELCOME15: $15 off for new customers (min $75)
        //  - BOGO50: 50% off the cheapest eligible item if 2+ eligible items
        if (code.equals("SAVE10")) {
            if (isEmployee) {
                return PromoApplication.none("PROMO_NOT_ELIGIBLE_EMPLOYEE");
            }
            if (discountedSubtotal.compareTo(new BigDecimal("50.00")) >= 0) {
                discount = discountedSubtotal.multiply(new BigDecimal("0.10"));
            } else {
                return PromoApplication.none("MIN_NOT_MET");
            }
        } else if (code.equals("WELCOME15")) {
            if (!isNewCustomer) {
                return PromoApplication.none("PROMO_NEW_CUSTOMERS_ONLY");
            }
            if (discountedSubtotal.compareTo(new BigDecimal("75.00")) >= 0) {
                discount = new BigDecimal("15.00");
            } else {
                return PromoApplication.none("MIN_NOT_MET");
            }
        } else if (code.equals("FREESHIP")) {
            // This promo is handled in shipping computation (legacy cross-cutting)
            if (!"US".equalsIgnoreCase(req.getAddress().getCountryCode())) {
                return PromoApplication.none("PROMO_US_ONLY");
            }
            if (discountedSubtotal.compareTo(new BigDecimal("25.00")) < 0) {
                return PromoApplication.none("MIN_NOT_MET");
            }
            msg = "FREESHIP_ELIGIBLE";
            discount = BigDecimal.ZERO;
        } else if (code.equals("BOGO50")) {
            // Very simplified: assume items tagged "bogoEligible"
            int eligibleCount = 0;
            BigDecimal cheapest = null;
            for (CartItem item : req.getCart().getItems()) {
                if (item != null && item.getTags() != null && item.getTags().contains("bogoEligible")) {
                    eligibleCount += item.getQty();
                    BigDecimal p = safe(item.getUnitPrice());
                    if (cheapest == null || p.compareTo(cheapest) < 0) cheapest = p;
                }
            }
            if (eligibleCount >= 2 && cheapest != null) {
                discount = cheapest.multiply(new BigDecimal("0.50"));
            } else {
                return PromoApplication.none("NOT_ENOUGH_ELIGIBLE_ITEMS");
            }
        } else {
            return PromoApplication.none("UNKNOWN_PROMO");
        }

        // Legacy cap: discount cannot exceed 30% of discounted subtotal (except WELCOME15)
        if (!code.equals("WELCOME15")) {
            BigDecimal cap = discountedSubtotal.multiply(new BigDecimal("0.30"));
            if (discount.compareTo(cap) > 0) {
                discount = cap;
                msg = "PROMO_CAPPED";
            }
        }

        // Legacy: black-out dates (e.g., holiday freeze) for SAVE10
        if (code.equals("SAVE10")) {
            if (today.getMonthValue() == 11 && today.getDayOfMonth() >= 20) {
                return PromoApplication.none("PROMO_BLACKOUT");
            }
        }

        // Round promo discount but keep some odd scale behavior (intentional)
        discount = discount.setScale(2, RoundingMode.HALF_UP);
        return PromoApplication.applied(discount, msg);
    }

    private BigDecimal computeShipping(CheckoutRequest req, BigDecimal afterPromoSubtotal) {
        String country = req.getAddress().getCountryCode();
        String tier = req.getCart().getShippingTier() == null ? "STANDARD" : req.getCart().getShippingTier().toUpperCase();

        BigDecimal base;
        if ("US".equalsIgnoreCase(country)) {
            if ("EXPRESS".equals(tier)) base = new BigDecimal("14.99");
            else if ("OVERNIGHT".equals(tier)) base = new BigDecimal("29.99");
            else base = new BigDecimal("7.99");
        } else if ("CA".equalsIgnoreCase(country)) {
            base = new BigDecimal("12.99");
        } else {
            base = new BigDecimal("24.99");
        }

        // Weight surcharge (legacy: approximates using qty)
        int totalQty = 0;
        for (CartItem item : req.getCart().getItems()) {
            totalQty += item.getQty();
        }
        if (totalQty > 8) {
            base = base.add(new BigDecimal("6.00"));
        } else if (totalQty > 4) {
            base = base.add(new BigDecimal("3.00"));
        }

        // FREESHIP promo cross-cuts here
        if (req.getPromoCode() != null && req.getPromoCode().trim().equalsIgnoreCase("FREESHIP")) {
            if ("US".equalsIgnoreCase(country) && afterPromoSubtotal.compareTo(new BigDecimal("25.00")) >= 0) {
                return BigDecimal.ZERO;
            }
        }

        // Free shipping threshold for STANDARD US orders >= $100
        if ("US".equalsIgnoreCase(country) && "STANDARD".equals(tier)) {
            if (afterPromoSubtotal.compareTo(new BigDecimal("100.00")) >= 0) {
                return BigDecimal.ZERO;
            }
        }

        return base;
    }

    private BigDecimal safe(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    // ----- Supporting “in-file” legacy collaborators (for demo realism) -----

    static class TaxRateProvider {
        BigDecimal getRate(String country, String state) {
            if (country == null) return BigDecimal.ZERO;
            if (!"US".equalsIgnoreCase(country)) return new BigDecimal("0.00"); // simplified
            if (state == null) return new BigDecimal("0.065");
            String st = state.trim().toUpperCase(Locale.ROOT);
            // rough examples
            if (st.equals("CA")) return new BigDecimal("0.0825");
            if (st.equals("NY")) return new BigDecimal("0.08875");
            if (st.equals("TX")) return new BigDecimal("0.0625");
            if (st.equals("DE")) return new BigDecimal("0.00");
            return new BigDecimal("0.065");
        }
    }

    static class InventoryGateway {
        boolean isAvailable(String sku, int qty) {
            // For demo: treat "SKU-OOS" as out of stock
            return !"SKU-OOS".equalsIgnoreCase(sku);
        }
    }

    static class FraudRules {
        boolean isHighRisk(CheckoutRequest req) {
            // For demo: very high totals + first-time buyers -> review
            if (req.getFlags() != null && Boolean.TRUE.equals(req.getFlags().get("trustedUser"))) return false;
            BigDecimal est = BigDecimal.ZERO;
            if (req.getCart() != null && req.getCart().getItems() != null) {
                for (CartItem i : req.getCart().getItems()) {
                    est = est.add((i.getUnitPrice() == null ? BigDecimal.ZERO : i.getUnitPrice())
                            .multiply(BigDecimal.valueOf(i.getQty())));
                }
            }
            return est.compareTo(new BigDecimal("1000.00")) > 0;
        }
    }
}