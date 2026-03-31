package ru.panyukovnn.videoretellingbot.model;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

import static ru.panyukovnn.videoretellingbot.util.Constants.DEFAULT_DB_USER;

@Getter
@Setter
@MappedSuperclass
public class AuditableEntity {

    private Instant createTime;
    private String createUser;
    private Instant lastUpdateTime;
    private String lastUpdateUser;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        this.createTime = now;
        this.lastUpdateTime = now;
        this.createUser = DEFAULT_DB_USER;
        this.lastUpdateUser = DEFAULT_DB_USER;
    }

    @PreUpdate
    public void preUpdate() {
        this.lastUpdateTime = Instant.now();
        this.lastUpdateUser = DEFAULT_DB_USER;

        if (this.createTime == null) {
            this.createTime = Instant.now();
        }

        if (this.createUser == null) {
            this.createUser = DEFAULT_DB_USER;
        }
    }
}