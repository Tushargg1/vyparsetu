package com.vyaparsetu.admin.service;

import com.vyaparsetu.catalog.repository.ProductRepository;
import com.vyaparsetu.common.enums.Enums;
import com.vyaparsetu.common.exception.ResourceNotFoundException;
import com.vyaparsetu.order.repository.OrderRepository;
import com.vyaparsetu.user.entity.User;
import com.vyaparsetu.user.repository.RetailerRepository;
import com.vyaparsetu.user.repository.SupplierRepository;
import com.vyaparsetu.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final RetailerRepository retailerRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public AdminService(UserRepository userRepository, RetailerRepository retailerRepository,
                        SupplierRepository supplierRepository, ProductRepository productRepository,
                        OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.retailerRepository = retailerRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Long> dashboard() {
        return Map.of(
                "users", userRepository.count(),
                "retailers", retailerRepository.count(),
                "suppliers", supplierRepository.count(),
                "products", productRepository.count(),
                "orders", orderRepository.count()
        );
    }

    @Transactional(readOnly = true)
    public Page<User> users(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Transactional
    public void setUserStatus(Long userId, Enums.UserStatus status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        user.setStatus(status);
        userRepository.save(user);
    }
}
