package com.productos.mari.domain.infrastructure.admin;

import com.productos.mari.domain.product.Product;
import com.productos.mari.domain.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/public/seo")
@RequiredArgsConstructor
public class SeoController {

    private final ProductRepository productRepository;

    @GetMapping(value = "/product/{id}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getProductSeoTags(@PathVariable Long id) {
        Optional<Product> optionalProduct = productRepository.findById(id);
        
        if (optionalProduct.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Product product = optionalProduct.get();

        String title = product.getName() + " | BelMarket";
        String description = product.getDescription();
        if (description != null && description.length() > 150) {
            description = description.substring(0, 150) + "...";
        }
        
        String imageUrl = product.getMainImageUrl() != null ? product.getMainImageUrl() : "";

        String html = "<!DOCTYPE html>\n" +
                "<html lang=\"es\">\n" +
                "<head>\n" +
                "    <meta charset=\"utf-8\">\n" +
                "    <title>" + escapeHtml(title) + "</title>\n" +
                "    <meta property=\"og:type\" content=\"product\" />\n" +
                "    <meta property=\"og:title\" content=\"" + escapeHtml(title) + "\" />\n" +
                "    <meta property=\"og:description\" content=\"" + escapeHtml(description) + "\" />\n" +
                "    <meta property=\"og:image\" content=\"" + escapeHtml(imageUrl) + "\" />\n" +
                "    <meta name=\"twitter:card\" content=\"summary_large_image\">\n" +
                "</head>\n" +
                "<body>\n" +
                "    <script>window.location.replace(\"/product/" + id + "\");</script>\n" +
                "</body>\n" +
                "</html>";

        return ResponseEntity.ok(html);
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;");
    }
}
