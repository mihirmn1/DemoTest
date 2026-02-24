package com.retail.checkout;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class CartCheckoutServiceTest {

    @Test
    void checkout_returnsError_onEmptyCart() {
        CartCheckoutService svc = new CartCheckoutService();
        CheckoutRequest req = new CheckoutRequest()
                .setUserId("u1")
                .setAddress(new Address().setCountryCode("US").setStateOrProvince("CA"))
                .setCart(new Cart().setItems(List.of()));

        CheckoutResult r = svc.checkout(req);
        assertFalse(r.isSuccess());
        assertEquals("EMPTY_CART", r.getErrorCode());
    }

    @Test
    void checkout_appliesSave10_whenMinMet_andNotEmployee() {
        CartCheckoutService svc = new CartCheckoutService();
        CheckoutRequest req = baseRequest()
                .setPromoCode("SAVE10")
                .setFlags(Map.of("employee", false));

        CheckoutResult r = svc.checkout(req);
        assertTrue(r.isSuccess());
        // Not asserting exact total (legacy rounding/tax/shipping); just sanity-check discount present
        assertTrue(r.getPromoDiscount().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void checkout_freeShipPromo_zeroesShipping_whenEligible() {
        CartCheckoutService svc = new CartCheckoutService();
        CheckoutRequest req = baseRequest()
                .setPromoCode("FREESHIP");

        CheckoutResult r = svc.checkout(req);
        assertTrue(r.isSuccess());
        assertEquals(new BigDecimal("0.00"), r.getShipping());
    }

    @Test
    void checkout_bogo50_requiresTwoEligibleItems() {
        CartCheckoutService svc = new CartCheckoutService();

        Cart cart = new Cart().setCurrency("USD").setShippingTier("STANDARD").setItems(List.of(
                new CartItem().setSku("SKU-1").setQty(1).setUnitPrice(new BigDecimal("20.00")).setTags(Set.of("bogoEligible"))
        ));

        CheckoutRequest req = new CheckoutRequest()
                .setUserId("u1")
                .setAddress(new Address().setCountryCode("US").setStateOrProvince("CA"))
                .setCart(cart)
                .setPromoCode("BOGO50")
                .setFlags(Map.of("newCustomer", false))
                .setRequestDate(LocalDate.of(2026, 2, 24));

        CheckoutResult r = svc.checkout(req);
        assertTrue(r.isSuccess());
        assertEquals(new BigDecimal("0.00"), r.getPromoDiscount());
        assertEquals("NOT_ENOUGH_ELIGIBLE_ITEMS", r.getPromoMessage());
    }

    private CheckoutRequest baseRequest() {
        Cart cart = new Cart().setCurrency("USD").setShippingTier("STANDARD").setItems(List.of(
                new CartItem().setSku("SKU-1").setQty(2).setUnitPrice(new BigDecimal("40.00")),
                new CartItem().setSku("SKU-2").setQty(1).setUnitPrice(new BigDecimal("30.00"))
        ));

        return new CheckoutRequest()
                .setUserId("u1")
                .setAddress(new Address().setCountryCode("US").setStateOrProvince("CA"))
                .setCart(cart)
                .setFlags(Map.of("newCustomer", false))
                .setRequestDate(LocalDate.of(2026, 2, 24));
    }
}