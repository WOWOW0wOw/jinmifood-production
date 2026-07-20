package com.jinmifood.shop.repository;
import com.jinmifood.shop.domain.ProductInquiry;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ProductInquiryRepository extends JpaRepository<ProductInquiry,Long>{
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths="member")
    Page<ProductInquiry> findByProductIdOrderByCreatedAtDesc(Long productId,Pageable pageable);
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths={"member","product"})
    Page<ProductInquiry> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
