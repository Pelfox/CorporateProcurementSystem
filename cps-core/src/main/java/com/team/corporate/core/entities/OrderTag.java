package com.team.corporate.core.entities;

import com.team.corporate.core.utils.EntityValidation;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Entity
@Table(name = "tags")
public class OrderTag {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "color", length = 20)
    private String color;

    public OrderTag() {}

    public OrderTag(@NotNull String name) {
        this.name = EntityValidation.requireText(name, "Название тега");
    }

    public OrderTag(@NotNull String name, @NotNull String color) {
        this.name = EntityValidation.requireText(name, "Название тега");
        this.color = EntityValidation.optionalText(color, "Цвет тега");
    }

    @NotNull public UUID getId() { return id; }
    @NotNull public String getName() { return name; }
    public void setName(@NotNull String name) { this.name = EntityValidation.requireText(name, "Название тега"); }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = EntityValidation.optionalText(color, "Цвет тега"); }
}