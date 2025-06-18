package com.apache.ignite.examples.schema.model;

import org.apache.ignite.catalog.annotations.Column;
import org.apache.ignite.catalog.annotations.ColumnRef;
import org.apache.ignite.catalog.annotations.Id;
import org.apache.ignite.catalog.annotations.Index;
import org.apache.ignite.catalog.annotations.Table;
import org.apache.ignite.catalog.annotations.Zone;

/**
 * Customer entity demonstrating complex business entity patterns.
 * 
 * Shows field mapping, multiple data types, nullability constraints,
 * and index configurations for business entities. Root entity in sales
 * transaction hierarchy for colocation demonstrations.
 */
@Table(
    zone = @Zone(value = "MusicStore", storageProfiles = "default"),
    indexes = @Index(value = "IFK_CustomerSupportRepId", columns = { @ColumnRef("SupportRepId") })
)
public class Customer {
    
    @Id
    private Integer CustomerId;
    
    @Column(value = "FirstName", length = 40, nullable = false)
    private String FirstName;
    
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
                ", Email='" + Email + '\'' +
                ", City='" + City + '\'' +
                ", Country='" + Country + '\'' +
                '}';
    }
}