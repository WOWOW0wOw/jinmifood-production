package com.jinmifood.shop.service;

import com.jinmifood.shop.domain.*;
import com.jinmifood.shop.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.NoSuchElementException;

@Service
@Transactional(readOnly=true)
public class CommunityService {
    private final ProductRepository products; private final MemberRepository members; private final CustomerOrderRepository orders;
    private final ProductReviewRepository reviews; private final ProductInquiryRepository inquiries; private final NoticeRepository notices;
    public CommunityService(ProductRepository products,MemberRepository members,CustomerOrderRepository orders,ProductReviewRepository reviews,ProductInquiryRepository inquiries,NoticeRepository notices){
        this.products=products;this.members=members;this.orders=orders;this.reviews=reviews;this.inquiries=inquiries;this.notices=notices;
    }
    public Page<ProductReview> reviews(Long productId,Pageable pageable){return reviews.findByProductIdOrderByCreatedAtDesc(productId,pageable);}
    public Page<ProductInquiry> inquiries(Long productId,Pageable pageable){return inquiries.findByProductIdOrderByCreatedAtDesc(productId,pageable);}
    public Page<ProductInquiry> allInquiries(Pageable pageable){return inquiries.findAllByOrderByCreatedAtDesc(pageable);}
    public Page<Notice> notices(Pageable pageable){return notices.findAllByOrderByPinnedDescCreatedAtDesc(pageable);}
    public Notice notice(Long id){return notices.findById(id).orElseThrow(()->new NoSuchElementException("공지사항을 찾을 수 없습니다."));}
    public long reviewCount(Long productId){return reviews.countByProductId(productId);}
    public double averageRating(Long productId){return reviews.averageRating(productId);}
    public boolean canReview(String email,Long productId){
        if(email==null)return false;
        var member=members.findByEmailIgnoreCase(email).orElse(null);
        return member!=null&&member.isActive()&&!reviews.existsByProductIdAndMemberId(productId,member.getId())
            &&orders.countPurchasedProduct(member.getId(),productId,PaymentStatus.PAID)>0;
    }
    @Transactional public void addReview(String email,Long productId,int rating,String content){
        if(rating<1||rating>5)throw new IllegalArgumentException("별점은 1점부터 5점까지 선택해 주세요.");
        String clean=required(content,2000,"리뷰 내용을 입력해 주세요.","리뷰는 2,000자 이하로 입력해 주세요.");
        var member=activeMember(email);var product=activeProduct(productId);
        if(reviews.existsByProductIdAndMemberId(productId,member.getId()))throw new IllegalStateException("이 상품에는 이미 리뷰를 작성했습니다.");
        if(orders.countPurchasedProduct(member.getId(),productId,PaymentStatus.PAID)==0)throw new IllegalStateException("결제가 완료된 구매 회원만 리뷰를 작성할 수 있습니다.");
        try{reviews.saveAndFlush(new ProductReview(product,member,rating,clean));}
        catch(DataIntegrityViolationException e){throw new IllegalStateException("이 상품에는 이미 리뷰를 작성했습니다.");}
    }
    @Transactional public void deleteReview(String email,boolean admin,Long reviewId){
        var review=reviews.findById(reviewId).orElseThrow(()->new NoSuchElementException("리뷰를 찾을 수 없습니다."));
        if(!admin&&!review.getMember().getEmail().equalsIgnoreCase(email))throw new IllegalStateException("본인이 작성한 리뷰만 삭제할 수 있습니다.");
        reviews.delete(review);
    }
    @Transactional public void addInquiry(String email,Long productId,String title,String content){
        var member=activeMember(email);var product=activeProduct(productId);
        inquiries.save(new ProductInquiry(product,member,
            required(title,120,"문의 제목을 입력해 주세요.","문의 제목은 120자 이하로 입력해 주세요."),
            required(content,2000,"문의 내용을 입력해 주세요.","문의는 2,000자 이하로 입력해 주세요.")));
    }
    @Transactional public void deleteInquiry(String email,boolean admin,Long inquiryId){
        var inquiry=inquiries.findById(inquiryId).orElseThrow(()->new NoSuchElementException("상품 문의를 찾을 수 없습니다."));
        if(!admin&&!inquiry.getMember().getEmail().equalsIgnoreCase(email))throw new IllegalStateException("본인이 작성한 문의만 삭제할 수 있습니다.");
        inquiries.delete(inquiry);
    }
    @Transactional public void answerInquiry(Long inquiryId,String answer,String adminEmail){
        var inquiry=inquiries.findById(inquiryId).orElseThrow(()->new NoSuchElementException("상품 문의를 찾을 수 없습니다."));
        inquiry.answer(required(answer,3000,"답변 내용을 입력해 주세요.","답변은 3,000자 이하로 입력해 주세요."),adminEmail);
    }
    @Transactional public Notice saveNotice(Long id,String title,String content,boolean pinned){
        String cleanTitle=required(title,160,"공지 제목을 입력해 주세요.","공지 제목은 160자 이하로 입력해 주세요.");
        String cleanContent=required(content,10000,"공지 내용을 입력해 주세요.","공지 내용은 10,000자 이하로 입력해 주세요.");
        if(id==null)return notices.save(new Notice(cleanTitle,cleanContent,pinned));
        var notice=notice(id);notice.update(cleanTitle,cleanContent,pinned);return notice;
    }
    @Transactional public void deleteNotice(Long id){notices.delete(notice(id));}
    private Member activeMember(String email){
        var member=members.findByEmailIgnoreCase(email==null?"":email).orElseThrow(()->new IllegalStateException("회원 정보를 찾을 수 없습니다."));
        if(!member.isActive())throw new IllegalStateException("활성 회원만 이용할 수 있습니다.");return member;
    }
    private Product activeProduct(Long id){var p=products.findById(id).orElseThrow(()->new NoSuchElementException("상품을 찾을 수 없습니다."));if(!p.isActive())throw new IllegalStateException("판매 중인 상품이 아닙니다.");return p;}
    private String required(String value,int max,String blankMessage,String lengthMessage){String clean=value==null?"":value.trim();if(clean.isBlank())throw new IllegalArgumentException(blankMessage);if(clean.length()>max)throw new IllegalArgumentException(lengthMessage);return clean;}
}
