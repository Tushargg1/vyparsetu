package com.vyaparsetu.user.service;

import com.vyaparsetu.common.exception.BusinessException;
import com.vyaparsetu.common.exception.ResourceNotFoundException;
import com.vyaparsetu.common.security.CurrentUser;
import com.vyaparsetu.user.dto.UserResponse;
import com.vyaparsetu.user.entity.Retailer;
import com.vyaparsetu.user.entity.User;
import com.vyaparsetu.user.repository.RetailerRepository;
import com.vyaparsetu.user.repository.SupplierRepository;
import com.vyaparsetu.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RetailerRepository retailerRepository;
    private final SupplierRepository supplierRepository;

    public UserService(UserRepository userRepository,
                       RetailerRepository retailerRepository,
                       SupplierRepository supplierRepository) {
        this.userRepository = userRepository;
        this.retailerRepository = retailerRepository;
        this.supplierRepository = supplierRepository;
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser() {
        User user = userRepository.findById(CurrentUser.id())
                .orElseThrow(() -> new ResourceNotFoundException("User", CurrentUser.id()));
        return UserResponse.from(user);
    }

    /**
     * Resolve the retailer profile id for the current user.
     */
    @Transactional(readOnly = true)
    public Long currentRetailerId() {
        return retailerRepository.findByUserId(CurrentUser.id())
                .orElseThrow(() -> new ResourceNotFoundException("Retailer profile for user", CurrentUser.id()))
                .getId();
    }

    @Transactional(readOnly = true)
    public Retailer currentRetailer() {
        return retailerRepository.findByUserId(CurrentUser.id())
                .orElseThrow(() -> new ResourceNotFoundException("Retailer profile for user", CurrentUser.id()));
    }

    /**
     * The distributor (supplier) the current retailer belongs to.
     * Throws if the retailer has not yet joined a distributor.
     */
    @Transactional(readOnly = true)
    public Long currentDistributorId() {
        Long distributorId = currentRetailer().getDistributorId();
        if (distributorId == null) {
            throw new BusinessException("NOT_LINKED", HttpStatus.CONFLICT,
                    "You are not linked to a distributor yet. Join one with an invite code.");
        }
        return distributorId;
    }

    @Transactional(readOnly = true)
    public Long currentSupplierId() {
        return supplierRepository.findByUserId(CurrentUser.id())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier profile for user", CurrentUser.id()))
                .getId();
    }
}
