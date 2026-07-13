package com.vyaparsetu.order.service;

import com.vyaparsetu.catalog.entity.Product;
import com.vyaparsetu.catalog.repository.ProductRepository;
import com.vyaparsetu.common.exception.BusinessException;
import com.vyaparsetu.common.exception.ResourceNotFoundException;
import com.vyaparsetu.order.dto.CartItemRequest;
import com.vyaparsetu.order.dto.CartResponse;
import com.vyaparsetu.order.entity.Cart;
import com.vyaparsetu.order.entity.CartItem;
import com.vyaparsetu.order.repository.CartItemRepository;
import com.vyaparsetu.order.repository.CartRepository;
import com.vyaparsetu.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserService userService;

    public CartService(CartRepository cartRepository, CartItemRepository cartItemRepository,
                       ProductRepository productRepository, UserService userService) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userService = userService;
    }

    @Transactional
    public CartResponse addItem(CartItemRequest req) {
        Long retailerId = userService.currentRetailerId();
        Product product = productRepository.findById(req.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", req.productId()));
        // INTEGRITY: a product can only be added to the cart of the supplier that sells it,
        // and it must be active.
        if (!product.isActive()) {
            throw new BusinessException("PRODUCT_INACTIVE", HttpStatus.CONFLICT, "Product is not available");
        }
        if (!product.getSupplierId().equals(req.supplierId())) {
            throw new BusinessException("PRODUCT_SUPPLIER_MISMATCH", HttpStatus.BAD_REQUEST,
                    "This product is not sold by the selected distributor");
        }
        Cart cart = cartRepository.findByRetailerIdAndSupplierId(retailerId, req.supplierId())
                .orElseGet(() -> {
                    Cart c = new Cart();
                    c.setRetailerId(retailerId);
                    c.setSupplierId(req.supplierId());
                    return cartRepository.save(c);
                });

        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId())
                .orElseGet(() -> {
                    CartItem ci = new CartItem();
                    ci.setCartId(cart.getId());
                    ci.setProductId(product.getId());
                    ci.setQuantity(BigDecimal.ZERO);
                    return ci;
                });
        item.setQuantity(req.quantity());
        cartItemRepository.save(item);

        return getCart(req.supplierId());
    }

    @Transactional
    public void removeItem(Long cartItemId) {
        Long retailerId = userService.currentRetailerId();
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", cartItemId));
        // SECURITY: ensure the cart item belongs to a cart owned by the current retailer.
        Cart cart = cartRepository.findById(item.getCartId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart", item.getCartId()));
        if (!cart.getRetailerId().equals(retailerId)) {
            throw new AccessDeniedException("Not your cart item");
        }
        cartItemRepository.deleteById(cartItemId);
    }

    @Transactional(readOnly = true)
    public java.util.List<CartResponse> myCarts() {
        Long retailerId = userService.currentRetailerId();
        return cartRepository.findByRetailerId(retailerId).stream()
                .map(c -> getCart(c.getSupplierId()))
                .filter(c -> c.items() != null && !c.items().isEmpty())
                .toList();
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(Long supplierId) {
        Long retailerId = userService.currentRetailerId();
        Cart cart = cartRepository.findByRetailerIdAndSupplierId(retailerId, supplierId)
                .orElse(null);
        if (cart == null) {
            return new CartResponse(null, supplierId, List.of(), BigDecimal.ZERO);
        }
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        Map<Long, Product> products = productRepository.findAllById(
                        items.stream().map(CartItem::getProductId).toList())
                .stream().collect(Collectors.toMap(Product::getId, Function.identity()));

        BigDecimal total = BigDecimal.ZERO;
        java.util.List<CartResponse.Line> lines = new java.util.ArrayList<>();
        for (CartItem ci : items) {
            Product p = products.get(ci.getProductId());
            if (p == null) continue;
            BigDecimal lineTotal = p.getSellingPrice().multiply(ci.getQuantity());
            total = total.add(lineTotal);
            lines.add(new CartResponse.Line(ci.getId(), p.getId(), p.getName(),
                    ci.getQuantity(), p.getSellingPrice(), lineTotal));
        }
        return new CartResponse(cart.getId(), supplierId, lines, total);
    }
}
