/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.apache.ignite.examples.schema.model;

import java.math.BigDecimal;
import org.apache.ignite.catalog.annotations.Column;
import org.apache.ignite.catalog.annotations.ColumnRef;
import org.apache.ignite.catalog.annotations.Id;
import org.apache.ignite.catalog.annotations.Index;
import org.apache.ignite.catalog.annotations.Table;
import org.apache.ignite.catalog.annotations.Zone;

/**
 * InvoiceLine entity demonstrating transaction detail colocation patterns.
 * 
 * Shows invoice line item colocation using invoice hierarchy,
 * purchase transaction details, pricing calculations, and quantity management.
 * Third level in sales transaction hierarchy for complete data locality.
 */
@Table(
    zone = @Zone(value = "MusicStore", storageProfiles = "default"),
    colocateBy = @ColumnRef("InvoiceId"),
    indexes = {
        @Index(value = "IFK_InvoiceLineInvoiceId", columns = { @ColumnRef("InvoiceId") }),
        @Index(value = "IFK_InvoiceLineTrackId", columns = { @ColumnRef("TrackId") })
    }
)
public class InvoiceLine {
    
    @Id
    private Integer InvoiceLineId;
    
    @Id
    private Integer InvoiceId;
    
    @Column(value = "TrackId", nullable = false)
    private Integer TrackId;
    
    @Column(value = "UnitPrice", precision = 10, scale = 2, nullable = false)
    private BigDecimal UnitPrice;
    
    @Column(value = "Quantity", nullable = false)
    private Integer Quantity;
    
    public InvoiceLine() {
    }
    
    public InvoiceLine(Integer invoiceLineId, Integer invoiceId, Integer trackId, 
                      BigDecimal unitPrice, Integer quantity) {
        this.InvoiceLineId = invoiceLineId;
        this.InvoiceId = invoiceId;
        this.TrackId = trackId;
        this.UnitPrice = unitPrice;
        this.Quantity = quantity;
    }
    
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