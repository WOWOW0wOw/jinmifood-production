package com.jinmifood.shop.repository;

import com.jinmifood.shop.domain.SocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;
import java.util.Collection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SocialAccountRepository extends JpaRepository<SocialAccount,Long> {
    @Query("select a from SocialAccount a join fetch a.member where a.provider=:provider and a.providerUserId=:providerUserId")
    Optional<SocialAccount> findByProviderAndProviderUserId(@Param("provider") String provider,@Param("providerUserId") String providerUserId);
    List<SocialAccount> findByMemberIdOrderByCreatedAtAsc(Long memberId);
    boolean existsByMemberId(Long memberId);
    void deleteByMemberId(Long memberId);
    @Query("select a from SocialAccount a join fetch a.member where a.member.id in :memberIds order by a.createdAt asc")
    List<SocialAccount> findByMemberIds(@Param("memberIds") Collection<Long> memberIds);
}
