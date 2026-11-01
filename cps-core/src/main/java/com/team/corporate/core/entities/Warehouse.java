package com.team.corporate.core.entities;

import com.team.corporate.core.utils.EntityValidation;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Entity
@Table(name = "warehouses")
public class Warehouse {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "location")
    private String location;

    public Warehouse() {
    }

    public Warehouse(@NotNull String name, @Nullable String location) {
        this.name = EntityValidation.requireText(name, "Название склада");
        this.location = EntityValidation.optionalText(location, "Адрес");
    }

    public Warehouse(@NotNull String name) {
        this.name = EntityValidation.requireText(name, "Название склада");
    }

    @NotNull
    public UUID getId() {
        return id;
    }

    @NotNull
    public String getName() {
        return name;
    }

    public void setName(@NotNull String name) {
        this.name = EntityValidation.requireText(name, "Название склада");
    }

    @Nullable
    public String getLocation() {
        return location;
    }

    public void setLocation(@Nullable String location) {
        this.location = EntityValidation.optionalText(location, "Адрес");
    }
}
