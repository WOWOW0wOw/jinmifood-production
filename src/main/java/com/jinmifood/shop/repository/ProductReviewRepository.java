package com.jinmifood.shop.repository;
import com.jinmifood.shop.domain.ProductReview;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface ProductReviewRepository extends JpaRepository<ProductReview,Long>{
    @EntityGraph(attributePaths="member")
    Page<ProductReview> findByProductIdOrderByCreatedAtDesc(Long productId,Pageable pageable);
    boolean existsByProductIdAndMemberId(Long productId,Long memberId);
    long countByProductId(Long productId);
    @Query("select coalesce(avg(r.rating),0) from ProductReview r where r.product.id=:productId")
    double averageRating(@Param("productId")Long productId);
}
