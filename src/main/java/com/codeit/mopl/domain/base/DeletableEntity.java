package com.codeit.mopl.domain.base;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public class DeletableEntity extends UpdatableEntity {

    @Column(name = "deleted_at")
    protected Instant deletedAt;
}
