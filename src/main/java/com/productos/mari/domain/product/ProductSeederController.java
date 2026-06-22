package com.productos.mari.domain.product;
import com.productos.mari.domain.mecatronic.MecatronicDeviceRepository;
import com.productos.mari.domain.mecatronic.DeviceVariableRepository;
import com.productos.mari.domain.mecatronic.VariableReadingRepository;
import com.productos.mari.domain.mecatronic.DeviceActionRepository;
import com.productos.mari.domain.auth.SecurityLogRepository;
import com.productos.mari.domain.product.ProductDetailListRepository;
import com.productos.mari.domain.mecatronic.MecatronicDevice;
import com.productos.mari.domain.mecatronic.DeviceVariable;
import com.productos.mari.domain.mecatronic.VariableReading;
import com.productos.mari.domain.mecatronic.DeviceAction;
import com.productos.mari.domain.user.UserLinkedDeviceRepository;
import com.productos.mari.domain.user.UserRepository;
import com.productos.mari.domain.marketing.OfferRepository;
import com.productos.mari.domain.review.ReviewRepository;
import com.productos.mari.domain.brand.BrandRepository;
import com.productos.mari.domain.brand.Brand;
import com.productos.mari.domain.reservation.ReservationItemRepository;



import com.productos.mari.domain.category.Category;
import com.productos.mari.domain.category.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

