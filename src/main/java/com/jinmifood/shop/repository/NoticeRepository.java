package com.jinmifood.shop.repository;
import com.jinmifood.shop.domain.Notice;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface NoticeRepository extends JpaRepository<Notice,Long>{
    Page<Notice> findAllByOrderByPinnedDescCreatedAtDesc(Pageable pageable);
}
