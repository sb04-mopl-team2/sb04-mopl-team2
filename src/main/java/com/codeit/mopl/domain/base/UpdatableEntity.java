package com.codeit.mopl.domain.base;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@MappedSuperclass
public class UpdatableEntity extends BaseEntity {

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    protected Instant updatedAt;
}