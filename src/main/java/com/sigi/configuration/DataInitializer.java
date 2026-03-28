package com.sigi.configuration;

import com.sigi.persistence.entity.*;
import com.sigi.persistence.enums.InvoiceStatus;
import com.sigi.persistence.enums.MovementType;
import com.sigi.persistence.enums.OrderStatus;
import com.sigi.persistence.enums.RoleList;
import com.sigi.persistence.repository.*;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Log4j2
@Profile("!test")
public class DataInitializer {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final ClientRepository clientRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final DispatcherRepository dispatcherRepository;
    private final OrderRepository orderRepository;
    private final OrderLineRepository orderLineRepository;
    private final InvoiceRepository invoiceRepository;
    private final MovementRepository movementRepository;

    private List<User> users = new ArrayList<>();
    private List<Client> clients = new ArrayList<>();
    private List<Warehouse> warehouses = new ArrayList<>();
    private List<Product> products = new ArrayList<>();
    private List<Inventory> inventories = new ArrayList<>();
    private List<Dispatcher> dispatchers = new ArrayList<>();
    private List<Order> orders = new ArrayList<>();
    private List<OrderLine> orderLines = new ArrayList<>();
    private List<Invoice> invoices = new ArrayList<>();


    private Role getOrCreateRole(RoleList roleName) {
        return roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(new Role(null, roleName)));
    }

    @PostConstruct
    @Transactional
    public void initData() {
        createUsersIfNotExist();
        createClientsIfNotExist();
        createWarehousesIfNotExist();
        createProductsIfNotExist();
        createDispatchersIfNotExist();
        createInventoriesIfNotExist();
        createOrdersIfNotExist();
        createOrderLinesIfNotExist();
        createInvoicesIfNotExist();
        createMovementsIfNotExist();
    }

    @Transactional
    private void createUsersIfNotExist() {
        long existingUsers = userRepository.count();
        if (existingUsers > 0) {
            log.info("DataInitializer: usuarios ya existen (count={}), omitiendo inicialización.", existingUsers);
            return;
        }

        Role roleAdmin = getOrCreateRole(RoleList.ROLE_ADMIN);
        Role roleWarehouse = getOrCreateRole(RoleList.ROLE_WAREHOUSE);
        Role roleSeller = getOrCreateRole(RoleList.ROLE_SELLER);
        Role roleAuditor = getOrCreateRole(RoleList.ROLE_AUDITOR);
        Role roleDispatcher = getOrCreateRole(RoleList.ROLE_DISPATCHER);

        String rawPassword = "123456789";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        users = new ArrayList<>(List.of(
                User.builder().name("Andrés").surname("Arias").phoneNumber("3104000001")
                        .email("admin@sigi.com").password(encodedPassword).role(roleAdmin).active(true).build(),

                User.builder().name("María").surname("López").phoneNumber("3104000002")
                        .email("warehouse@sigi.com").password(encodedPassword).role(roleWarehouse).active(true).build(),

                User.builder().name("Juan").surname("Ramírez").phoneNumber("3104000003")
                        .email("seller@sigi.com").password(encodedPassword).role(roleSeller).active(true).build(),

                User.builder().name("Catalina").surname("Pérez").phoneNumber("3104000004")
                        .email("auditor@sigi.com").password(encodedPassword).role(roleAuditor).active(true).build(),

                User.builder().name("Diego").surname("Martínez").phoneNumber("3104000005")
                        .email("dispatcher@sigi.com").password(encodedPassword).role(roleDispatcher).active(true).build(),

                User.builder().name("Laura").surname("Gómez").phoneNumber("3104000006")
                        .email("laura.gomez@sigi.com").password(encodedPassword).role(roleSeller).active(true).build(),

                User.builder().name("Carlos").surname("Rodríguez").phoneNumber("3104000007")
                        .email("carlos.rodriguez@sigi.com").password(encodedPassword).role(roleWarehouse).active(true).build(),

                User.builder().name("Paula").surname("Sánchez").phoneNumber("3104000008")
                        .email("paula.sanchez@sigi.com").password(encodedPassword).role(roleAdmin).active(true).build(),

                User.builder().name("Santiago").surname("Vargas").phoneNumber("3104000009")
                        .email("santiago.vargas@sigi.com").password(encodedPassword).role(roleSeller).active(true).build(),

                User.builder().name("Valentina").surname("Cárdenas").phoneNumber("3104000010")
                        .email("valentina.cardenas@sigi.com").password(encodedPassword).role(roleAuditor).active(true).build(),

                User.builder().name("Felipe").surname("Castro").phoneNumber("3104000011")
                        .email("felipe.castro@sigi.com").password(encodedPassword).role(roleDispatcher).active(true).build(),

                User.builder().name("Natalia").surname("Ríos").phoneNumber("3104000012")
                        .email("natalia.rios@sigi.com").password(encodedPassword).role(roleWarehouse).active(true).build(),

                User.builder().name("Andrés Felipe").surname("Quintero").phoneNumber("3104000013")
                        .email("andresf.quintero@sigi.com").password(encodedPassword).role(roleAdmin).active(true).build(),

                User.builder().name("Mónica").surname("Herrera").phoneNumber("3104000014")
                        .email("monica.herrera@sigi.com").password(encodedPassword).role(roleSeller).active(true).build(),

                User.builder().name("Javier").surname("Córdoba").phoneNumber("3104000015")
                        .email("javier.cordoba@sigi.com").password(encodedPassword).role(roleDispatcher).active(true).build()));

        List<User> toSave = new ArrayList<>();
        for (User u : users) {
            if (!userRepository.existsByEmail(u.getEmail())) {
                toSave.add(u);
            } else {
                log.info("DataInitializer: usuario con email {} ya existe, omitiendo.", u.getEmail());
            }
        }

        if (!toSave.isEmpty()) {
            userRepository.saveAll(toSave);
            log.info("DataInitializer: {} usuarios creados.", toSave.size());
        } else {
            log.info("DataInitializer: no hay usuarios nuevos para crear.");
        }
    }

    @Transactional
    private void createClientsIfNotExist() {
        long existingClients = clientRepository.count();
        if (existingClients > 0) {
            log.info("DataInitializer: clientes ya existen (count={}), omitiendo inicialización de clients.", existingClients);
            return;
        }

        clients = new ArrayList<>(List.of(
                Client.builder().name("Supermercado La 14 Armenia").identification("900123456-7")
                        .location("Armenia, Quindío").phone("6067451234").email("contacto.la14@sigi.com").active(true).build(),

                Client.builder().name("Distribuidora El Cafetal").identification("901987654-3")
                        .location("Calarcá, Quindío").phone("6067455678").email("ventas.cafetal@sigi.com").active(true).build(),

                Client.builder().name("D1 Armenia").identification("901234567-0")
                        .location("Armenia, Quindío").phone("6067654321").email("contacto.d1@sigi.com").active(true).build(),

                Client.builder().name("Éxito Quindío").identification("902111222-8")
                        .location("Armenia, Quindío").phone("6061234567").email("proveedores.exito@sigi.com").active(true).build(),

                Client.builder().name("Justo & Bueno Pereira").identification("902345678-1")
                        .location("Pereira, Risaralda").phone("6068765432").email("ventas.justoybueno@sigi.com").active(true).build(),

                Client.builder().name("Makro Pereira").identification("903456789-2")
                        .location("Pereira, Risaralda").phone("6019876543").email("compras.makro@sigi.com").active(true).build(),

                Client.builder().name("Ara Montenegro").identification("904567890-3")
                        .location("Montenegro, Quindío").phone("6062345678").email("contacto.ara@sigi.com").active(true).build(),

                Client.builder().name("Alkosto Manizales").identification("905678901-4")
                        .location("Manizales, Caldas").phone("6023456789").email("proveedores.alkosto@sigi.com").active(true).build(),

                Client.builder().name("Surtimax Dosquebradas").identification("906789012-5")
                        .location("Dosquebradas, Risaralda").phone("6044567890").email("ventas.surtimax@sigi.com").active(true).build(),

                Client.builder().name("Distribuciones del Café").identification("907890123-6")
                        .location("La Tebaida, Quindío").phone("6069988776").email("contacto.distribuciones@sigi.com").active(true).build(),
                Client.builder().name("Mercacentro La Estrella").identification("908112233-7")
                        .location("Armenia, Quindío").phone("6067123456").email("contacto.mercacentro@sigi.com").active(true).build(),

                Client.builder().name("Mayorista El Roble").identification("909223344-8")
                        .location("Pereira, Risaralda").phone("6067234567").email("ventas.elroble@sigi.com").active(true).build(),

                Client.builder().name("Distribuciones Cafeteras S.A.").identification("910334455-9")
                        .location("Manizales, Caldas").phone("6067345678").email("contacto.cafeteras@sigi.com").active(true).build(),

                Client.builder().name("Almacenes La 20").identification("911445566-0")
                        .location("Montenegro, Quindío").phone("6067456789").email("info.la20@sigi.com").active(true).build(),

                Client.builder().name("Mayorista del Valle").identification("912556677-1")
                        .location("Dosquebradas, Risaralda").phone("6067567890").email("compras.mayorista@sigi.com").active(true).build()
        ));

        List<Client> toSave = new ArrayList<>();
        for (Client c : clients) {
            boolean exists = false;
            if (c.getIdentification() != null && !c.getIdentification().isBlank()) {
                exists = clientRepository.existsByIdentification(c.getIdentification());
            }
            if (!exists && c.getEmail() != null && !c.getEmail().isBlank()) {
                exists = clientRepository.existsByEmail(c.getEmail());
            }
            if (!exists) {
                toSave.add(c);
            } else {
                log.info("DataInitializer: cliente con identificación/email ya existe, omitiendo: {} / {}", c.getIdentification(), c.getEmail());
            }
        }

        if (!toSave.isEmpty()) {
            clientRepository.saveAll(toSave);
            log.info("DataInitializer: {} clientes creados.", toSave.size());
        } else {
            log.info("DataInitializer: no hay clientes nuevos para crear.");
        }
    }

    @Transactional
    private void createWarehousesIfNotExist() {
        long existing = warehouseRepository.count();
        if (existing > 0) {
            log.info("DataInitializer: warehouses ya existen (count={}), omitiendo inicialización de warehouses.", existing);
            return;
        }

        warehouses = new ArrayList<>(List.of(
                Warehouse.builder().name("Bodega Central Armenia").location("Zona Industrial Armenia").totalCapacity(12000).active(true).build(),
                Warehouse.builder().name("Bodega Norte Armenia").location("Sector La Castellana, Armenia").totalCapacity(6000).active(true).build(),
                Warehouse.builder().name("Bodega Sur Armenia").location("Sector La Fachada, Armenia").totalCapacity(7000).active(true).build(),
                Warehouse.builder().name("Bodega Pereira Central").location("Zona Industrial Cuba, Pereira").totalCapacity(14000).active(true).build(),
                Warehouse.builder().name("Bodega Dosquebradas").location("Parque Industrial Dosquebradas").totalCapacity(8000).active(true).build(),
                Warehouse.builder().name("Bodega Manizales Norte").location("Polígono Industrial Manizales").totalCapacity(9000).active(true).build(),
                Warehouse.builder().name("Bodega La Tebaida").location("La Tebaida, Quindío").totalCapacity(5000).active(true).build(),
                Warehouse.builder().name("Bodega Montenegro").location("Montenegro, Quindío").totalCapacity(4500).active(true).build(),
                Warehouse.builder().name("Bodega Caldas Logística").location("Chinchiná, Caldas").totalCapacity(7000).active(true).build(),
                Warehouse.builder().name("Bodega Armenia Este").location("Barrio El Bosque, Armenia").totalCapacity(5500).active(true).build(),
                Warehouse.builder().name("Bodega Pereira Este").location("Sector La Julita, Pereira").totalCapacity(6000).active(true).build(),
                Warehouse.builder().name("Bodega Manizales Sur").location("Zona Franca Manizales").totalCapacity(10000).active(true).build(),
                Warehouse.builder().name("Bodega La Virginia").location("La Virginia, Risaralda").totalCapacity(4800).active(true).build(),
                Warehouse.builder().name("Bodega Santa Rosa").location("Santa Rosa de Cabal, Risaralda").totalCapacity(5200).active(true).build(),
                Warehouse.builder().name("Bodega Eje Cafetero Central").location("Corredor Logístico Eje Cafetero").totalCapacity(20000).active(true).build()
        ));

        List<Warehouse> toSave = new ArrayList<>();
        for (Warehouse w : warehouses) {
            if (!warehouseRepository.existsByName(w.getName())) {
                toSave.add(w);
            } else {
                log.info("DataInitializer: warehouse con nombre {} ya existe, omitiendo.", w.getName());
            }
        }

        if (!toSave.isEmpty()) {
            warehouseRepository.saveAll(toSave);
            log.info("DataInitializer: {} warehouses creados.", toSave.size());
        } else {
            log.info("DataInitializer: no hay warehouses nuevos para crear.");
        }
    }

    @Transactional
    private void createProductsIfNotExist() {
        long existing = productRepository.count();
        if (existing > 0) {
            log.info("DataInitializer: products ya existen (count={}), omitiendo inicialización de products.", existing);
            return;
        }

        products = List.of(
                Product.builder().sku("SKU-011").name("Yogurt Alpina 125g").category("Lácteos").unit("UN")
                        .price(BigDecimal.valueOf(1200)).barcode("7701110000111").imageUrl("https://example.com/yogurt.jpg").active(true).build(),
                Product.builder().sku("SKU-012").name("Pan Integral 500g").category("Panadería").unit("UN")
                        .price(BigDecimal.valueOf(4200)).barcode("7701110000222").imageUrl("https://example.com/panintegral.jpg").active(true).build(),
                Product.builder().sku("SKU-013").name("Galletas Festival 200g").category("Aperitivos").unit("UN")
                        .price(BigDecimal.valueOf(3500)).barcode("7701110000333").imageUrl("https://example.com/galletas.jpg").active(true).build(),
                Product.builder().sku("SKU-014").name("Refresco Colombiana 2L").category("Bebidas").unit("UN")
                        .price(BigDecimal.valueOf(7200)).barcode("7701110000444").imageUrl("https://example.com/refresco.jpg").active(true).build(),
                Product.builder().sku("SKU-015").name("Jugo Hit 1L").category("Bebidas").unit("UN")
                        .price(BigDecimal.valueOf(4800)).barcode("7701110000555").imageUrl("https://example.com/jugo.jpg").active(true).build(),
                Product.builder().sku("SKU-016").name("Queso Campesino 400g").category("Lácteos").unit("UN")
                        .price(BigDecimal.valueOf(9800)).barcode("7701110000666").imageUrl("https://example.com/queso.jpg").active(true).build(),
                Product.builder().sku("SKU-017").name("Café Molido Eje Cafetero 250g").category("Bebidas").unit("UN")
                        .price(BigDecimal.valueOf(14500)).barcode("7701110000777").imageUrl("https://example.com/cafe_eje.jpg").active(true).build(),
                Product.builder().sku("SKU-018").name("Azúcar Morena 1kg").category("Alimentos").unit("KG")
                        .price(BigDecimal.valueOf(3600)).barcode("7701110000888").imageUrl("https://example.com/azucar_morena.jpg").active(true).build(),
                Product.builder().sku("SKU-019").name("Harina Pan 1kg").category("Alimentos").unit("KG")
                        .price(BigDecimal.valueOf(4200)).barcode("7701110000999").imageUrl("https://example.com/harina.jpg").active(true).build(),
                Product.builder().sku("SKU-020").name("Leche UHT 1L").category("Lácteos").unit("UN")
                        .price(BigDecimal.valueOf(3800)).barcode("7701110001005").imageUrl("https://example.com/leche_uht.jpg").active(true).build(),
                Product.builder().sku("SKU-021").name("Aceite Girasol 900ml").category("Aceites").unit("UN")
                        .price(BigDecimal.valueOf(12500)).barcode("7701110001116").imageUrl("https://example.com/aceite_girasol.jpg").active(true).build(),
                Product.builder().sku("SKU-022").name("Salsa de Tomate 400g").category("Conservas").unit("UN")
                        .price(BigDecimal.valueOf(3200)).barcode("7701110001227").imageUrl("https://example.com/salsa.jpg").active(true).build(),
                Product.builder().sku("SKU-023").name("Atún en Agua 170g").category("Conservas").unit("UN")
                        .price(BigDecimal.valueOf(7600)).barcode("7701110001338").imageUrl("https://example.com/atun.jpg").active(true).build(),
                Product.builder().sku("SKU-024").name("Detergente Líquido 1L").category("Aseo").unit("UN")
                        .price(BigDecimal.valueOf(15800)).barcode("7701110001449").imageUrl("https://example.com/detergente_liq.jpg").active(true).build(),
                Product.builder().sku("SKU-025").name("Jabón en Barra 3un").category("Aseo").unit("UN")
                        .price(BigDecimal.valueOf(4200)).barcode("7701110001550").imageUrl("https://example.com/jabon.jpg").active(true).build(),
                Product.builder().sku("SKU-026").name("Pasta Dental 90g").category("Aseo").unit("UN")
                        .price(BigDecimal.valueOf(6800)).barcode("7701110001661").imageUrl("https://example.com/pasta.jpg").active(true).build(),
                Product.builder().sku("SKU-027").name("Bebida Energética 500ml").category("Bebidas").unit("UN")
                        .price(BigDecimal.valueOf(7200)).barcode("7701110001772").imageUrl("https://example.com/energetica.jpg").active(true).build(),
                Product.builder().sku("SKU-028").name("Snack Papas 120g").category("Aperitivos").unit("UN")
                        .price(BigDecimal.valueOf(3800)).barcode("7701110001883").imageUrl("https://example.com/papas.jpg").active(true).build(),
                Product.builder().sku("SKU-029").name("Mermelada Fresa 300g").category("Conservas").unit("UN")
                        .price(BigDecimal.valueOf(5400)).barcode("7701110001994").imageUrl("https://example.com/mermelada.jpg").active(true).build(),
                Product.builder().sku("SKU-030").name("Gaseosa Cola 330ml").category("Bebidas").unit("UN")
                        .price(BigDecimal.valueOf(2200)).barcode("7701110002007").imageUrl("https://example.com/cola330.jpg").active(true).build()
        );

        List<Product> toSave = new ArrayList<>();
        for (Product p : products) {
            boolean exists = false;
            if (p.getSku() != null && !p.getSku().isBlank()) {
                exists = productRepository.existsBySku(p.getSku());
            }
            if (!exists && p.getBarcode() != null && !p.getBarcode().isBlank()) {
                exists = productRepository.existsByBarcode(p.getBarcode());
            }
            if (!exists) {
                toSave.add(p);
            } else {
                log.info("DataInitializer: producto con sku/barcode ya existe, omitiendo: {} / {}", p.getSku(), p.getBarcode());
            }
        }

        if (!toSave.isEmpty()) {
            productRepository.saveAll(toSave);
            log.info("DataInitializer: {} productos creados.", toSave.size());
        } else {
            log.info("DataInitializer: no hay productos nuevos para crear.");
        }
    }

    @Transactional
    private void createInventoriesIfNotExist() {
        long existing = inventoryRepository.count();
        if (existing > 0) {
            log.info("DataInitializer: inventories ya existen (count={}), omitiendo inicialización de inventories.", existing);
            return;
        }

        if (this.products == null || this.products.isEmpty()) {
            this.products = productRepository.findAll();
        }
        if (this.warehouses == null || this.warehouses.isEmpty()) {
            this.warehouses = warehouseRepository.findAll();
        }

        if (this.products.isEmpty() || this.warehouses.isEmpty()) {
            log.warn("DataInitializer: no hay products o warehouses disponibles para crear inventories. products={}, warehouses={}",
                    this.products.size(), this.warehouses.size());
            return;
        }

        inventories = new ArrayList<>();
        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            Warehouse w = warehouses.get(i % warehouses.size());

            String lot = String.format("LOT-%s-%s", p.getSku(), LocalDate.now().getYear());

            boolean exists = inventoryRepository.existsByProductAndWarehouseAndLot(p, w, lot);
            if (exists) {
                log.info("DataInitializer: inventory ya existe para product {} en warehouse {} con lot {}, omitiendo.",
                        p.getSku(), w.getName(), lot);
                continue;
            }

            Inventory inv = Inventory.builder()
                    .product(p)
                    .warehouse(w)
                    .location("Estante " + (char) ('A' + (i % 10)) + (1 + (i % 12)))
                    .lot(lot)
                    .productionDate(LocalDate.now().minusMonths(3))
                    .expirationDate(LocalDate.now().plusMonths(12))
                    .availableQuantity(BigDecimal.valueOf(100 + (i * 10)))
                    .reservedQuantity(BigDecimal.valueOf(5 + (i % 5)))
                    .active(true)
                    .build();

            inventories.add(inv);
        }

        if (!inventories.isEmpty()) {
            inventoryRepository.saveAll(inventories);
            log.info("DataInitializer: {} inventories creados.", inventories.size());
        } else {
            log.info("DataInitializer: no hay inventories nuevos para crear.");
        }
    }

    @Transactional
    private void createDispatchersIfNotExist() {
        long existing = dispatcherRepository.count();
        if (existing > 0) {
            log.info("DataInitializer: dispatchers ya existen (count={}), omitiendo inicialización de dispatchers.", existing);
            return;
        }

        dispatchers = List.of(
                Dispatcher.builder().name("Transportes Rápidos S.A.").contact("Juan Pérez").phone("3021234567")
                        .email("logistica.rapidos@sigi.com").active(true).identification("111111111").location("Armenia, Quindío").build(),

                Dispatcher.builder().name("Logística Express").contact("Carlos Gómez").phone("3019876543")
                        .email("contacto.express@sigi.com").active(true).identification("222222222").location("Pereira, Risaralda").build(),

                Dispatcher.builder().name("Envíos Nacionales Ltda").contact("María López").phone("3007654321")
                        .email("envios.nacionales@sigi.com").active(true).identification("333333333").location("Pitalito, Huila").build(),

                Dispatcher.builder().name("Distribuciones del Café").contact("Andrés Ramírez").phone("3041234567")
                        .email("distribuciones.cafe@sigi.com").active(true).identification("444444444").location("Calarcá, Quindío").build(),

                Dispatcher.builder().name("Carga Segura S.A.").contact("Paola Torres").phone("3052345678")
                        .email("carga.segura@sigi.com").active(true).identification("555555555").location("Balboa, Risaralda").build(),

                Dispatcher.builder().name("Transportes del Quindío").contact("Felipe Castro").phone("3063456789")
                        .email("transportes.quindio@sigi.com").active(true).identification("666666666").location("Armenia, Quindío").build(),

                Dispatcher.builder().name("Logística Andina").contact("Camila Rojas").phone("3074567890")
                        .email("logistica.andina@sigi.com").active(true).identification("777777777").location("Sabaneta, Antioquia").build(),

                Dispatcher.builder().name("Envíos Rápidos Armenia").contact("Jorge Hernández").phone("3085678901")
                        .email("envios.armenia@sigi.com").active(true).identification("888888888").location("Armenia, Quindío").build(),

                Dispatcher.builder().name("Distribuidora Nacional").contact("Diana Morales").phone("3096789012")
                        .email("distribuidora.nacional@sigi.com").active(true).identification("999999999").location("Cali, Valle del Cauca").build(),

                Dispatcher.builder().name("Transportes del Valle").contact("Luis Martínez").phone("3107890123")
                        .email("transportes.valle@sigi.com").active(true).identification("123456670").location("La Unión, Valle del Cauca").build()
        );

        List<Dispatcher> toSave = new ArrayList<>();
        for (Dispatcher d : dispatchers) {
            boolean exists = false;
            if (d.getIdentification() != null && !d.getIdentification().isBlank()) {
                exists = dispatcherRepository.existsByIdentification(d.getIdentification());
            }
            if (!exists && d.getEmail() != null && !d.getEmail().isBlank()) {
                exists = dispatcherRepository.existsByEmail(d.getEmail());
            }
            if (!exists) {
                toSave.add(d);
            } else {
                log.info("DataInitializer: dispatcher con identification/email ya existe, omitiendo: {} / {}", d.getIdentification(), d.getEmail());
            }
        }

        if (!toSave.isEmpty()) {
            dispatcherRepository.saveAll(toSave);
            log.info("DataInitializer: {} dispatchers creados.", toSave.size());
        } else {
            log.info("DataInitializer: no hay dispatchers nuevos para crear.");
        }
    }
    @Transactional
    private void createOrdersIfNotExist() {
        long existing = orderRepository.count();
        if (existing > 0) {
            log.info("DataInitializer: orders ya existen (count={}), omitiendo inicialización de orders.", existing);
            return;
        }

        if (this.clients == null || this.clients.isEmpty()) {
            this.clients = clientRepository.findAll();
        }
        if (this.users == null || this.users.isEmpty()) {
            this.users = userRepository.findAll();
        }
        if (this.warehouses == null || this.warehouses.isEmpty()) {
            this.warehouses = warehouseRepository.findAll();
        }
        if (this.dispatchers == null || this.dispatchers.isEmpty()) {
            this.dispatchers = dispatcherRepository.findAll();
        }

        if (clients.isEmpty() || users.isEmpty() || warehouses.isEmpty() || dispatchers.isEmpty()) {
            log.warn("DataInitializer: faltan entidades para crear orders. clients={}, users={}, warehouses={}, dispatchers={}",
                    clients.size(), users.size(), warehouses.size(), dispatchers.size());
            return;
        }

         orders = new ArrayList<>();
        int n = Math.min(10, clients.size());
        for (int i = 0; i < n; i++) {
            Client c = clients.get(i);
            User u = users.get(i % users.size());
            Warehouse w = warehouses.get(i % warehouses.size());
            Dispatcher d = dispatchers.get(i % dispatchers.size());

            BigDecimal total = BigDecimal.valueOf(10000 + (i * 5000));
            boolean exists = orderRepository.existsByClientAndWarehouseAndDispatcherAndTotal(c, w, d, total);
            if (exists) {
                log.info("DataInitializer: order ya existe para client {} en warehouse {} con dispatcher {} y total {}, omitiendo.",
                        c.getName(), w.getName(), d.getName(), total);
                continue;
            }

            Order order = Order.builder()
                    .client(c)
                    .user(u)
                    .warehouse(w)
                    .dispatcher(d)
                    .status(i % 3 == 0 ? OrderStatus.DRAFT : (i % 3 == 1 ? OrderStatus.CONFIRMED : OrderStatus.DELIVERED))
                    .total(total)
                    .active(true)
                    .build();

            orders.add(order);
        }

        if (!orders.isEmpty()) {
            orderRepository.saveAll(orders);
            log.info("DataInitializer: {} orders creados.", orders.size());
        } else {
            log.info("DataInitializer: no hay orders nuevos para crear.");
        }
    }
    @Transactional
    private void createOrderLinesIfNotExist() {
        long existing = orderLineRepository.count();
        if (existing > 0) {
            log.info("DataInitializer: order lines ya existen (count={}), omitiendo inicialización de order lines.", existing);
            return;
        }

        if (this.products == null || this.products.isEmpty()) {
            this.products = productRepository.findAll();
        }
        if (this.inventories == null || this.inventories.isEmpty()) {
            this.inventories = inventoryRepository.findAll();
        }
        List<Order> orders = orderRepository.findAll();

        if (orders.isEmpty() || products.isEmpty() || inventories.isEmpty()) {
            log.warn("DataInitializer: faltan orders/products/inventories para crear order lines. orders={}, products={}, inventories={}",
                    orders.size(), products.size(), inventories.size());
            return;
        }

        orderLines = new ArrayList<>();

        for (int i = 0; i < orders.size(); i++) {
            Order order = orders.get(i);
            int linesCount = 1 + (i % 3);
            for (int j = 0; j < linesCount; j++) {
                Product p = products.get((i + j) % products.size());
                Inventory inv = inventories.stream()
                        .filter(it -> it.getProduct().getSku().equals(p.getSku()))
                        .findFirst()
                        .orElse(inventories.get((i + j) % inventories.size())); // fallback

                String lot = inv.getLot() != null ? inv.getLot() : "LOT-" + p.getSku();

                boolean exists = orderLineRepository.existsByOrderAndProductAndLot(order, p, lot);
                if (exists) {
                    log.info("DataInitializer: orderLine ya existe para order {} product {} lot {}, omitiendo.",
                            order.getId(), p.getSku(), lot);
                    continue;
                }

                BigDecimal qty = BigDecimal.valueOf(1 + ((i + j) % 10));
                BigDecimal unitPrice = p.getPrice() != null ? p.getPrice() : BigDecimal.ZERO;

                OrderLine line = OrderLine.builder()
                        .order(order)
                        .product(p)
                        .inventory(inv)
                        .lot(lot)
                        .quantity(qty)
                        .unitPrice(unitPrice)
                        .active(true)
                        .build();

                orderLines.add(line);

                order.setTotal(order.getTotal().add(unitPrice.multiply(new BigDecimal(qty.toString()))));
            }
        }

        if (!orderLines.isEmpty()) {
            orderLineRepository.saveAll(orderLines);
            orderRepository.saveAll(orderRepository.findAllById(
                    orderLines.stream().map(ol -> ol.getOrder().getId()).distinct().toList()
            ));
            log.info("DataInitializer: {} order lines creadas.", orderLines.size());
        } else {
            log.info("DataInitializer: no hay order lines nuevas para crear.");
        }
    }

    @Transactional
    private void createInvoicesIfNotExist() {
        long existing = invoiceRepository.count();
        if (existing > 0) {
            log.info("DataInitializer: invoices ya existen (count={}), omitiendo inicialización de invoices.", existing);
            return;
        }

        List<Order> orders = orderRepository.findAll();
        if (orders.isEmpty()) {
            log.warn("DataInitializer: no hay orders para generar invoices.");
            return;
        }

       invoices = new ArrayList<>();
        int sequence = 1;

        for (Order order : orders) {
            boolean invoiceExists = invoiceRepository.existsByOrder(order);
            if (invoiceExists) {
                log.info("DataInitializer: invoice ya existe para order {}, omitiendo.", order.getId());
                continue;
            }

            List<OrderLine> lines = orderLineRepository.findByOrder(order);
            if (lines == null || lines.isEmpty()) {
                log.info("DataInitializer: order {} no tiene líneas, omitiendo invoice.", order.getId());
                continue;
            }

            BigDecimal subtotal = lines.stream()
                    .map(l -> l.getUnitPrice().multiply(new BigDecimal(l.getQuantity().toString())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal taxRate = BigDecimal.valueOf(0.16);
            BigDecimal tax = subtotal.multiply(taxRate).setScale(2, BigDecimal.ROUND_HALF_UP);
            BigDecimal total = subtotal.add(tax).setScale(2, BigDecimal.ROUND_HALF_UP);

            String year = String.valueOf(LocalDate.now().getYear());
            String number;
            do {
                number = String.format("INV-%s-%03d", year, sequence++);
            } while (invoiceRepository.existsByNumber(number));

            Invoice invoice = Invoice.builder()
                    .number(number)
                    .order(order)
                    .client(order.getClient())
                    .subtotal(subtotal.setScale(2, BigDecimal.ROUND_HALF_UP))
                    .tax(tax)
                    .total(total)
                    .status(InvoiceStatus.ISSUED)
                    .active(true)
                    .build();

            invoices.add(invoice);
        }

        if (!invoices.isEmpty()) {
            invoiceRepository.saveAll(invoices);
            log.info("DataInitializer: {} invoices creadas.", invoices.size());
        } else {
            log.info("DataInitializer: no hay invoices nuevas para crear.");
        }
    }
    @Transactional
    private void createMovementsIfNotExist() {
        long existing = movementRepository.count();
        if (existing > 0) {
            log.info("DataInitializer: movements ya existen (count={}), omitiendo inicialización de movements.", existing);
            return;
        }

        if (this.products == null || this.products.isEmpty()) {
            this.products = productRepository.findAll();
        }
        if (this.inventories == null || this.inventories.isEmpty()) {
            this.inventories = inventoryRepository.findAll();
        }
        if (this.users == null || this.users.isEmpty()) {
            this.users = userRepository.findAll();
        }
        if (this.orders == null || this.orders.isEmpty()) {
            this.orders = orderRepository.findAll();
        }
        if (this.dispatchers == null || this.dispatchers.isEmpty()) {
            this.dispatchers = dispatcherRepository.findAll();
        }

        if (products.isEmpty() || inventories.isEmpty() || users.isEmpty()) {
            log.warn("DataInitializer: faltan entidades para crear movements. products={}, inventories={}, users={}",
                    products.size(), inventories.size(), users.size());
            return;
        }

        List<Movement> toSave = new ArrayList<>();

        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            Inventory inv = inventories.stream()
                    .filter(it -> it.getProduct().getSku().equals(p.getSku()))
                    .findFirst()
                    .orElse(null);

            if (inv == null) {
                log.info("DataInitializer: no hay inventory para product {}, omitiendo movement.", p.getSku());
                continue;
            }

            User u = users.get(i % users.size());
            Order order = orders.isEmpty() ? null : orders.get(i % orders.size());
            Dispatcher d = dispatchers.isEmpty() ? null : dispatchers.get(i % dispatchers.size());

            MovementType type = (i % 2 == 0) ? MovementType.ENTRY : MovementType.EXIT;
            BigDecimal qty = BigDecimal.valueOf(5 + (i % 20)); // cantidades plausibles

            // Idempotencia: evita duplicados por inventory+product+order+type+quantity+createdAt aproximado
            boolean exists = movementRepository.existsByInventoryAndProductAndOrderAndTypeAndQuantity(
                    inv, p, order, type, qty
            );
            if (exists) {
                log.info("DataInitializer: movement ya existe para product {} inventory {} tipo {}, omitiendo.",
                        p.getSku(), inv.getId(), type);
                continue;
            }

            Movement m = Movement.builder()
                    .type(type)
                    .inventory(inv)
                    .product(p)
                    .quantity(qty)
                    .user(u)
                    .order(order)
                    .dispatcher(d)
                    .motive(type == MovementType.ENTRY ? "Ingreso inicial de " + p.getName() : "Salida inicial de " + p.getName())
                    .active(true)
                    .build();

            // Ajustar inventario en memoria antes de persistir
            if (type == MovementType.ENTRY) {
                inv.setAvailableQuantity(inv.getAvailableQuantity().add(qty));
            } else { // EXIT
                BigDecimal available = inv.getAvailableQuantity();
                BigDecimal newAvailable = available.subtract(qty);
                if (newAvailable.compareTo(BigDecimal.ZERO) < 0) {
                    // No forzar negativo: ajustar a cero y registrar advertencia
                    log.warn("DataInitializer: intento de salida mayor que disponible para inventory {} product {}. available={}, requested={}. Ajustando a 0.",
                            inv.getId(), p.getSku(), available, qty);
                    inv.setAvailableQuantity(BigDecimal.ZERO);
                } else {
                    inv.setAvailableQuantity(newAvailable);
                }
            }

            toSave.add(m);
        }

        if (!toSave.isEmpty()) {
            // Guardar movimientos y luego persistir los inventarios actualizados
            movementRepository.saveAll(toSave);
            inventoryRepository.saveAll(inventories); // persiste los cambios de cantidades
            log.info("DataInitializer: {} movements creados y {} inventories actualizados.", toSave.size(), inventories.size());
        } else {
            log.info("DataInitializer: no hay movements nuevos para crear.");
        }
    }

}