package com.jinmifood.shop.repository;

import com.jinmifood.shop.domain.Member;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MemberRepository extends JpaRepository<Member,Long> {
    Optional<Member> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    Optional<Member> findByEmailIgnoreCaseAndPhone(String email,String phone);
    List<Member> findByNameAndPhoneOrderByCreatedAtAsc(String name,String phone);
    @Query("select m from Member m where :query='' or lower(m.email) like lower(concat('%',:query,'%')) or lower(m.name) like lower(concat('%',:query,'%')) or m.phone like concat('%',:query,'%')")
    Page<Member> search(@Param("query") String query,Pageable pageable);
    long countByAdminTrueAndActiveTrue();
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Member m where lower(m.email)=lower(:email)")
    Optional<Member> findByEmailForUpdate(@Param("email") String email);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Member m where m.id=:id")
    Optional<Member> findByIdForUpdate(@Param("id") Long id);
}
