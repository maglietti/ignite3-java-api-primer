package com.example.model;

import org.apache.ignite.catalog.annotations.Column;
import org.apache.ignite.catalog.annotations.ColumnRef;
import org.apache.ignite.catalog.annotations.Table;
import org.apache.ignite.catalog.annotations.Id;
import org.apache.ignite.catalog.annotations.Index;
import org.apache.ignite.catalog.annotations.Zone;

import java.time.LocalDate;

/**
 * Represents an employee in the Chinook database.
 * This class maps to the Employee table which contains information about Chinook employees.
 */
@Table(
        zone = @Zone(value = "Chinook", storageProfiles = "default"),
        indexes = {
            @Index(value = "IFK_EmployeeReportsTo", columns = { @ColumnRef("ReportsTo") })
        }
)
public class Employee {
    // Primary key field
    @Id
    @Column(value = "EmployeeId", nullable = false)
    private Integer EmployeeId;

    @Column(value = "LastName", nullable = false)
    private String LastName;

    @Column(value = "FirstName", nullable = false)
    private String FirstName;

    @Column(value = "Title", nullable = true)
    private String Title;

    @Column(value = "ReportsTo", nullable = true)
    private Integer ReportsTo;

    @Column(value = "BirthDate", nullable = true)
    private LocalDate BirthDate;

    @Column(value = "HireDate", nullable = true)
    private LocalDate HireDate;

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

    @Column(value = "Email", nullable = true)
    private String Email;

    /**
     * Default constructor required for serialization
     */
    public Employee() { }

    /**
     * Constructs an Employee with essential details
     *
     * @param employeeId The unique identifier for the employee
     * @param lastName The last name of the employee
     * @param firstName The first name of the employee
     */
    public Employee(Integer employeeId, String lastName, String firstName) {
        this.EmployeeId = employeeId;
        this.LastName = lastName;
        this.FirstName = firstName;
    }

    /**
     * Constructs an Employee with full details
     *
     * @param employeeId The unique identifier for the employee
     * @param lastName The last name of the employee
     * @param firstName The first name of the employee
     * @param title The job title of the employee
     * @param reportsTo The ID of the employee's manager
     * @param birthDate The birth date of the employee
     * @param hireDate The hire date of the employee
     * @param address The address of the employee
     * @param city The city of the employee
     * @param state The state of the employee
     * @param country The country of the employee
     * @param postalCode The postal code of the employee
     * @param phone The phone number of the employee
     * @param fax The fax number of the employee
     * @param email The email of the employee
     */
    public Employee(Integer employeeId, String lastName, String firstName, String title,
                    Integer reportsTo, LocalDate birthDate, LocalDate hireDate, String address,
                    String city, String state, String country, String postalCode, String phone,
                    String fax, String email) {
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

    // Getters and setters

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
                '}';
    }
}