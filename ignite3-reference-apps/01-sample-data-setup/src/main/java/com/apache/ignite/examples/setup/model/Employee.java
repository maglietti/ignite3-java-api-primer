package com.apache.ignite.examples.setup.model;

import java.time.LocalDate;
import org.apache.ignite.catalog.annotations.Column;
import org.apache.ignite.catalog.annotations.ColumnRef;
import org.apache.ignite.catalog.annotations.Id;
import org.apache.ignite.catalog.annotations.Index;
import org.apache.ignite.catalog.annotations.Table;
import org.apache.ignite.catalog.annotations.Zone;

@Table(
    zone = @Zone(value = "MusicStore", storageProfiles = "default"),
    indexes = @Index(value = "IFK_EmployeeReportsTo", columns = { @ColumnRef("ReportsTo") })
)
public class Employee {
    
    @Id
    private Integer EmployeeId;
    
    @Column(value = "LastName", length = 20, nullable = false)
    private String LastName;
    
    @Column(value = "FirstName", length = 20, nullable = false)
    private String FirstName;
    
    @Column(value = "Title", length = 30, nullable = true)
    private String Title;
    
    @Column(value = "ReportsTo", nullable = true)
    private Integer ReportsTo;
    
    @Column(value = "BirthDate", nullable = true)
    private LocalDate BirthDate;
    
    @Column(value = "HireDate", nullable = true)
    private LocalDate HireDate;
    
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
    
    @Column(value = "Email", length = 60, nullable = true)
    private String Email;
    
    public Employee() {
    }
    
    public Employee(Integer employeeId, String lastName, String firstName, String title,
                   Integer reportsTo, LocalDate birthDate, LocalDate hireDate, String address,
                   String city, String state, String country, String postalCode,
                   String phone, String fax, String email) {
        this.EmployeeId = employeeId;
        this.LastName = lastName;
        this.FirstName = firstName;
        this.Title = title;
        this.ReportsTo = reportsTo;
        this.BirthDate = birthDate;
        this.HireDate = hireDate;
        this.Address = address;
        this.City = city;
        this.State = state;
        this.Country = country;
        this.PostalCode = postalCode;
        this.Phone = phone;
        this.Fax = fax;
        this.Email = email;
    }
    
    public Integer getEmployeeId() {
        return EmployeeId;
    }
    
    public void setEmployeeId(Integer employeeId) {
        this.EmployeeId = employeeId;
    }
    
    public String getLastName() {
        return LastName;
    }
    
    public void setLastName(String lastName) {
        this.LastName = lastName;
    }
    
    public String getFirstName() {
        return FirstName;
    }
    
    public void setFirstName(String firstName) {
        this.FirstName = firstName;
    }
    
    public String getTitle() {
        return Title;
    }
    
    public void setTitle(String title) {
        this.Title = title;
    }
    
    public Integer getReportsTo() {
        return ReportsTo;
    }
    
    public void setReportsTo(Integer reportsTo) {
        this.ReportsTo = reportsTo;
    }
    
    public LocalDate getBirthDate() {
        return BirthDate;
    }
    
    public void setBirthDate(LocalDate birthDate) {
        this.BirthDate = birthDate;
    }
    
    public LocalDate getHireDate() {
        return HireDate;
    }
    
    public void setHireDate(LocalDate hireDate) {
        this.HireDate = hireDate;
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
    
    @Override
    public String toString() {
        return "Employee{" +
                "EmployeeId=" + EmployeeId +
                ", LastName='" + LastName + '\'' +
                ", FirstName='" + FirstName + '\'' +
                ", Title='" + Title + '\'' +
                ", ReportsTo=" + ReportsTo +
                ", Email='" + Email + '\'' +
                '}';
    }
}