package com.retail.checkout;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

class CheckoutRequest {
    private String userId;
    private Cart cart;
    private Address address;
    private String promoCode;
    private Map<String, Boolean> flags;
    private LocalDate requestDate;

    public String getUserId() { return userId; }
    public Cart getCart() { return cart; }
    public Address getAddress() { return address; }
    public String getPromoCode() { return promoCode; }
    public Map<String, Boolean> getFlags() { return flags; }
    public LocalDate getRequestDate() { return requestDate; }

    public CheckoutRequest setUserId(String userId) { this.userId = userId; return this; }
    public CheckoutRequest setCart(Cart cart) { this.cart = cart; return this; }
    public CheckoutRequest setAddress(Address address) { this.address = address; return this; }
    public CheckoutRequest setPromoCode(String promoCode) { this.promoCode = promoCode; return this; }
    public CheckoutRequest setFlags(Map<String, Boolean> flags) { this.flags = flags; return this; }
    public CheckoutRequest setRequestDate(LocalDate requestDate) { this.requestDate = requestDate; return this; }
}

class Cart {
    private List<CartItem> items;
    private String currency;
    private String shippingTier;

    public List<CartItem> getItems() { return items; }
    public String getCurrency() { return currency; }
    public String getShippingTier() { return shippingTier; }

    public Cart setItems(List<CartItem> items) { this.items = items; return this; }
    public Cart setCurrency(String currency) { this.currency = currency; return this; }
    public Cart setShippingTier(String shippingTier) { this.shippingTier = shippingTier; return this; }
}

class CartItem {
    private String sku;
    private int qty;
    private BigDecimal unitPrice;
    private BigDecimal itemDiscountPct; // e.g. 0.10 for 10%
    private Set<String> tags;

    public String getSku() { return sku; }
    public int getQty() { return qty; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getItemDiscountPct() { return itemDiscountPct; }
    public Set<String> getTags() { return tags; }

    public CartItem setSku(String sku) { this.sku = sku; return this; }
    public CartItem setQty(int qty) { this.qty = qty; return this; }
    public CartItem setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; return this; }
    public CartItem setItemDiscountPct(BigDecimal itemDiscountPct) { this.itemDiscountPct = itemDiscountPct; return this; }
    public CartItem setTags(Set<String> tags) { this.tags = tags; return this; }
}

class Address {
    private String countryCode;
    private String stateOrProvince;
    private String postalCode;

    public String getCountryCode() { return countryCode; }
    public String getStateOrProvince() { return stateOrProvince; }
    public String getPostalCode() { return postalCode; }

    public Address setCountryCode(String countryCode) { this.countryCode = countryCode; return this; }
    public Address setStateOrProvince(String stateOrProvince) { this.stateOrProvince = stateOrProvince; return this; }
    public Address setPostalCode(String postalCode) { this.postalCode = postalCode; return this; }
}

class CheckoutResult {
    private boolean success;
    private String errorCode;

    private String currency;
    private String promoCode;
    private String promoMessage;

    private BigDecimal subtotal;
    private BigDecimal itemDiscount;
    private BigDecimal promoDiscount;
    private BigDecimal shipping;
    private BigDecimal tax;
    private BigDecimal total;

    static CheckoutResult success() {
        CheckoutResult r = new CheckoutResult();
        r.success = true;
        return r;
    }

    static CheckoutResult error(String code) {
        CheckoutResult r = new CheckoutResult();
        r.success = false;
        r.errorCode = code;
        return r;
    }

    public boolean isSuccess() { return success; }
    public String getErrorCode() { return errorCode; }

    public String getCurrency() { return currency; }
    public String getPromoCode() { return promoCode; }
    public String getPromoMessage() { return promoMessage; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getItemDiscount() { return itemDiscount; }
    public BigDecimal getPromoDiscount() { return promoDiscount; }
    public BigDecimal getShipping() { return shipping; }
    public BigDecimal getTax() { return tax; }
    public BigDecimal getTotal() { return total; }

    public void setCurrency(String currency) { this.currency = currency; }
    public void setPromoCode(String promoCode) { this.promoCode = promoCode; }
    public void setPromoMessage(String promoMessage) { this.promoMessage = promoMessage; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public void setItemDiscount(BigDecimal itemDiscount) { this.itemDiscount = itemDiscount; }
    public void setPromoDiscount(BigDecimal promoDiscount) { this.promoDiscount = promoDiscount; }
    public void setShipping(BigDecimal shipping) { this.shipping = shipping; }
    public void setTax(BigDecimal tax) { this.tax = tax; }
    public void setTotal(BigDecimal total) { this.total = total; }
}

class PromoApplication {
    private final BigDecimal discount;
    private final String message;

    private PromoApplication(BigDecimal discount, String message) {
        this.discount = discount;
        this.message = message;
    }

    static PromoApplication none(String message) {
        return new PromoApplication(BigDecimal.ZERO, message);
    }

    static PromoApplication applied(BigDecimal discount, String message) {
        return new PromoApplication(discount, message);
    }

    public BigDecimal getDiscount() { return discount; }
    public String getMessage() { return message; }
}