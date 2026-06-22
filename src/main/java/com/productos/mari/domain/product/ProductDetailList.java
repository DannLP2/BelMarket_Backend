package com.productos.mari.domain.product;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "product_detail_lists")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDetailList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(name = "display_type", nullable = false)
    @Builder.Default
    private String displayType = "GRID";

    @ElementCollection
    @CollectionTable(name = "product_detail_list_items", joinColumns = @JoinColumn(name = "list_id"))
    @Column(name = "item")
    private List<String> items;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;
}
