package com.retail.checkout;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DemoRunner {
    public static void main(String[] args) {
        CartCheckoutService svc = new CartCheckoutService();

        Cart cart = new Cart()
                .setCurrency("USD")
                .setShippingTier("STANDARD")
                .setItems(List.of(
                        new CartItem().setSku("SKU-1").setQty(2).setUnitPrice(new BigDecimal("40.00")),
                        new CartItem().setSku("SKU-2").setQty(1).setUnitPrice(new BigDecimal("30.00")).setTags(Set.of("bogoEligible"))
                ));

        CheckoutRequest req = new CheckoutRequest()
                .setUserId("u1")
                .setAddress(new Address().setCountryCode("US").setStateOrProvince("CA").setPostalCode("94107"))
                .setCart(cart)
                .setPromoCode("SAVE10")
                .setFlags(Map.of("employee", false, "newCustomer", false, "trustedUser", true))
                .setRequestDate(LocalDate.of(2026, 2, 24));

        CheckoutResult r = svc.checkout(req);

        if (!r.isSuccess()) {
            System.out.println("Checkout failed: " + r.getErrorCode());
            return;
        }

        System.out.println("Currency:       " + r.getCurrency());
        System.out.println("Subtotal:       " + r.getSubtotal());
        System.out.println("Item Discount:  " + r.getItemDiscount());
        System.out.println("Promo Code:     " + r.getPromoCode());
        System.out.println("Promo Discount: " + r.getPromoDiscount() + " (" + r.getPromoMessage() + ")");
        System.out.println("Shipping:       " + r.getShipping());
        System.out.println("Tax:            " + r.getTax());
        System.out.println("Total:          " + r.getTotal());
    }
}
