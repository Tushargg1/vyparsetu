package com.vyaparsetu.whatsapp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Maps a retailer's slang/abbreviation to a catalog product (or a canonical
 * search term). Lets the matcher resolve common terms without the LLM.
 */
@Entity
@Table(name = "product_aliases")
@Getter
@Setter
public class ProductAlias {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(nullable = false)
    private String alias;

    /** Direct product mapping (preferred). */
    @Column(name = "product_id")
    private Long productId;

    /** Alternative: a canonical term to search for instead of the alias. */
    private String canonical;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
}
