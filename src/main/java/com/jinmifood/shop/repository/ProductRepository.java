package com.jinmifood.shop.repository;
import com.jinmifood.shop.domain.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.*;
public interface ProductRepository extends JpaRepository<Product,Long>{
    long countByImageUrl(String imageUrl);
    List<Product> findTop8ByActiveTrueAndFeaturedTrueOrderByCreatedAtDesc();
    List<Product> findByActiveTrueOrderByCreatedAtDesc();
    List<Product> findByActiveTrueAndCategorySlugOrderByCreatedAtDesc(String slug);
    List<Product> findByActiveTrueAndNameContainingIgnoreCaseOrderByCreatedAtDesc(String query);
    Page<Product> findByActiveTrue(Pageable pageable);
    Page<Product> findByActiveTrueAndCategorySlug(String slug,Pageable pageable);
    Page<Product> findByActiveTrueAndNameContainingIgnoreCase(String query,Pageable pageable);
    Optional<Product> findBySlugAndActiveTrue(String slug);
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug,Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select p from Product p where p.id=:id") Optional<Product> findForUpdate(@Param("id") Long id);
}
