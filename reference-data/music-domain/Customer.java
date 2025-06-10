package com.example.model;

import org.apache.ignite.catalog.annotations.Column;
import org.apache.ignite.catalog.annotations.ColumnRef;
import org.apache.ignite.catalog.annotations.Table;
import org.apache.ignite.catalog.annotations.Id;
import org.apache.ignite.catalog.annotations.Index;
import org.apache.ignite.catalog.annotations.Zone;

/**
 * Represents a customer in the Chinook database.
 * This class maps to the Customer table which contains information about customers.
 */
@Table(
        zone = @Zone(value = "Chinook", storageProfiles = "default"),
        indexes = {
            @Index(value = "IFK_CustomerSupportRepId", columns = { @ColumnRef("SupportRepId") })
        }
)
public class Customer {
    // Primary key field
    @Id
    @Column(value = "CustomerId", nullable = false)
    private Integer CustomerId;

    @Column(value = "FirstName", nullable = false)
    private String FirstName;

    @Column(value = "LastName", nullable = false)
    private String LastName;

    @Column(value = "Company", nullable = true)
    private String Company;

    @Column(value = "Address", nullable = true)
    private String Address;

    @Column(value = "City", nullable = true)
    private String City;

    @Column(value = "State", nullable = true)
    private String State;

    @Column(value = "Country", nullable = true)
    private String Country;

    @Column(value = "PostalCode", nullable = true)
    private String PostalCode;

    @Column(value = "Phone", nullable = true)
    private String Phone;

    @Column(value = "Fax", nullable = true)
    private String Fax;

    @Column(value = "Email", nullable = false)
    private String Email;

    @Column(value = "SupportRepId", nullable = true)
    private Integer SupportRepId;

    /**
     * Default constructor required for serialization
     */
    public Customer() { }

    /**
     * Constructs a Customer with specified details
     *
     * @param customerId The unique identifier for the customer
     * @param firstName The first name of the customer
     * @param lastName The last name of the customer
     * @param email The email of the customer
     */
    public Customer(Integer customerId, String firstName, String lastName, String email) {
        this.CustomerId = customerId;
        this.FirstName = firstName;
        this.LastName = lastName;
        this.Email = email;
    }

    /**
     * Constructs a Customer with all details
     *
     * @param customerId The unique identifier for the customer
     * @param firstName The first name of the customer
     * @param lastName The last name of the customer
     * @param company The company of the customer
     * @param address The address of the customer
     * @param city The city of the customer
     * @param state The state of the customer
     * @param country The country of the customer
     * @param postalCode The postal code of the customer
     * @param phone The phone number of the customer
     * @param fax The fax number of the customer
     * @param email The email of the customer
     * @param supportRepId The ID of the employee who supports this customer
     */
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

    // Getters and setters

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
                '}';
    }
}