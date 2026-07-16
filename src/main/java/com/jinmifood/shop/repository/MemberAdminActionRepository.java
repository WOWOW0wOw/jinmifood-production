package com.jinmifood.shop.repository;

import com.jinmifood.shop.domain.MemberAdminAction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MemberAdminActionRepository extends JpaRepository<MemberAdminAction,Long> {
    List<MemberAdminAction> findTop30ByMemberIdOrderByCreatedAtDesc(Long memberId);
}
