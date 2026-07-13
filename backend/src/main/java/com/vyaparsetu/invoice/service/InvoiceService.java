package com.vyaparsetu.invoice.service;

import com.vyaparsetu.common.exception.ResourceNotFoundException;
import com.vyaparsetu.common.security.CurrentUser;
import com.vyaparsetu.common.storage.StorageService;
import com.vyaparsetu.common.util.NumberGenerator;
import com.vyaparsetu.invoice.entity.Invoice;
import com.vyaparsetu.invoice.entity.InvoiceItem;
import com.vyaparsetu.invoice.repository.InvoiceItemRepository;
import com.vyaparsetu.invoice.repository.InvoiceRepository;
import com.vyaparsetu.order.entity.Order;
import com.vyaparsetu.order.entity.OrderItem;
import com.vyaparsetu.order.repository.OrderItemRepository;
import com.vyaparsetu.order.repository.OrderRepository;
import com.vyaparsetu.user.service.UserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final StorageService storageService;
    private final UserService userService;

    public InvoiceService(InvoiceRepository invoiceRepository, InvoiceItemRepository invoiceItemRepository,
                          OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                          StorageService storageService, UserService userService) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.storageService = storageService;
        this.userService = userService;
    }

    @Transactional
    public Invoice generateForOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        // SECURITY: only the owning supplier (or admin) may generate an invoice.
        ensureSupplierOwnsOrAdmin(order);
        Invoice existing = invoiceRepository.findByOrderId(orderId).orElse(null);
        if (existing != null) {
            return existing;
        }
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);

        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(NumberGenerator.invoiceNumber());
        invoice.setOrderId(orderId);
        invoice.setSupplierId(order.getSupplierId());
        invoice.setRetailerId(order.getRetailerId());
        invoice.setSubtotal(order.getSubtotal());
        invoice.setTaxAmount(order.getTaxAmount());
        invoice.setTotalAmount(order.getTotalAmount());
        invoice = invoiceRepository.save(invoice);

        for (OrderItem oi : orderItems) {
            InvoiceItem ii = new InvoiceItem();
            ii.setInvoiceId(invoice.getId());
            ii.setProductName(oi.getProductName());
            ii.setQuantity(oi.getQuantity());
            ii.setUnitPrice(oi.getUnitPrice());
            ii.setGstRate(oi.getGstRate());
            ii.setLineTotal(oi.getLineTotal());
            invoiceItemRepository.save(ii);
        }

        String html = render(order, invoice, orderItems);
        storageService.store(storageKey(invoice.getInvoiceNumber()), html, "text/html");
        // Secured, ownership-checked endpoint instead of a public static file path.
        invoice.setPdfUrl("/api/v1/invoices/order/" + orderId + "/document");
        return invoiceRepository.save(invoice);
    }

    @Transactional(readOnly = true)
    public Invoice getByOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        // SECURITY: only the order's supplier, its retailer, or an admin may read the invoice.
        ensureParticipant(order);
        return invoiceRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice for order", orderId));
    }

    /**
     * Returns the rendered invoice HTML, but only to a participant of the order.
     */
    @Transactional(readOnly = true)
    public String getDocument(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        ensureParticipant(order);
        Invoice invoice = invoiceRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice for order", orderId));
        String content = storageService.read(storageKey(invoice.getInvoiceNumber()));
        if (content == null) {
            throw new ResourceNotFoundException("Invoice document for order", orderId);
        }
        return content;
    }

    private String storageKey(String invoiceNumber) {
        return "invoices/" + invoiceNumber + ".html";
    }

    private void ensureSupplierOwnsOrAdmin(Order order) {
        if (CurrentUser.get().roles().contains("ADMIN")) return;
        if (!order.getSupplierId().equals(userService.currentSupplierId())) {
            throw new AccessDeniedException("Not your order");
        }
    }

    private void ensureParticipant(Order order) {
        var roles = CurrentUser.get().roles();
        if (roles.contains("ADMIN")) return;
        if (roles.contains("SUPPLIER") && order.getSupplierId().equals(userService.currentSupplierId())) return;
        if (roles.contains("RETAILER") && order.getRetailerId().equals(userService.currentRetailerId())) return;
        throw new AccessDeniedException("Not allowed to view this invoice");
    }

    private String render(Order order, Invoice invoice, List<OrderItem> items) {
        StringBuilder rows = new StringBuilder();
        for (OrderItem oi : items) {
            rows.append("<tr>")
                    .append("<td>").append(escape(oi.getProductName())).append("</td>")
                    .append("<td>").append(oi.getQuantity()).append("</td>")
                    .append("<td>").append(oi.getUnitPrice()).append("</td>")
                    .append("<td>").append(oi.getGstRate()).append("%</td>")
                    .append("<td>").append(oi.getLineTotal()).append("</td>")
                    .append("</tr>");
        }
        return """
                <html><head><meta charset="utf-8"><title>Invoice %s</title>
                <style>body{font-family:sans-serif} table{border-collapse:collapse;width:100%%}
                td,th{border:1px solid #ccc;padding:6px;text-align:left}</style></head>
                <body>
                <h2>Tax Invoice</h2>
                <p><b>Invoice No:</b> %s<br/><b>Order No:</b> %s</p>
                <table><thead><tr><th>Item</th><th>Qty</th><th>Rate</th><th>GST</th><th>Amount</th></tr></thead>
                <tbody>%s</tbody></table>
                <p style="text-align:right">
                Subtotal: %s<br/>Tax: %s<br/><b>Total: %s</b></p>
                </body></html>
                """.formatted(invoice.getInvoiceNumber(), invoice.getInvoiceNumber(),
                order.getOrderNumber(), rows, invoice.getSubtotal(),
                invoice.getTaxAmount(), invoice.getTotalAmount());
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
