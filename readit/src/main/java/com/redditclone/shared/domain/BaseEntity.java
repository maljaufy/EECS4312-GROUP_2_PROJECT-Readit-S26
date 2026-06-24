package com.redditclone.shared.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;



    @Getter
    @Setter
    @MappedSuperclass
    @EntityListeners(AuditingEntityListener.class)

    /*
    Base entity class for all entities
     */
    public abstract class BaseEntity {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @CreatedDate
        @Column(updatable = false)
        private LocalDateTime createdAt;

        @LastModifiedDate
        private LocalDateTime updatedAt;

        @CreatedBy
        @Column(updatable = false)
        private String createdBy;

        @LastModifiedBy
        private String updatedBy;

        @Version
        @Column(nullable = false)
        private Long version = 0L;

}
