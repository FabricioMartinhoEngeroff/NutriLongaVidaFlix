package com.dvFabricio.NutriLongaVidaFlix.domain.payment;

import com.dvFabricio.NutriLongaVidaFlix.domain.user.User;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Table(name = "payments")
@Entity
@EqualsAndHashCode(of = "id")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    private User user;
    private String method;
    private double amount;
    private Date date;
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;
    private String externalTransactionId;
    private String description;
    private Date creationDate;
    private Date updateDate;

    public Payment() {}

    public Payment(User user, String method, double amount, Date date, String description) {
        this.user = user;
        this.method = method;
        this.amount = amount;
        this.date = date;
        this.description = description;
        this.status = PaymentStatus.PENDING;
        this.creationDate = new Date();
        this.updateDate = new Date();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getExternalTransactionId() {
        return externalTransactionId;
    }

    public void setExternalTransactionId(String externalTransactionId) {
        this.externalTransactionId = externalTransactionId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public Date getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
    }
}