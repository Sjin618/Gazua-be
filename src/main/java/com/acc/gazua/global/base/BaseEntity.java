package com.acc.gazua.global.base;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

import java.time.Clock;
import java.time.LocalDateTime;

@Getter
@MappedSuperclass
public abstract class BaseEntity extends BaseTimeEntity{
    private LocalDateTime deletedAt;

    public void delete(LocalDateTime now){
        this.deletedAt = now;
    }

    public boolean isDeleted(){
        return this.deletedAt != null;
    }
}
