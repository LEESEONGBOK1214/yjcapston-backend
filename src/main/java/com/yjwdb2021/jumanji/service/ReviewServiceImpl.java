package com.yjwdb2021.jumanji.service;

import com.yjwdb2021.jumanji.data.*;
import com.yjwdb2021.jumanji.repository.OrderRepository;
import com.yjwdb2021.jumanji.repository.ReviewRepository;
import com.yjwdb2021.jumanji.service.exception.orderException.OrderNotPaidException;
import com.yjwdb2021.jumanji.service.exception.reviewException.ReviewHasExistException;
import com.yjwdb2021.jumanji.service.exception.reviewException.ReviewIsNotYoursException;
import com.yjwdb2021.jumanji.service.exception.reviewException.ReviewNotFoundException;
import com.yjwdb2021.jumanji.service.interfaces.BasicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;


import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class ReviewServiceImpl implements BasicService<Review, Review.Request, String> {
    @Autowired
    ReviewRepository reviewRepository;
    @Autowired
    StorageServiceImpl storageService;
    @Autowired
    UserServiceImpl userService;
    @Autowired
    ShopServiceImpl shopService;
    @Autowired
    OrderServiceImpl orderService;
    @Autowired
    OrderRepository orderRepository;


    @Override
    public Review get(@Nullable String authorization, String... reviewId) {
        return isPresent(reviewId[0]);
    }

    public List<Review> getMyReviewList(String authorization){
        String loginId = userService.getMyId(authorization);
        List<Review> reviewList = reviewRepository.findAllByUser_IdOrderByIdDesc(loginId);
        return reviewList;
    }

    @Override
    public List<Review> getList(@Nullable String authorization,String... shopId) {
        return reviewRepository.findAllByShopIdOrderByRegTimeDesc(shopId[0]);
    }

    @Override
    public Review post(@Nullable String authorization, Review.Request request) {
        String loginId = userService.getMyId(authorization);
        String uri = "shop/" + request.getShopId() + "/review/"; // TODO ?????? storage service?????? ????????? ?????? ???????????? ???????????? ??????.
        String imgPath = "";
        Timestamp orderId = new Timestamp(Long.parseLong(request.getOrderId()));


        // ????????? ??????
        User user = userService.isPresent(loginId); // ?????? ????????? ??????.
        Shop shop = shopService.isPresent(request.getShopId()); //
        Order order = orderService.isPresent(orderId);
        if(!order.getStatus().equals("pd"))throw new OrderNotPaidException(); //????????? ????????? ?????????????????? ?????? ???.
        isEmpty(request.getOrderId()); // ?????? ?????? ????????? ?????? ????????? ????????? ??????
        //TODO ????????? ???????????? ??? ?????? ?????? ?????? ?????? ?????? ???.
//        System.out.println(request.getImg().getName());
        if (request.getImg() != null && request.getImg().getSize() > 0)
            imgPath = storageService.store(request.getImg(), request.getImg().getResource().getFilename().replace(" ", "_"), uri.split("/"));
        Date regDate = new Date();
        Timestamp regTime = new Timestamp(regDate.getTime());
        Review review;
        String reviewId = request.getShopId().substring(0,2) + DateOperator.dateToYYYYMMDD(regDate, false);
        int countDayReviews = reviewRepository.countByIdStartingWith(reviewId);
        reviewId = StringUtils.append(reviewId, String.format("%03d", countDayReviews));
//        order.setReviewedY();
        orderRepository.save(order);
        review = Review.init()
                .id(reviewId)
                .content(request.getContent())
                .regTime(regTime)
                .parentId(request.getParentId())
                .score(request.getScore())
                .imgUrl(imgPath)
                .user(user)
                .shop(shop)
                .order(order)
                .build();
        review = reviewRepository.save(review);
        return review;
    }

    @Override
    public Review patch(String authorization, Review.Request request) {
        return null;
    }

    @Override
    public void delete(@Nullable String authorization, String... reviewId) {
        String loginId = userService.getMyId(authorization);
        // ????????? ??????
        userService.isPresent(loginId); // ?????? ????????? ???????????????.
        Review review = isOwnReview(loginId, reviewId[0]); // ????????? ????????????. ????????? ?????? ???????????? ??????.
        // ??? ?????? ????????? ????????? ?????? ?????? ??????????????? ????????? ???????????????.

        reviewRepository.delete(review);
    }

    @Override
    public Review isPresent(String reviewId) {
        Optional<Review> review = reviewRepository.findById(reviewId);
        if(review.isPresent())return review.get();
        throw new ReviewNotFoundException();
    }

    @Override
    public boolean isEmpty(String orderId) {
        Optional<Review> review = reviewRepository.findByOrderId(new Timestamp(Long.parseLong(orderId)));
        if(review.isEmpty())return true;
        throw new ReviewHasExistException();
    }

    public Review isOwnReview(String loginId, String reviewId){
        Review r = (Review) isPresent(reviewId);
        String reviewer = r.getUser().getId();
        if(reviewer.equals(loginId))return r;
        throw new ReviewIsNotYoursException();
    }
}