@RestController
@RequestMapping("/api/admin/seed")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class ProductSeederController {

        private final ProductRepository productRepository;
        private final BrandRepository brandRepository;
        private final CategoryRepository categoryRepository;
        private final ProductDetailListRepository detailListRepository;
        private final OfferRepository offerRepository;
        private final ReviewRepository reviewRepository;
        private final ReservationItemRepository reservationItemRepository;
        private final MecatronicDeviceRepository mecatronicDeviceRepository;
        private final UserLinkedDeviceRepository userLinkedDeviceRepository;
        private final DeviceVariableRepository deviceVariableRepository;
        private final VariableReadingRepository variableReadingRepository;
        private final DeviceActionRepository deviceActionRepository;
        private final jakarta.persistence.EntityManager entityManager;

        @GetMapping("/full-catalog")
        @Transactional
        public ResponseEntity<String> seedFullCatalog() {
                // 0. Limpiar previo para asegurar un estado limpio y evitar errores 500
                purgeCatalog("CONFIRMAR");

                // 1. Marcas y Categorías
                Brand ecoPlus = getBrand("ECO+");
                Brand gardenPro = getBrand("GardenPRO");
                Brand petBuddy = getBrand("PetBuddy");
                Brand dior = getBrand("Dior");
                Brand vintageCo = getBrand("VintageCo");
                Brand ninja = getBrand("Ninja");

                Category mecatronica = getCategory("Mecatrónica");
                Category perfumeria = getCategory("Perfumería");
                Category moda = getCategory("Moda");
                Category electrodomesticos = getCategory("Electrodomésticos");
                Category hogar = getCategory("Hogar");

                // --- PRODUCTO 1: ECO+ Sistema de Riego ---
                Product p1 = createBaseProduct("ECO+ Sistema de Riego Inteligente V2", "sistema-riego-eco-plus",
                                ecoPlus, new BigDecimal("450.00"), 100, true);
                p1.setMainImageUrl(
                                "https://images.unsplash.com/photo-1563514227147-6d2ff665a6a0?q=80&w=1000&auto=format&fit=crop");
                p1.setDescription(
                                "Revolucione su jardín con la precisión del sistema ECO+. Monitoreo en tiempo real de humedad de suelo, temperatura ambiental y consumo de agua.");
                p1.setCategories(new HashSet<>(Arrays.asList(mecatronica, hogar)));
                p1.setVideoUrl("https://www.youtube.com/watch?v=VIKO7CUN59c");
                p1.setGalleryImageUrls(Arrays.asList(
                                "https://images.unsplash.com/photo-1589923188900-85dae523342b?q=80&w=1000&auto=format&fit=crop",
                                "https://images.unsplash.com/photo-1592419044706-39796d40f98ca?q=80&w=1000&auto=format&fit=crop"));
                p1.setManuals(Arrays.asList(
                                ProductManual.builder().title("Guía de Instalación ECO+ V2").url(
                                                "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf")
                                                .build(),
                                ProductManual.builder().title("Ficha Técnica Sensores").url(
                                                "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf")
                                                .build()));
                Product sp1 = productRepository.save(p1);

                clearDetailLists(sp1);
                addDetailListQuiet(sp1, "Características Principales", "GRID", Arrays.asList("Monitoreo 24/7 Cloud",
                                "Sensor Suelo Capacitivo", "Modo Automático AI", "Alertas de Sequía"));
                addDetailListQuiet(sp1, "¿Qué hay en la caja?", "LIST", Arrays.asList("Unidad Central HUB",
                                "Sensor Humedad Suelo", "Electroválvula 12V", "Fuente Poder IP67"));
                addDetailListQuiet(sp1, "Especificaciones del Hardware", "SPEC",
                                Arrays.asList("Procesador: ESP32 Dual Core", "WiFi: 2.4GHz Largo Alcance",
                                                "Protección: IP65 Outdoor"));

                // --- PRODUCTO 2: Invernadero Automatizado GardenPRO ---
                Product p2 = createBaseProduct("Invernadero Automatizado Pro Cloud", "invernadero-pro", gardenPro,
                                new BigDecimal("850.00"), 50, true);
                p2.setMainImageUrl(
                                "https://images.unsplash.com/photo-1585320806297-9794b3e4eeae?q=80&w=1000&auto=format&fit=crop");
                p2.setDescription(
                                "Controle su microclima desde cualquier lugar. Integra sensores de CO2, humedad y luz para maximizar el crecimiento de sus cultivos.");
                p2.setCategories(new HashSet<>(Arrays.asList(mecatronica, hogar)));
                p2.setVideoUrl("https://www.youtube.com/watch?v=W79d0Nnzb4I");
                p2.setGalleryImageUrls(Arrays.asList(
                                "https://images.unsplash.com/photo-1592419044706-39796d40f98c?q=80&w=1000&auto=format&fit=crop"));
                p2.setManuals(Arrays.asList(
                                ProductManual.builder().title("Manual de Ensamblaje Estructura").url(
                                                "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf")
                                                .build(),
                                ProductManual.builder().title("Guía de Control IoT Invernadero").url(
                                                "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf")
                                                .build()));
                Product sp2 = productRepository.save(p2);

                clearDetailLists(sp2);
                addDetailListQuiet(sp2, "Ventajas del Cultivo Inteligente", "GRID", Arrays.asList("Optimización de CO2",
                                "Control de Luz Solar", "Riego de Precisión", "Cosechas en Menor Tiempo"));
                addDetailListQuiet(sp2, "Sensores de Grado Agrícola", "LIST", Arrays.asList("Sensor NDIR CO2",
                                "Luxómetro Digital", "Humedad Relativa", "Temperatura Foliar"));
                addDetailListQuiet(sp2, "Ficha Estructural", "SPEC", Arrays.asList("Material: Policarbonato Alveolar",
                                "Resistencia Viento: 100km/h", "Área Cultivo: 6m2"));

                // --- PRODUCTO 3: Alimentador de Mascotas Cloud ---
                Product p3 = createBaseProduct("PetBuddy Feeder Cloud V2", "pet-feeder-cloud", petBuddy,
                                new BigDecimal("145.00"), 80, true);
                p3.setMainImageUrl(
                                "https://panamericana.vtexassets.com/arquivos/ids/427292-800-auto?v=637802805122800000");
                p3.setDescription(
                                "No se preocupe por el horario de su mascota. Programe porciones exactas y monitoree el consumo de alimento en tiempo real.");
                p3.setCategories(new HashSet<>(Arrays.asList(mecatronica, hogar)));
                p3.setVideoUrl("https://www.youtube.com/watch?v=Fj-6-0L_F9w");
                p3.setGalleryImageUrls(Arrays.asList(
                                "https://images.unsplash.com/photo-1583511655857-d19b40a7a54e?q=80&w=1000&auto=format&fit=crop",
                                "https://images.unsplash.com/photo-1516734212186-a967f81ad0d7?q=80&w=1000&auto=format&fit=crop"));
                p3.setManuals(Arrays.asList(
                                ProductManual.builder().title("Guía Rápida de Configuración App").url(
                                                "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf")
                                                .build()));
                Product sp3 = productRepository.save(p3);

                clearDetailLists(sp3);
                addDetailListQuiet(sp3, "Funciones Smart", "GRID", Arrays.asList("Audio Bi-direccional",
                                "Apertura Anti-Bloqueo", "Sincronización Cloud", "Historial de Consumo"));
                addDetailListQuiet(sp3, "Mascotas Compatibles", "LIST",
                                Arrays.asList("Perros Pequeños/Medianos", "Gatos", "Cachorros en crecimiento"));
                addDetailListQuiet(sp3, "Dimensiones y Capacidad", "SPEC", Arrays.asList("Capacidad Tanque: 4 Litros",
                                "Material: Plástico Grado Alimentario", "Batería Respaldo: 3 Pilas D"));

                // --- PRODUCTO 4: Sauvage Parfum ---
                Product p4 = createBaseProduct("Sauvage Parfum for Men Premium", "sauvage-parfum-men", dior,
                                new BigDecimal("125.00"), 200, false);
                p4.setMainImageUrl(
                                "https://www.lexormiami.com/cdn/shop/products/Y0998004_C099600456_E01_ZHC_800x.jpg?v=1750782598");
                p4.setDescription(
                                "Una fragancia de alta concentración que fusiona la frescura extrema con cálidas notas orientales.");
                p4.setCategories(new HashSet<>(Arrays.asList(perfumeria)));
                p4.setGalleryImageUrls(Arrays.asList(
                                "https://images.unsplash.com/photo-1541643600914-78b084683601?q=80&w=1000&auto=format&fit=crop",
                                "https://images.unsplash.com/photo-1594035910387-fea47794261f?q=80&w=1000&auto=format&fit=crop"));
                Product sp4 = productRepository.save(p4);
                clearDetailLists(sp4);
                addDetailListQuiet(sp4, "Pirámide Olfativa", "LIST", Arrays.asList("Salida: Bergamota de Reggio",
                                "Corazón: Absoluto de Vainilla", "Fondo: Ámbar Gris"));

                // --- PRODUCTO 5: Chaqueta de Cuero Vintage ---
                Product p5 = createBaseProduct("Chaqueta Leather Vintage Original", "chaqueta-cuero-biker", vintageCo,
                                new BigDecimal("190.00"), 45, false);
                p5.setMainImageUrl(
                                "https://images.unsplash.com/photo-1551028719-00167b16eac5?q=80&w=1000&auto=format&fit=crop");
                p5.setDescription("Confeccionada 100% en cuero genuino premium con acabados artesanales.");
                p5.setCategories(new HashSet<>(Arrays.asList(moda)));
                p5.setGalleryImageUrls(Arrays.asList(
                                "https://images.unsplash.com/photo-1521223890158-f9f7c3d5d504?q=80&w=1000&auto=format&fit=crop",
                                "https://images.unsplash.com/photo-1495105787522-5334e3ffa0f5?q=80&w=1000&auto=format&fit=crop"));
                Product sp5 = productRepository.save(p5);
                clearDetailLists(sp5);
                addDetailListQuiet(sp5, "Guía de Tallas", "SPEC",
                                Arrays.asList("Talla S: 95cm Pecho", "Talla M: 105cm Pecho", "Talla L: 115cm Pecho"));

                // --- PRODUCTO 6: Freidora de Aire Ninja ---
                Product p6 = createBaseProduct("Freidora de Aire Ninja Foodi Dual", "ninja-foodi-fryer", ninja,
                                new BigDecimal("240.00"), 60, false);
                p6.setMainImageUrl(
                                "https://groupesebcol.vtexassets.com/arquivos/ids/171065/1510002546-1.jpg");
                p6.setDescription(
                                "Cocine como un profesional con la Ninja Foodi de 2 canastas independientes. 6 funciones en 1.");
                p6.setCategories(new HashSet<>(Arrays.asList(electrodomesticos, hogar)));
                p6.setGalleryImageUrls(Arrays.asList(
                                "https://images.unsplash.com/photo-1626074353765-517a681e40be?q=80&w=1000&auto=format&fit=crop",
                                "https://images.unsplash.com/photo-1626778103100-33230c14eee4?q=80&w=1000&auto=format&fit=crop"));
                p6.setManuals(Arrays.asList(ProductManual.builder().title("Manual de Cocina Ninja")
                                .url("https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf")
                                .build()));
                Product sp6 = productRepository.save(p6);
                clearDetailLists(sp6);
                addDetailListQuiet(sp6, "Funciones Incluidas", "GRID", Arrays.asList("Max Crisp", "Asar", "Hornear",
                                "Recalentar", "Deshidratar", "Freír con Aire"));

                return ResponseEntity.ok("Catálogo Creado SATISFACTORIAMENTE (6 Productos)");
        }

        private void clearDetailLists(Product p) {
                detailListRepository.deleteByProductId(p.getId());
        }

        private void addDetailListQuiet(Product p, String title, String type, List<String> items) {
                ProductDetailList list = ProductDetailList.builder()
                                .title(title)
                                .displayType(type)
                                .product(p)
                                .items(items)
                                .build();
                detailListRepository.save(list);
        }

        private Brand getBrand(String name) {
                return brandRepository.findByNameIgnoreCase(name)
                                .orElseGet(() -> brandRepository.save(Brand.builder().name(name).build()));
        }

        private Category getCategory(String name) {
                return categoryRepository.findByNameIgnoreCase(name)
                                .orElseGet(() -> categoryRepository.save(Category.builder().name(name).build()));
        }

        private Product createBaseProduct(String name, String slug, Brand brand, BigDecimal price, int stock,
                        boolean isMecatronic) {
                Product p = productRepository.findBySlug(slug).orElse(new Product());
                p.setName(name);
                p.setSlug(slug);
                p.setBrand(brand);
                p.setPrice(price);
                p.setPurchasePrice(price.multiply(new BigDecimal("0.4")));
                p.setStock(stock);
                p.setIsActive(true);
                p.setIsMecatronic(isMecatronic);
                if (p.getCategories() == null)
                        p.setCategories(new HashSet<>());
                if (p.getGalleryImageUrls() == null)
                        p.setGalleryImageUrls(new ArrayList<>());
                if (p.getManuals() == null)
                        p.setManuals(new ArrayList<>());
                return p;
        }

        /**
         * [DEV] Purga todos los productos del catálogo sembrado.
         * Requiere token de confirmación para evitar borrados accidentales.
         * SOLO usar en entornos de desarrollo.
         */
        @DeleteMapping("/purge")
        @Transactional
        public ResponseEntity<String> purgeCatalog(@RequestParam(defaultValue = "") String confirm) {
                if (!"CONFIRMAR".equals(confirm)) {
                        return ResponseEntity.badRequest()
                                .body("⚠️ Operación peligrosa. Envía ?confirm=CONFIRMAR para proceder. Esto eliminará TODOS los productos.");
                }
                
                long count = productRepository.count();

                // 1. Limpiar dependencias IoT (Crucial para evitar Error 500 por FK)
                variableReadingRepository.deleteAllInBatch();
                deviceActionRepository.deleteAllInBatch();
                deviceVariableRepository.deleteAllInBatch();
                userLinkedDeviceRepository.deleteAllInBatch();
                mecatronicDeviceRepository.deleteAllInBatch();

                // 2. Limpiar dependencias de productos
                offerRepository.deleteAllInBatch();
                reviewRepository.deleteAllInBatch();
                reservationItemRepository.deleteAllInBatch();
                detailListRepository.deleteAllInBatch();

                // 3. Limpiar tablas @ElementCollection y @ManyToMany usando queries nativas directas
                executeNative("DELETE FROM product_manuals");
                executeNative("DELETE FROM product_images");
                executeNative("DELETE FROM product_categories");

                // 4. Borrado FÍSICO final de productos (Hard Delete)
                productRepository.hardDeleteAllProducts();

                return ResponseEntity.ok("🗑️ Catálogo purgado. " + count + " productos eliminados físicamente de la base de datos.");
        }

        private void executeNative(String sql) {
                entityManager.createNativeQuery(sql).executeUpdate();
        }
}


