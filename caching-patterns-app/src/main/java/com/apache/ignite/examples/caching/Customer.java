package com.apache.ignite.examples.caching;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Customer entity representing streaming service users.
 * 
 * Used for demonstrating write-through patterns with consistency-critical
 * customer data operations.
 */
public class Customer {
    
    private int customerId;
    private String name;
    private String email;
    private SubscriptionTier subscriptionTier;
    private PaymentMethod paymentMethod;
    private LocalDate subscriptionStartDate;
    private boolean active;
    
    public Customer() {
        // Default constructor required for Ignite
    }
    
    private Customer(Builder builder) {
        this.customerId = builder.customerId;
        this.name = builder.name;
        this.email = builder.email;
        this.subscriptionTier = builder.subscriptionTier;
        this.paymentMethod = builder.paymentMethod;
        this.subscriptionStartDate = builder.subscriptionStartDate;
        this.active = builder.active;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public Builder toBuilder() {
        return new Builder()
            .customerId(this.customerId)
            .name(this.name)
            .email(this.email)
            .subscriptionTier(this.subscriptionTier)
            .paymentMethod(this.paymentMethod)
            .subscriptionStartDate(this.subscriptionStartDate)
            .active(this.active);
    }
    
    /**
     * Checks if customer has an active subscription.
     */
    public boolean hasActiveSubscription() {
        return active && subscriptionTier != null && subscriptionTier != SubscriptionTier.FREE;
    }
    
    /**
     * Checks if customer can upgrade to specified tier.
     */
    public boolean canUpgradeTo(SubscriptionTier newTier) {
        if (!hasActiveSubscription()) {
            return false;
        }
        
        // Simple upgrade logic - can upgrade to higher tiers
        return newTier.ordinal() > this.subscriptionTier.ordinal();
    }
    
    // Getters and setters
    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public SubscriptionTier getSubscriptionTier() { return subscriptionTier; }
    public void setSubscriptionTier(SubscriptionTier subscriptionTier) { this.subscriptionTier = subscriptionTier; }
    
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    
    public LocalDate getSubscriptionStartDate() { return subscriptionStartDate; }
    public void setSubscriptionStartDate(LocalDate subscriptionStartDate) { this.subscriptionStartDate = subscriptionStartDate; }
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Customer customer = (Customer) o;
        return customerId == customer.customerId;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(customerId);
    }
    
    @Override
    public String toString() {
        return String.format("Customer{id=%d, name='%s', email='%s', tier=%s, active=%s}", 
            customerId, name, email, subscriptionTier, active);
    }
    
    public static class Builder {
        private int customerId;
        private String name;
        private String email;
        private SubscriptionTier subscriptionTier = SubscriptionTier.FREE;
        private PaymentMethod paymentMethod;
        private LocalDate subscriptionStartDate = LocalDate.now();
        private boolean active = true;
        
        public Builder customerId(int customerId) {
            this.customerId = customerId;
            return this;
        }
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder email(String email) {
            this.email = email;
            return this;
        }
        
        public Builder subscriptionTier(SubscriptionTier subscriptionTier) {
            this.subscriptionTier = subscriptionTier;
            return this;
        }
        
        public Builder paymentMethod(PaymentMethod paymentMethod) {
            this.paymentMethod = paymentMethod;
            return this;
        }
        
        public Builder subscriptionStartDate(LocalDate subscriptionStartDate) {
            this.subscriptionStartDate = subscriptionStartDate;
            return this;
        }
        
        public Builder active(boolean active) {
            this.active = active;
            return this;
        }
        
        public Customer build() {
            return new Customer(this);
        }
    }
}

enum SubscriptionTier {
    FREE, BASIC, PREMIUM, FAMILY, PREMIUM_PLUS
}

enum PaymentMethod {
    CREDIT_CARD, PAYPAL, BANK_TRANSFER, GIFT_CARD
}