package com.example.model;

import org.apache.ignite.catalog.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents an invoice in the Chinook database.
 * This class maps to the Invoice table which contains billing information for sales.
 */
@Table(
        zone = @Zone(value = "Chinook", storageProfiles = "default"),
        colocateBy = @ColumnRef("CustomerId"),
        indexes = {
            @Index(value = "IFK_InvoiceCustomerId", columns = { @ColumnRef("CustomerId") })
        }
)
public class Invoice {
    // Primary key field
    @Id
    @Column(value = "InvoiceId", nullable = false)
    private Integer InvoiceId;

    // Foreign key to Customer
    @Id
    @Column(value = "CustomerId", nullable = false)
    private Integer CustomerId;

    @Column(value = "InvoiceDate", nullable = false)
    private LocalDate InvoiceDate;

    @Column(value = "BillingAddress", nullable = true)
    private String BillingAddress;

    @Column(value = "BillingCity", nullable = true)
    private String BillingCity;

    @Column(value = "BillingState", nullable = true)
    private String BillingState;

    @Column(value = "BillingCountry", nullable = true)
    private String BillingCountry;

    @Column(value = "BillingPostalCode", nullable = true)
    private String BillingPostalCode;

    @Column(value = "Total", nullable = false)
    private BigDecimal Total;

    /**
     * Default constructor required for serialization
     */
    public Invoice() { }

    /**
     * Constructs an Invoice with essential details
     *
     * @param invoiceId The unique identifier for the invoice
     * @param customerId The ID of the customer who made the purchase
     * @param invoiceDate The date of the invoice
     * @param total The total amount of the invoice
     */
    public Invoice(Integer invoiceId, Integer customerId, LocalDate invoiceDate, BigDecimal total) {
        this.InvoiceId = invoiceId;
        this.CustomerId = customerId;
        this.InvoiceDate = invoiceDate;
        this.Total = total;
    }

    /**
     * Constructs an Invoice with full details
     *
     * @param invoiceId The unique identifier for the invoice
     * @param customerId The ID of the customer who made the purchase
     * @param invoiceDate The date of the invoice
     * @param billingAddress The billing address
     * @param billingCity The billing city
     * @param billingState The billing state
     * @param billingCountry The billing country
     * @param billingPostalCode The billing postal code
     * @param total The total amount of the invoice
     */
    public Invoice(Integer invoiceId, Integer customerId, LocalDate invoiceDate, 
                   String billingAddress, String billingCity, String billingState, 
                   String billingCountry, String billingPostalCode, BigDecimal total) {
        this.InvoiceId = invoiceId;
        this.CustomerId = customerId;
        this.InvoiceDate = invoiceDate;
        this.BillingAddress = billingAddress;
        this.BillingCity = billingCity;
        this.BillingState = billingState;
        this.BillingCountry = billingCountry;
        this.BillingPostalCode = billingPostalCode;
        this.Total = total;
    }

    // Getters and setters

    public Integer getInvoiceId() {
        return InvoiceId;
    }

    public void setInvoiceId(Integer invoiceId) {
        this.InvoiceId = invoiceId;
    }

    public Integer getCustomerId() {
        return CustomerId;
    }

    public void setCustomerId(Integer customerId) {
        this.CustomerId = customerId;
    }

    public LocalDate getInvoiceDate() {
        return InvoiceDate;
    }

    public void setInvoiceDate(LocalDate invoiceDate) {
        this.InvoiceDate = invoiceDate;
    }

    public String getBillingAddress() {
        return BillingAddress;
    }

    public void setBillingAddress(String billingAddress) {
        this.BillingAddress = billingAddress;
    }

    public String getBillingCity() {
        return BillingCity;
    }

    public void setBillingCity(String billingCity) {
        this.BillingCity = billingCity;
    }

    public String getBillingState() {
        return BillingState;
    }

    public void setBillingState(String billingState) {
        this.BillingState = billingState;
    }

    public String getBillingCountry() {
        return BillingCountry;
    }

    public void setBillingCountry(String billingCountry) {
        this.BillingCountry = billingCountry;
    }

    public String getBillingPostalCode() {
        return BillingPostalCode;
    }

    public void setBillingPostalCode(String billingPostalCode) {
        this.BillingPostalCode = billingPostalCode;
    }

    public BigDecimal getTotal() {
        return Total;
    }

    public void setTotal(BigDecimal total) {
        this.Total = total;
    }

    @Override
    public String toString() {
        return "Invoice{" +
                "InvoiceId=" + InvoiceId +
                ", CustomerId=" + CustomerId +
                ", InvoiceDate=" + InvoiceDate +
                ", Total=" + Total +
                '}';
    }
}