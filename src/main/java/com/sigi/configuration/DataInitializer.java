package com.sigi.configuration;

import com.sigi.persistence.entity.*;
import com.sigi.persistence.enums.InvoiceStatus;
import com.sigi.persistence.enums.MovementType;
import com.sigi.persistence.enums.OrderStatus;
import com.sigi.persistence.enums.RoleList;
import com.sigi.persistence.repository.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Log4j2
@Profile("!test")
public class DataInitializer {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final RoleRepository roleRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryRepository inventoryRepository;
    private final MovementRepository movementRepository;
    private final OrderRepository orderRepository;
    private final OrderLineRepository onlineOrderRepository;
    private final PasswordEncoder passwordEncoder;
    private final InvoiceRepository invoiceRepository;
    private final DispatcherRepository dispatcherRepository;


    private Role getRoleByName(RoleList roleName) {
        return roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(new Role(null, roleName)));
    }


    @PostConstruct
    public void initData() {
        Role roleAdmin = getRoleByName(RoleList.ROLE_ADMIN);
        Role roleWarehouse = getRoleByName(RoleList.ROLE_WAREHOUSE);
        Role roleSeller = getRoleByName(RoleList.ROLE_SELLER);
        Role roleAuditor = getRoleByName(RoleList.ROLE_AUDITOR);
        Role roleDispatcher = getRoleByName(RoleList.ROLE_DISPATCHER);

        List<User> users = List.of(
                User.builder().name("Admin").surname("Admin").phoneNumber("3067778899")
                        .email("admin@sigi.com").password(passwordEncoder.encode("123456789")).role(roleAdmin).active(true).build(),
                User.builder().name("Warehouse").surname("Warehouse").phoneNumber("3078889900")
                        .email("warehouse@sigi.com").password(passwordEncoder.encode("123456789")).role(roleWarehouse).active(true).build(),
                User.builder().name("Dispatcher").surname("dispatcher").phoneNumber("30788891")
                        .email("dispatcher@sigi.com").password(passwordEncoder.encode("123456789")).role(roleDispatcher).active(true).build(),
                User.builder().name("Seller").surname("Seller").phoneNumber("3089990011")
                        .email("seller@sigi.com").password(passwordEncoder.encode("123456789")).role(roleSeller).active(true).build(),
                User.builder().name("Auditor").surname("Auditor").phoneNumber("3090001122")
                        .email("auditor@sigi.com").password(passwordEncoder.encode("123456789")).role(roleAuditor).active(true).build(),
                User.builder().name("Carlos").surname("Ramírez").phoneNumber("3001112233")
                        .email("carlos.ramirez@sigi.com").password(passwordEncoder.encode("123456789")).role(roleAdmin).active(true).build(),
                User.builder().name("Ana").surname("Gómez").phoneNumber("3012223344")
                        .email("ana.gomez@sigi.com").password(passwordEncoder.encode("123456789")).role(roleWarehouse).active(true).build(),
                User.builder().name("Luis").surname("Martínez").phoneNumber("3023334455")
                        .email("luis.martinez@sigi.com").password(passwordEncoder.encode("123456789")).role(roleSeller).active(true).build(),
                User.builder().name("María").surname("Fernández").phoneNumber("3034445566")
                        .email("maria.fernandez@sigi.com").password(passwordEncoder.encode("123456789")).role(roleAuditor).active(true).build(),
                User.builder().name("Jorge").surname("Pérez").phoneNumber("3045556677")
                        .email("jorge.perez@sigi.com").password(passwordEncoder.encode("123456789")).role(roleAdmin).active(true).build(),
                User.builder().name("Camila").surname("Rojas").phoneNumber("3056667788")
                        .email("camila.rojas@sigi.com").password(passwordEncoder.encode("123456789")).role(roleDispatcher).active(true).build()
        );
        userRepository.saveAll(users);

        List<Client> clients = List.of(
                Client.builder().name("Supermercado La 14").identification("900123456-7").location("Armenia, Quindío")
                        .phone("6067451234").email("contacto@la14.com").active(true).build(),
                Client.builder().name("Distribuidora El Cafetal").identification("901987654-3").location("Calarcá, Quindío")
                        .phone("6067455678").email("ventas@cafetal.com").active(true).build(),
                Client.builder().name("Tiendas Olímpica").identification("890765432-1").location("Bogotá, Cundinamarca")
                        .phone("6012345678").email("compras@olimpica.com").active(true).build(),
                Client.builder().name("Éxito Armenia").identification("890123456-9").location("Armenia, Quindío")
                        .phone("6061234567").email("proveedores@exito.com").active(true).build(),
                Client.builder().name("D1 Armenia").identification("901234567-0").location("Armenia, Quindío")
                        .phone("6067654321").email("contacto@d1.com").active(true).build(),
                Client.builder().name("Justo & Bueno").identification("902345678-1").location("Pereira, Risaralda")
                        .phone("6068765432").email("ventas@justoybueno.com").active(true).build(),
                Client.builder().name("Makro Bogotá").identification("903456789-2").location("Bogotá, Cundinamarca")
                        .phone("6019876543").email("compras@makro.com").active(true).build(),
                Client.builder().name("Ara Armenia").identification("904567890-3").location("Armenia, Quindío")
                        .phone("6062345678").email("contacto@ara.com").active(true).build(),
                Client.builder().name("Alkosto Cali").identification("905678901-4").location("Cali, Valle del Cauca")
                        .phone("6023456789").email("proveedores@alkosto.com").active(true).build(),
                Client.builder().name("Surtimax Medellín").identification("906789012-5").location("Medellín, Antioquia")
                        .phone("6044567890").email("ventas@surtimax.com").active(true).build()
        );
        clientRepository.saveAll(clients);

        List<Warehouse> warehouses = List.of(
                Warehouse.builder().name("Bodega Central").location("Zona Industrial Armenia").totalCapacity(10000).active(true).build(),
                Warehouse.builder().name("Bodega Norte").location("Sector La Castellana").totalCapacity(5000).active(true).build(),
                Warehouse.builder().name("Bodega Sur").location("Sector La Fachada").totalCapacity(7000).active(true).build(),
                Warehouse.builder().name("Bodega Occidente").location("Sector La Florida").totalCapacity(8000).active(true).build(),
                Warehouse.builder().name("Bodega Oriente").location("Sector El Bosque").totalCapacity(6000).active(true).build(),
                Warehouse.builder().name("Bodega Bogotá").location("Zona Industrial Montevideo").totalCapacity(15000).active(true).build(),
                Warehouse.builder().name("Bodega Cali").location("Zona Industrial Yumbo").totalCapacity(12000).active(true).build(),
                Warehouse.builder().name("Bodega Medellín").location("Zona Industrial Itagüí").totalCapacity(11000).active(true).build(),
                Warehouse.builder().name("Bodega Pereira").location("Zona Industrial Cuba").totalCapacity(9000).active(true).build(),
                Warehouse.builder().name("Bodega Bucaramanga").location("Zona Industrial Girón").totalCapacity(9500).active(true).build()
        );
        warehouseRepository.saveAll(warehouses);

        List<Product> products = List.of(
                Product.builder().sku("SKU-001").name("Arroz Diana 500g").category("Alimentos").unit("KG")
                        .price(BigDecimal.valueOf(2500)).barcode("7701234567890").imageUrl("https://example.com/arroz.jpg").active(true).build(),
                Product.builder().sku("SKU-002").name("Aceite Premier 1L").category("Aceites").unit("KG")
                        .price(BigDecimal.valueOf(8500)).barcode("7709876543210").imageUrl("https://example.com/aceite.jpg").active(true).build(),
                Product.builder().sku("SKU-003").name("Azúcar Incauca 1kg").category("Alimentos").unit("KG")
                        .price(BigDecimal.valueOf(3200)).barcode("7704567890123").imageUrl("https://example.com/azucar.jpg").active(true).build(),
                Product.builder().sku("SKU-004").name("Sal Refisal 1kg").category("Alimentos").unit("KG")
                        .price(BigDecimal.valueOf(1500)).barcode("7706543210987").imageUrl("https://example.com/sal.jpg").active(true).build(),
                Product.builder().sku("SKU-005").name("Leche Alquería 1L").category("Lácteos").unit("KG")
                        .price(BigDecimal.valueOf(3800)).barcode("7708765432109").imageUrl("https://example.com/leche.jpg").active(true).build(),
                Product.builder().sku("SKU-006").name("Pan Bimbo Familiar").category("Panadería").unit("KG")
                        .price(BigDecimal.valueOf(4500)).barcode("7702345678901").imageUrl("https://example.com/pan.jpg").active(true).build(),
                Product.builder().sku("SKU-007").name("Huevos AA x30").category("Proteína").unit("KG")
                        .price(BigDecimal.valueOf(14500)).barcode("7703456789012").imageUrl("https://example.com/huevos.jpg").active(true).build(),
                Product.builder().sku("SKU-008").name("Café Quindío 500g").category("Bebidas").unit("KG")
                        .price(BigDecimal.valueOf(12000)).barcode("7705678901234").imageUrl("https://example.com/cafe.jpg").active(true).build(),
                Product.builder().sku("SKU-009").name("Gaseosa Postobón 1.5L").category("Bebidas").unit("KG")
                        .price(BigDecimal.valueOf(3500)).barcode("7706789012345").imageUrl("https://example.com/gaseosa.jpg").active(true).build(),
                Product.builder().sku("SKU-010").name("Detergente Ariel 2kg").category("Aseo").unit("KG")
                        .price(BigDecimal.valueOf(18500)).barcode("7707890123456").imageUrl("https://example.com/detergente.jpg").active(true).build()
        );
        productRepository.saveAll(products);


        List<Inventory> inventories = List.of(
                Inventory.builder().product(products.get(0)).warehouse(warehouses.get(0)).location("Estante A1").lot("LOT-ARZ-2026")
                        .productionDate(LocalDate.of(2025, 12, 1)).expirationDate(LocalDate.of(2026, 12, 1))
                        .availableQuantity(BigDecimal.valueOf(500)).reservedQuantity(BigDecimal.valueOf(50)).active(true).build(),
                Inventory.builder().product(products.get(1)).warehouse(warehouses.get(1)).location("Estante B2").lot("LOT-ACE-2026")
                        .productionDate(LocalDate.of(2025, 11, 15)).expirationDate(LocalDate.of(2026, 11, 15))
                        .availableQuantity(BigDecimal.valueOf(300)).reservedQuantity(BigDecimal.valueOf(30)).active(true).build(),
                Inventory.builder().product(products.get(2)).warehouse(warehouses.get(2)).location("Estante C3").lot("LOT-AZU-2026")
                        .productionDate(LocalDate.of(2025, 10, 10)).expirationDate(LocalDate.of(2026, 10, 10))
                        .availableQuantity(BigDecimal.valueOf(400)).reservedQuantity(BigDecimal.valueOf(40)).active(true).build(),
                Inventory.builder().product(products.get(3)).warehouse(warehouses.get(3)).location("Estante D4").lot("LOT-SAL-2026")
                        .productionDate(LocalDate.of(2025, 9, 5)).expirationDate(LocalDate.of(2026, 9, 5))
                        .availableQuantity(BigDecimal.valueOf(600)).reservedQuantity(BigDecimal.valueOf(60)).active(true).build(),
                Inventory.builder().product(products.get(4)).warehouse(warehouses.get(4)).location("Estante E5").lot("LOT-LEC-2026")
                        .productionDate(LocalDate.of(2025, 8, 20)).expirationDate(LocalDate.of(2026, 8, 20))
                        .availableQuantity(BigDecimal.valueOf(200)).reservedQuantity(BigDecimal.valueOf(20)).active(true).build(),
                Inventory.builder().product(products.get(5)).warehouse(warehouses.get(5)).location("Estante F6").lot("LOT-PAN-2026")
                        .productionDate(LocalDate.of(2025, 7, 15)).expirationDate(LocalDate.of(2026, 7, 15))
                        .availableQuantity(BigDecimal.valueOf(150)).reservedQuantity(BigDecimal.valueOf(15)).active(true).build(),
                Inventory.builder().product(products.get(6)).warehouse(warehouses.get(6)).location("Estante G7").lot("LOT-HUE-2026")
                        .productionDate(LocalDate.of(2025, 6, 10)).expirationDate(LocalDate.of(2026, 6, 10))
                        .availableQuantity(BigDecimal.valueOf(100)).reservedQuantity(BigDecimal.valueOf(10)).active(true).build(),
                Inventory.builder().product(products.get(7)).warehouse(warehouses.get(7)).location("Estante H8").lot("LOT-CAF-2026")
                        .productionDate(LocalDate.of(2025, 5, 5)).expirationDate(LocalDate.of(2026, 5, 5))
                        .availableQuantity(BigDecimal.valueOf(250)).reservedQuantity(BigDecimal.valueOf(25)).active(true).build(),
                Inventory.builder().product(products.get(8)).warehouse(warehouses.get(8)).location("Estante I9").lot("LOT-GAS-2026")
                        .productionDate(LocalDate.of(2025, 4, 1)).expirationDate(LocalDate.of(2026, 4, 1))
                        .availableQuantity(BigDecimal.valueOf(350)).reservedQuantity(BigDecimal.valueOf(35)).active(true).build(),
                Inventory.builder().product(products.get(9)).warehouse(warehouses.get(9)).location("Estante J10").lot("LOT-DET-2026")
                        .productionDate(LocalDate.of(2025, 3, 1)).expirationDate(LocalDate.of(2026, 3, 1))
                        .availableQuantity(BigDecimal.valueOf(180)).reservedQuantity(BigDecimal.valueOf(18)).active(true).build()
        );
        inventoryRepository.saveAll(inventories);
        List<Dispatcher> dispatchers = List.of(
                Dispatcher.builder().name("Transportes Rápidos S.A.").contact("Juan Pérez").phone("3021234567").email("logistica@rapidos.com").active(true).identification("111111111").location("Armenia, Quindio").build(),
                Dispatcher.builder().name("Logística Express").contact("Carlos Gómez").phone("3019876543").email("contacto@express.com").active(true).identification("222222222").location("Pereira, Risaralda").build(),
                Dispatcher.builder().name("Envíos Nacionales Ltda").contact("María López").phone("3007654321").email("envios@ltda.com").active(true).identification("33333333333").location("Pitalito, Huila").build(),
                Dispatcher.builder().name("Distribuciones del Café").contact("Andrés Ramírez").phone("3041234567").email("cafe@distribuciones.com").active(true).identification("44444444").location("Calarca, Quindio").build(),
                Dispatcher.builder().name("Carga Segura S.A.").contact("Paola Torres").phone("3052345678").email("segura@carga.com").active(true).identification("5555555").location("Balboa, Risaralda").build(),
                Dispatcher.builder().name("Transportes del Quindío").contact("Felipe Castro").phone("3063456789").email("quindio@transportes.com").active(true).identification("6666666666").location("Medellin, Antioquia").build(),
                Dispatcher.builder().name("Logística Andina").contact("Camila Rojas").phone("3074567890").email("andina@logistica.com").active(true).identification("77777777").location("Sabeneta").build(),
                Dispatcher.builder().name("Envíos Rápidos Armenia").contact("Jorge Hernández").phone("3085678901").email("armenia@envios.com").active(true).identification("88888888").location("Union, Valle").build(),
                Dispatcher.builder().name("Distribuidora Nacional").contact("Diana Morales").phone("3096789012").email("nacional@distribuidora.com").active(true).identification("9999999999990").location("Cali, Valle").build(),
                Dispatcher.builder().name("Transportes del Valle").contact("Luis Martínez").phone("3107890123").email("valle@transportes.com").active(true).identification("12345667").location("Choco").build()
        );
        dispatcherRepository.saveAll(dispatchers);

        List<Order> orders = List.of(
                Order.builder().client(clients.get(0)).user(users.get(0)).warehouse(warehouses.get(0)).dispatcher(dispatchers.get(0))
                        .status(OrderStatus.DRAFT).total(BigDecimal.valueOf(0)).active(true).build(),
                Order.builder().client(clients.get(1)).user(users.get(1)).warehouse(warehouses.get(1)).dispatcher(dispatchers.get(0))
                        .status(OrderStatus.CONFIRMED).total(BigDecimal.valueOf(50000)).active(true).build(),
                Order.builder().client(clients.get(2)).user(users.get(2)).warehouse(warehouses.get(2)).dispatcher(dispatchers.get(0))
                        .status(OrderStatus.DELIVERED).total(BigDecimal.valueOf(75000)).active(true).build(),
                Order.builder().client(clients.get(3)).user(users.get(3)).warehouse(warehouses.get(3)).dispatcher(dispatchers.get(0))
                        .status(OrderStatus.DRAFT).total(BigDecimal.valueOf(0)).active(true).build(),
                Order.builder().client(clients.get(4)).user(users.get(4)).warehouse(warehouses.get(4)).dispatcher(dispatchers.get(0))
                        .status(OrderStatus.CONFIRMED).total(BigDecimal.valueOf(25000)).active(true).build(),
                Order.builder().client(clients.get(5)).user(users.get(5)).warehouse(warehouses.get(5)).dispatcher(dispatchers.get(0))
                        .status(OrderStatus.DELIVERED).total(BigDecimal.valueOf(60000)).active(true).build(),
                Order.builder().client(clients.get(6)).user(users.get(6)).warehouse(warehouses.get(6)).dispatcher(dispatchers.get(0))
                        .status(OrderStatus.DRAFT).total(BigDecimal.valueOf(0)).active(true).build(),
                Order.builder().client(clients.get(7)).user(users.get(7)).warehouse(warehouses.get(7)).dispatcher(dispatchers.get(0))
                        .status(OrderStatus.CONFIRMED).total(BigDecimal.valueOf(40000)).active(true).build(),
                Order.builder().client(clients.get(8)).user(users.get(8)).warehouse(warehouses.get(8)).dispatcher(dispatchers.get(0))
                        .status(OrderStatus.DELIVERED).total(BigDecimal.valueOf(90000)).active(true).build(),
                Order.builder().client(clients.get(9)).user(users.get(9)).warehouse(warehouses.get(9)).dispatcher(dispatchers.get(0))
                        .status(OrderStatus.CONFIRMED).total(BigDecimal.valueOf(30000)).active(true).build()
        );
        orderRepository.saveAll(orders);

        List<OrderLine> onlineOrders = List.of(
                OrderLine.builder().order(orders.get(0)).product(products.get(0)).inventory(inventories.get(0))
                        .lot("LOT-ARZ-2026").quantity(BigDecimal.valueOf(20)).unitPrice(BigDecimal.valueOf(2500)).active(true).build(),
                OrderLine.builder().order(orders.get(1)).product(products.get(1)).inventory(inventories.get(1))
                        .lot("LOT-ACE-2026").quantity(BigDecimal.valueOf(10)).unitPrice(BigDecimal.valueOf(8500)).active(true).build(),
                OrderLine.builder().order(orders.get(2)).product(products.get(2)).inventory(inventories.get(2))
                        .lot("LOT-AZU-2026").quantity(BigDecimal.valueOf(15)).unitPrice(BigDecimal.valueOf(3200)).active(true).build(),
                OrderLine.builder().order(orders.get(3)).product(products.get(3)).inventory(inventories.get(3))
                        .lot("LOT-SAL-2026").quantity(BigDecimal.valueOf(30)).unitPrice(BigDecimal.valueOf(1500)).active(true).build(),
                OrderLine.builder().order(orders.get(4)).product(products.get(4)).inventory(inventories.get(4))
                        .lot("LOT-LEC-2026").quantity(BigDecimal.valueOf(25)).unitPrice(BigDecimal.valueOf(3800)).active(true).build(),
                OrderLine.builder().order(orders.get(5)).product(products.get(5)).inventory(inventories.get(5))
                        .lot("LOT-PAN-2026").quantity(BigDecimal.valueOf(40)).unitPrice(BigDecimal.valueOf(4500)).active(true).build(),
                OrderLine.builder().order(orders.get(6)).product(products.get(6)).inventory(inventories.get(6))
                        .lot("LOT-HUE-2026").quantity(BigDecimal.valueOf(5)).unitPrice(BigDecimal.valueOf(14500)).active(true).build(),
                OrderLine.builder().order(orders.get(7)).product(products.get(7)).inventory(inventories.get(7))
                        .lot("LOT-CAF-2026").quantity(BigDecimal.valueOf(12)).unitPrice(BigDecimal.valueOf(12000)).active(true).build(),
                OrderLine.builder().order(orders.get(8)).product(products.get(8)).inventory(inventories.get(8))
                        .lot("LOT-GAS-2026").quantity(BigDecimal.valueOf(50)).unitPrice(BigDecimal.valueOf(3500)).active(true).build(),
                OrderLine.builder().order(orders.get(9)).product(products.get(9)).inventory(inventories.get(9))
                        .lot("LOT-DET-2026").quantity(BigDecimal.valueOf(8)).unitPrice(BigDecimal.valueOf(18500)).active(true).build()
        );
        onlineOrderRepository.saveAll(onlineOrders);
        List<Invoice> invoices = List.of(
                Invoice.builder().number("INV-2026-001").order(orders.get(0)).client(clients.get(0))
                        .subtotal(BigDecimal.valueOf(50000)).tax(BigDecimal.valueOf(9500)).total(BigDecimal.valueOf(59500))
                        .status(InvoiceStatus.ISSUED).active(true).build(),
                Invoice.builder().number("INV-2026-002").order(orders.get(1)).client(clients.get(1))
                        .subtotal(BigDecimal.valueOf(85000)).tax(BigDecimal.valueOf(16150)).total(BigDecimal.valueOf(101150))
                        .status(InvoiceStatus.PAID).active(true).build(),
                Invoice.builder().number("INV-2026-003").order(orders.get(2)).client(clients.get(2))
                        .subtotal(BigDecimal.valueOf(48000)).tax(BigDecimal.valueOf(9120)).total(BigDecimal.valueOf(57120))
                        .status(InvoiceStatus.CANCELED).active(true).build(),
                Invoice.builder().number("INV-2026-004").order(orders.get(3)).client(clients.get(3))
                        .subtotal(BigDecimal.valueOf(30000)).tax(BigDecimal.valueOf(5700)).total(BigDecimal.valueOf(35700))
                        .status(InvoiceStatus.ISSUED).active(true).build(),
                Invoice.builder().number("INV-2026-005").order(orders.get(4)).client(clients.get(4))
                        .subtotal(BigDecimal.valueOf(25000)).tax(BigDecimal.valueOf(4750)).total(BigDecimal.valueOf(29750))
                        .status(InvoiceStatus.PAID).active(true).build(),
                Invoice.builder().number("INV-2026-006").order(orders.get(5)).client(clients.get(5))
                        .subtotal(BigDecimal.valueOf(60000)).tax(BigDecimal.valueOf(11400)).total(BigDecimal.valueOf(71400))
                        .status(InvoiceStatus.PAID).active(true).build(),
                Invoice.builder().number("INV-2026-007").order(orders.get(6)).client(clients.get(6))
                        .subtotal(BigDecimal.valueOf(40000)).tax(BigDecimal.valueOf(7600)).total(BigDecimal.valueOf(47600))
                        .status(InvoiceStatus.CANCELED).active(true).build(),
                Invoice.builder().number("INV-2026-008").order(orders.get(7)).client(clients.get(7))
                        .subtotal(BigDecimal.valueOf(90000)).tax(BigDecimal.valueOf(17100)).total(BigDecimal.valueOf(107100))
                        .status(InvoiceStatus.PAID).active(true).build(),
                Invoice.builder().number("INV-2026-009").order(orders.get(8)).client(clients.get(8))
                        .subtotal(BigDecimal.valueOf(30000)).tax(BigDecimal.valueOf(5700)).total(BigDecimal.valueOf(35700))
                        .status(InvoiceStatus.CANCELED).active(true).build(),
                Invoice.builder().number("INV-2026-010").order(orders.get(9)).client(clients.get(9))
                        .subtotal(BigDecimal.valueOf(185000)).tax(BigDecimal.valueOf(35150)).total(BigDecimal.valueOf(220150))
                        .status(InvoiceStatus.PAID).active(true).build()
        );
        invoiceRepository.saveAll(invoices);



        List<Movement> movements = List.of(
                Movement.builder().type(MovementType.EXIT).inventory(inventories.get(0)).product(products.get(0))
                        .quantity(BigDecimal.valueOf(20)).user(users.get(0)).order(orders.get(0)).dispatcher(dispatchers.get(0))
                        .motive("Entrega de arroz a Supermercado La 14").active(true).build(),
                Movement.builder().type(MovementType.ENTRY).inventory(inventories.get(1)).product(products.get(1))
                        .quantity(BigDecimal.valueOf(50)).user(users.get(1)).order(orders.get(1)).dispatcher(dispatchers.get(1))
                        .motive("Ingreso de aceite Premier").active(true).build(),
                Movement.builder().type(MovementType.EXIT).inventory(inventories.get(2)).product(products.get(2))
                        .quantity(BigDecimal.valueOf(15)).user(users.get(2)).order(orders.get(2)).dispatcher(dispatchers.get(2))
                        .motive("Salida de azúcar para Olímpica").active(true).build(),
                Movement.builder().type(MovementType.ENTRY).inventory(inventories.get(3)).product(products.get(3))
                        .quantity(BigDecimal.valueOf(100)).user(users.get(3)).order(orders.get(3)).dispatcher(dispatchers.get(3))
                        .motive("Ingreso de sal Refisal").active(true).build(),
                Movement.builder().type(MovementType.EXIT).inventory(inventories.get(4)).product(products.get(4))
                        .quantity(BigDecimal.valueOf(25)).user(users.get(4)).order(orders.get(4)).dispatcher(dispatchers.get(4))
                        .motive("Entrega de leche Alquería a D1").active(true).build(),
                Movement.builder().type(MovementType.ENTRY).inventory(inventories.get(5)).product(products.get(5))
                        .quantity(BigDecimal.valueOf(40)).user(users.get(5)).order(orders.get(5)).dispatcher(dispatchers.get(5))
                        .motive("Ingreso de pan Bimbo").active(true).build(),
                Movement.builder().type(MovementType.EXIT).inventory(inventories.get(6)).product(products.get(6))
                        .quantity(BigDecimal.valueOf(5)).user(users.get(6)).order(orders.get(6)).dispatcher(dispatchers.get(6))
                        .motive("Salida de huevos para Justo & Bueno").active(true).build(),
                Movement.builder().type(MovementType.ENTRY).inventory(inventories.get(7)).product(products.get(7))
                        .quantity(BigDecimal.valueOf(12)).user(users.get(7)).order(orders.get(7)).dispatcher(dispatchers.get(7))
                        .motive("Ingreso de café Quindío").active(true).build(),
                Movement.builder().type(MovementType.EXIT).inventory(inventories.get(8)).product(products.get(8))
                        .quantity(BigDecimal.valueOf(50)).user(users.get(8)).order(orders.get(8)).dispatcher(dispatchers.get(8))
                        .motive("Salida de gaseosa Postobón para Makro").active(true).build(),
                Movement.builder().type(MovementType.ENTRY).inventory(inventories.get(9)).product(products.get(9))
                        .quantity(BigDecimal.valueOf(8)).user(users.get(9)).order(orders.get(9)).dispatcher(dispatchers.get(9))
                        .motive("Ingreso de detergente Ariel").active(true).build()
        );
        movementRepository.saveAll(movements);

        log.info("Data initialization completed.");
    }
}
