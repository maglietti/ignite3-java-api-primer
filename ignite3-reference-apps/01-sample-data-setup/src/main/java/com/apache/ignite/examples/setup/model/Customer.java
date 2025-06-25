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

package com.apache.ignite.examples.setup.model;

import org.apache.ignite.catalog.annotations.Column;
import org.apache.ignite.catalog.annotations.ColumnRef;
import org.apache.ignite.catalog.annotations.Id;
import org.apache.ignite.catalog.annotations.Index;
import org.apache.ignite.catalog.annotations.Table;
import org.apache.ignite.catalog.annotations.Zone;

/**
 * Customer entity representing music store customers.
 * 
 * This demonstrates business entity patterns in Ignite 3:
 * - Root Business Entity: Starting point for customer-related data hierarchy
 * - Rich Customer Data: Comprehensive customer information including address, contact details
 * - Simple Primary Key: Single integer ID for straightforward distribution
 * - Foreign Key Index: Index on SupportRepId for efficient employee lookups
 * - Business Zone: Uses standard replication (2 replicas) for transactional data
 * 
 * Hierarchy: Customer → Invoice → InvoiceLine (colocated for performance)
 * This allows efficient queries like "get customer with all their orders and line items"
 */
@Table(
    // Standard business data zone with 2 replicas
    // Balances availability with storage efficiency for transactional data
    zone = @Zone(value = "MusicStore", storageProfiles = "default"),
    
    // Index on support representative for efficient employee-customer queries
    // Supports queries like "find all customers assigned to a sales rep"
    indexes = @Index(value = "IFK_CustomerSupportRepId", columns = { @ColumnRef("SupportRepId") })
)
public class Customer {
    
    /**
     * Customer identifier - simple primary key.
     * This will be used as the colocation key for Invoices and InvoiceLines
     * to ensure all customer data is stored together for efficient queries.
     */
    @Id
    private Integer CustomerId;
    
    /**
     * Customer first name - required field.
     */
    @Column(value = "FirstName", length = 40, nullable = false)
    private String FirstName;
    
    /**
     * Customer last name - required field.
     */
    @Column(value = "LastName", length = 20, nullable = false)
    private String LastName;
    
    @Column(value = "Company", length = 80, nullable = true)
    private String Company;
    
    @Column(value = "Address", length = 70, nullable = true)
    private String Address;
    
    @Column(value = "City", length = 40, nullable = true)
    private String City;
    
    @Column(value = "State", length = 40, nullable = true)
    private String State;
    
    @Column(value = "Country", length = 40, nullable = true)
    private String Country;
    
    @Column(value = "PostalCode", length = 10, nullable = true)
    private String PostalCode;
    
    @Column(value = "Phone", length = 24, nullable = true)
    private String Phone;
    
    @Column(value = "Fax", length = 24, nullable = true)
    private String Fax;
    
    @Column(value = "Email", length = 60, nullable = false)
    private String Email;
    
    @Column(value = "SupportRepId", nullable = true)
    private Integer SupportRepId;
    
    public Customer() {
    }
    
    public Customer(Integer customerId, String firstName, String lastName, String company, 
                   String address, String city, String state, String country, String postalCode,
                   String phone, String fax, String email, Integer supportRepId) {
        this.CustomerId = customerId;
        this.FirstName = firstName;
        this.LastName = lastName;
        this.Company = company;
        this.Address = address;
        this.City = city;
        this.State = state;
        this.Country = country;
        this.PostalCode = postalCode;
        this.Phone = phone;
        this.Fax = fax;
        this.Email = email;
        this.SupportRepId = supportRepId;
    }
    
    public Integer getCustomerId() {
        return CustomerId;
    }
    
    public void setCustomerId(Integer customerId) {
        this.CustomerId = customerId;
    }
    
    public String getFirstName() {
        return FirstName;
    }
    
    public void setFirstName(String firstName) {
        this.FirstName = firstName;
    }
    
    public String getLastName() {
        return LastName;
    }
    
    public void setLastName(String lastName) {
        this.LastName = lastName;
    }
    
    public String getCompany() {
        return Company;
    }
    
    public void setCompany(String company) {
        this.Company = company;
    }
    
    public String getAddress() {
        return Address;
    }
    
    public void setAddress(String address) {
        this.Address = address;
    }
    
    public String getCity() {
        return City;
    }
    
    public void setCity(String city) {
        this.City = city;
    }
    
    public String getState() {
        return State;
    }
    
    public void setState(String state) {
        this.State = state;
    }
    
    public String getCountry() {
        return Country;
    }
    
    public void setCountry(String country) {
        this.Country = country;
    }
    
    public String getPostalCode() {
        return PostalCode;
    }
    
    public void setPostalCode(String postalCode) {
        this.PostalCode = postalCode;
    }
    
    public String getPhone() {
        return Phone;
    }
    
    public void setPhone(String phone) {
        this.Phone = phone;
    }
    
    public String getFax() {
        return Fax;
    }
    
    public void setFax(String fax) {
        this.Fax = fax;
    }
    
    public String getEmail() {
        return Email;
    }
    
    public void setEmail(String email) {
        this.Email = email;
    }
    
    public Integer getSupportRepId() {
        return SupportRepId;
    }
    
    public void setSupportRepId(Integer supportRepId) {
        this.SupportRepId = supportRepId;
    }
    
    @Override
    public String toString() {
        return "Customer{" +
                "CustomerId=" + CustomerId +
                ", FirstName='" + FirstName + '\'' +
                ", LastName='" + LastName + '\'' +
                ", Company='" + Company + '\'' +
                ", Email='" + Email + '\'' +
                ", City='" + City + '\'' +
                ", Country='" + Country + '\'' +
                '}';
    }
}