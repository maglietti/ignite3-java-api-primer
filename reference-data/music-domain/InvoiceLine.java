package com.example.model;

import org.apache.ignite.catalog.annotations.*;
import java.math.BigDecimal;

/**
 * Represents an invoice line in the Chinook database.
 * This class maps to the InvoiceLine table which contains the details of each invoice.
 */
@Table(
        zone = @Zone(value = "Chinook", storageProfiles = "default"),
        colocateBy = @ColumnRef("InvoiceId"),
        indexes = {
            @Index(value = "IFK_InvoiceLineInvoiceId", columns = { @ColumnRef("InvoiceId") }),
            @Index(value = "IFK_InvoiceLineTrackId", columns = { @ColumnRef("TrackId") })
        }
)
public class InvoiceLine {
    // Primary key field
    @Id
    @Column(value = "InvoiceLineId", nullable = false)
    private Integer InvoiceLineId;

    // Foreign key to Invoice
    @Id
    @Column(value = "InvoiceId", nullable = false)
    private Integer InvoiceId;

    // Foreign key to Track
    @Column(value = "TrackId", nullable = false)
    private Integer TrackId;

    @Column(value = "UnitPrice", nullable = false)
    private BigDecimal UnitPrice;

    @Column(value = "Quantity", nullable = false)
    private Integer Quantity;

    /**
     * Default constructor required for serialization
     */
    public InvoiceLine() { }

    /**
     * Constructs an InvoiceLine with all details
     *
     * @param invoiceLineId The unique identifier for the invoice line
     * @param invoiceId The ID of the invoice this line belongs to
     * @param trackId The ID of the track that was purchased
     * @param unitPrice The price per unit
     * @param quantity The quantity purchased
     */
    public InvoiceLine(Integer invoiceLineId, Integer invoiceId, Integer trackId, 
                       BigDecimal unitPrice, Integer quantity) {
        this.InvoiceLineId = invoiceLineId;
        this.InvoiceId = invoiceId;
        this.TrackId = trackId;
        this.UnitPrice = unitPrice;
        this.Quantity = quantity;
    }

    // Getters and setters

    public Integer getInvoiceLineId() {
        return InvoiceLineId;
    }

    public void setInvoiceLineId(Integer invoiceLineId) {
        this.InvoiceLineId = invoiceLineId;
    }

    public Integer getInvoiceId() {
        return InvoiceId;
    }

    public void setInvoiceId(Integer invoiceId) {
        this.InvoiceId = invoiceId;
    }

    public Integer getTrackId() {
        return TrackId;
    }

    public void setTrackId(Integer trackId) {
        this.TrackId = trackId;
    }

    public BigDecimal getUnitPrice() {
        return UnitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.UnitPrice = unitPrice;
    }

    public Integer getQuantity() {
        return Quantity;
    }

    public void setQuantity(Integer quantity) {
        this.Quantity = quantity;
    }

    @Override
    public String toString() {
        return "InvoiceLine{" +
                "InvoiceLineId=" + InvoiceLineId +
                ", InvoiceId=" + InvoiceId +
                ", TrackId=" + TrackId +
                ", UnitPrice=" + UnitPrice +
                ", Quantity=" + Quantity +
                '}';
    }
}