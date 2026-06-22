package com.productos.mari.domain.chatbot.strategies;

import com.productos.mari.domain.brand.BrandService;
import com.productos.mari.domain.brand.BrandDto;
import com.productos.mari.domain.category.CategoryService;
import com.productos.mari.domain.category.CategoryDto;
import com.productos.mari.domain.marketing.BannerService;
import com.productos.mari.domain.marketing.OfferService;
import com.productos.mari.domain.product.ProductDto;
import com.productos.mari.domain.product.ProductService;
import com.productos.mari.domain.settings.AppSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ClientChatbotStrategy implements ChatbotRoleStrategy {

    private final ProductService productService;
    private final BannerService bannerService;
    private final OfferService offerService;
    private final CategoryService categoryService;
    private final BrandService brandService;

    @Override
    public boolean supports(String role) {
        return "CLIENT".equalsIgnoreCase(role);
    }

    @Override
    public String getSystemPrompt(String message, AppSettings settings) {
        String storeName = settings.getStoreName() != null ? settings.getStoreName() : "BelMarket";
        
        // 1. Product Context (Search + Fallback)
        Set<ProductDto> relevantProducts = new HashSet<>();
        relevantProducts.addAll(productService.searchProducts(message, PageRequest.of(0, 30)).getContent());
        if (relevantProducts.size() < 10) {
            relevantProducts.addAll(productService.getAllProducts(PageRequest.of(0, 15)).getContent());
        }
        List<ProductDto> finalProducts = new ArrayList<>(relevantProducts).stream().limit(40).toList();

        String productsContext = finalProducts.stream()
            .map(p -> {
                String link = p.getSlug() != null && !p.getSlug().isEmpty() ? "/product/" + p.getSlug() : "/product-id/" + p.getId();
                String img = p.getImageUrl() != null && !p.getImageUrl().isEmpty() ? p.getImageUrl() : "https://via.placeholder.com/300x300?text=Sin+Imagen";
                return String.format("- %s | Categorias: %s | Marca: %s | Precio: $%.2f | Stock: %d | Enlace: %s | Imagen: %s",
                    p.getName(),
                    p.getCategories() != null ? String.join(", ", p.getCategories()) : "General",
                    p.getBrand() != null ? p.getBrand() : storeName,
                    p.getPrice(), p.getStock(), link, img);
            }).collect(Collectors.joining("\n"));

        // 2. Marketing Context (Banners & Offers)
        String bannersCtx = bannerService.getActiveBanners().stream()
                .limit(3)
                .map(b -> (String) String.format("- %s: %s", b.getTitle(), b.getDescription()))
                .collect(Collectors.joining("\n"));
        
        String offersCtx = offerService.getActiveOffers().stream()
                .limit(3)
                .map(o -> (String) String.format("- %s (%s%% de descuento)", o.getTitle(), o.getDiscountValue()))
                .collect(Collectors.joining("\n"));

        // 3. Store Structure Context
        String categoriesCtx = categoryService.getAllCategoriesWithCount().stream()
                .limit(5)
                .map(c -> String.format("- %s (%d productos)", c.getName(), c.getProductCount()))
                .collect(Collectors.joining(", "));
        
        String brandsCtx = brandService.getAllBrandsWithCount().stream()
                .limit(5)
                .map(b -> b.getName())
                .collect(Collectors.joining(", "));

        // 4. Policies & Social Media
        StringBuilder infoCtx = new StringBuilder();
        if (Boolean.TRUE.equals(settings.getDistanceShippingEnabled())) {
            infoCtx.append(String.format("- Envio: Dinamico por distancia ($%.2f base).\n", settings.getDefaultShippingCost()));
        } else if (settings.getDefaultShippingCost() != null) {
            infoCtx.append(String.format("- Envio fijo: $%.2f.\n", settings.getDefaultShippingCost()));
        }
        if (settings.getFreeShippingThreshold() != null && settings.getFreeShippingThreshold().compareTo(BigDecimal.ZERO) > 0) {
            infoCtx.append(String.format("- ¡Envio GRATIS desde $%.2f!\n", settings.getFreeShippingThreshold()));
        }
        
        // Social Media & Contact
        if (settings.getInstagramUrl() != null) infoCtx.append("- Instagram: ").append(settings.getInstagramUrl()).append("\n");
        if (settings.getFacebookUrl() != null) infoCtx.append("- Facebook: ").append(settings.getFacebookUrl()).append("\n");
        if (settings.getWhatsappNumber() != null) infoCtx.append("- WhatsApp: +").append(settings.getWhatsappNumber()).append("\n");

        String catalogSection = finalProducts.isEmpty() 
            ? "== CATALOGO ACTUAL ==\nNo hay coincidencias exactas ahora mismo.\n"
            : "== PRODUCTOS ENCONTRADOS (" + finalProducts.size() + ") ==\n" + productsContext + "\n";

        return "Eres 'Mia', la embajadora y asistente personal de compras de " + storeName + ". Tu tono es elegante, profesional y muy servicial.\n\n" +
            "CONTEXTO DE MARKETING:\n" +
            "Banners actuales:\n" + (bannersCtx.isEmpty() ? "Varios estilos disponibles." : bannersCtx) + "\n" +
            "Ofertas activas:\n" + (offersCtx.isEmpty() ? "Precios competitivos en toda la tienda." : offersCtx) + "\n\n" +
            "ESTRUCTURA DE LA TIENDA:\n" +
            "Categorias populares: " + categoriesCtx + "\n" +
            "Marcas destacadas: " + brandsCtx + "\n\n" +
            "POLITICAS Y CONTACTO:\n" + infoCtx + "\n\n" +
            catalogSection + "\n" +
            "INSTRUCCIONES DE RESPUESTA (ESTRICTAS):\n" +
            "1. FORMATO DE PRODUCTO: Muestra siempre la imagen en Markdown ![nombre](url) seguido del nombre con enlace [nombre](url) y el precio. MAXIMO 5 productos.\n" +
            "2. PROACTIVIDAD: Si no encuentras el producto exacto, sugiere uno similar o invita a ver las categorias populares.\n" +
            "3. ENLACES INTERNOS Y CUENTA:\n" +
            "   - Para ver pedidos: [Mis Reservas](/my-reservations).\n" +
            "   - Para ayuda humana o dudas: [Centro de Ayuda](/help-center) (NUNCA uses /support).\n" +
            "   - Perfil/Cuenta: Indica al usuario que puede actualizar su informacion desde el icono de perfil en la barra superior.\n" +
            "   - Publicidad/Pautar: Si alguien quiere anunciarse (pautar), mencionalo como un servicio disponible que pueden gestionar con soporte.\n" +
            "4. INTEGRIDAD: No inventes productos. Si no esta en la lista, no existe en el stock actual.\n" +
            "5. ESTILO: Breve, usa MAX 3 emojis. Responde siempre en el idioma del usuario.";
    }
}
