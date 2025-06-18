package com.apache.ignite.examples.schema.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.apache.ignite.catalog.annotations.Column;
import org.apache.ignite.catalog.annotations.ColumnRef;
import org.apache.ignite.catalog.annotations.Id;
import org.apache.ignite.catalog.annotations.Index;
import org.apache.ignite.catalog.annotations.Table;
import org.apache.ignite.catalog.annotations.Zone;

/**
 * Invoice entity demonstrating transaction data colocation patterns.
 * 
 * Shows customer-invoice colocation using composite primary keys,
 * transaction date handling, billing information management, and
 * monetary calculations. Second level in sales transaction hierarchy.
 */
@Table(
    zone = @Zone(value = "MusicStore", storageProfiles = "default"),
    colocateBy = @ColumnRef("CustomerId"),
    indexes = @Index(value = "IFK_InvoiceCustomerId", columns = { @ColumnRef("CustomerId") })
)
public class Invoice {
    
    @Id
    private Integer InvoiceId;
    
    @Id
    private Integer CustomerId;
    
    @Column(value = "InvoiceDate", nullable = false)
    private LocalDate InvoiceDate;
    
    @Column(value = "BillingAddress", length = 70, nullable = true)
    private String BillingAddress;
    
    @Column(value = "BillingCity", length = 40, nullable = true)
    private String BillingCity;
    
    @Column(value = "BillingState", length = 40, nullable = true)
    private String BillingState;
    
    @Column(value = "BillingCountry", length = 40, nullable = true)
    private String BillingCountry;
    
    @Column(value = "BillingPostalCode", length = 10, nullable = true)
    private String BillingPostalCode;
    
    @Column(value = "Total", precision = 10, scale = 2, nullable = false)
    private BigDecimal Total;
    
    public Invoice() {
    }
    
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
                ", BillingCity='" + BillingCity + '\'' +
                ", BillingCountry='" + BillingCountry + '\'' +
                ", Total=" + Total +
                '}';
    }
}