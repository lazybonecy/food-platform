package com.food.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.food.common.core.exception.BusinessException;
import com.food.common.redis.util.RedisUtil;
import com.food.order.dto.*;
import com.food.order.entity.Coupon;
import com.food.order.entity.CouponOrder;
import com.food.order.entity.CouponType;
import com.food.order.entity.UserCoupon;
import com.food.order.mapper.CouponMapper;
import com.food.order.mapper.CouponOrderMapper;
import com.food.order.mapper.UserCouponMapper;
import com.food.order.mq.CouponClaimMessage;
import com.food.order.mq.RabbitMQConfig;
import com.food.order.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;
    private final CouponOrderMapper couponOrderMapper;
    private final RedisUtil redisUtil;
    private final RabbitTemplate rabbitTemplate;

    private static final String STOCK_KEY_PREFIX = "coupon:stock:";
    private static final String LIMIT_KEY_PREFIX = "coupon:limit:";

    @Override
    public Long createCoupon(Long merchantId, CouponDTO dto) {
        Coupon coupon = new Coupon();
        coupon.setMerchantId(merchantId);
        coupon.setArticleId(dto.getArticleId());
        coupon.setTitle(dto.getTitle());
        coupon.setDescription(dto.getDescription());
        coupon.setType(dto.getType());
        coupon.setThreshold(dto.getThreshold() != null ? dto.getThreshold() : BigDecimal.ZERO);
        coupon.setDiscount(dto.getDiscount());
        coupon.setOriginalPrice(dto.getOriginalPrice() != null ? dto.getOriginalPrice() : BigDecimal.ZERO);
        coupon.setTotalCount(dto.getTotalCount());
        coupon.setClaimedCount(0);
        coupon.setLimitPerUser(dto.getLimitPerUser() != null ? dto.getLimitPerUser() : 1);
        coupon.setVersion(0);
        coupon.setStartTime(LocalDateTime.parse(dto.getStartTime()));
        coupon.setEndTime(LocalDateTime.parse(dto.getEndTime()));
        coupon.setStatus(1);
        couponMapper.insert(coupon);
        // Initialize Redis stock
        initCouponStock(coupon.getId(), coupon.getTotalCount());
        return coupon.getId();
    }

    @Override
    public void updateCoupon(Long merchantId, Long couponId, CouponDTO dto) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null || !coupon.getMerchantId().equals(merchantId)) {
            throw new BusinessException("优惠券不存在");
        }
        if (dto.getTitle() != null) coupon.setTitle(dto.getTitle());
        if (dto.getDescription() != null) coupon.setDescription(dto.getDescription());
        if (dto.getThreshold() != null) coupon.setThreshold(dto.getThreshold());
        if (dto.getDiscount() != null) coupon.setDiscount(dto.getDiscount());
        if (dto.getLimitPerUser() != null) coupon.setLimitPerUser(dto.getLimitPerUser());
        if (dto.getTotalCount() != null) {
            coupon.setTotalCount(dto.getTotalCount());
            initCouponStock(couponId, coupon.getTotalCount() - coupon.getClaimedCount());
        }
        if (dto.getStartTime() != null) coupon.setStartTime(LocalDateTime.parse(dto.getStartTime()));
        if (dto.getEndTime() != null) coupon.setEndTime(LocalDateTime.parse(dto.getEndTime()));
        couponMapper.updateById(coupon);
    }

    @Override
    public CouponListDTO listCoupons(Long merchantId, int current, int size) {
        LambdaQueryWrapper<Coupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Coupon::getMerchantId, merchantId);
        wrapper.orderByDesc(Coupon::getCreateTime);

        Page<Coupon> page = couponMapper.selectPage(new Page<>(current, size), wrapper);

        CouponListDTO result = new CouponListDTO();
        result.setRecords(page.getRecords().stream().map(this::toDTO).collect(Collectors.toList()));
        result.setTotal(page.getTotal());
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        return result;
    }

    @Override
    public CouponDTO getCouponDetail(Long couponId) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) {
            throw new BusinessException("优惠券不存在");
        }
        return toDTO(coupon);
    }

    @Override
    public void updateCouponStatus(Long merchantId, Long couponId, Integer status) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null || !coupon.getMerchantId().equals(merchantId)) {
            throw new BusinessException("优惠券不存在");
        }
        coupon.setStatus(status);
        couponMapper.updateById(coupon);
    }

    @Override
    @Transactional
    public void claimCoupon(Long userId, Long couponId) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null || coupon.getStatus() != 1) {
            throw new BusinessException("优惠券不存在或已下架");
        }
        if (LocalDateTime.now().isAfter(coupon.getEndTime())) {
            throw new BusinessException("优惠券已过期");
        }
        if (LocalDateTime.now().isBefore(coupon.getStartTime())) {
            throw new BusinessException("优惠券尚未开始");
        }

        // Check if already claimed
        LambdaQueryWrapper<UserCoupon> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getCouponId, couponId);
        Long existing = userCouponMapper.selectCount(checkWrapper);
        if (existing > 0) {
            throw new BusinessException("您已领取过该优惠券");
        }

        // Atomic stock check
        int affected = couponMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Coupon>()
                .eq(Coupon::getId, couponId)
                .lt(Coupon::getClaimedCount, coupon.getTotalCount())
                .setSql("claimed_count = claimed_count + 1"));
        if (affected == 0) {
            throw new BusinessException("优惠券已被抢光");
        }

        // Generate user coupon
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(couponId);
        userCoupon.setCouponCode(UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        userCoupon.setStatus(0);

        if (coupon.getOriginalPrice().compareTo(BigDecimal.ZERO) > 0) {
            // Paid coupon - create order
            userCoupon.setPayAmount(coupon.getOriginalPrice());

            CouponOrder order = new CouponOrder();
            order.setUserId(userId);
            order.setCouponId(couponId);
            order.setAmount(coupon.getOriginalPrice());
            order.setStatus(1); // Mock payment: directly paid
            order.setPayTime(LocalDateTime.now());
            couponOrderMapper.insert(order);

            userCouponMapper.insert(userCoupon);

            order.setUserCouponId(userCoupon.getId());
            couponOrderMapper.updateById(order);
        } else {
            // Free coupon
            userCoupon.setPayAmount(BigDecimal.ZERO);
            userCouponMapper.insert(userCoupon);
        }
    }

    @Override
    public UserCouponListDTO listMyCoupons(Long userId, int current, int size) {
        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCoupon::getUserId, userId);
        wrapper.orderByDesc(UserCoupon::getCreateTime);

        Page<UserCoupon> page = userCouponMapper.selectPage(new Page<>(current, size), wrapper);

        List<UserCouponDTO> records = page.getRecords().stream()
                .map(this::toUserCouponDTO)
                .collect(Collectors.toList());

        UserCouponListDTO result = new UserCouponListDTO();
        result.setRecords(records);
        result.setTotal(page.getTotal());
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        return result;
    }

    @Override
    public UserCouponDTO getMyCouponDetail(Long userId, Long userCouponId) {
        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        if (userCoupon == null || !userCoupon.getUserId().equals(userId)) {
            throw new BusinessException("优惠券不存在");
        }
        return toUserCouponDTO(userCoupon);
    }

    @Override
    public CouponVerifyDTO verifyCoupon(Long merchantId, String couponCode) {
        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCoupon::getCouponCode, couponCode);
        UserCoupon userCoupon = userCouponMapper.selectOne(wrapper);
        if (userCoupon == null) {
            throw new BusinessException("券码无效");
        }

        Coupon coupon = couponMapper.selectById(userCoupon.getCouponId());
        if (coupon == null || !coupon.getMerchantId().equals(merchantId)) {
            throw new BusinessException("该券不属于您的店铺");
        }

        CouponVerifyDTO dto = new CouponVerifyDTO();
        dto.setId(userCoupon.getId());
        dto.setCouponCode(userCoupon.getCouponCode());
        dto.setCouponTitle(coupon.getTitle());
        dto.setType(coupon.getType());
        dto.setTypeDesc(CouponType.fromCode(coupon.getType()).getDesc());
        dto.setThreshold(coupon.getThreshold());
        dto.setDiscount(coupon.getDiscount());
        dto.setStatus(userCoupon.getStatus());
        dto.setStatusDesc(userCoupon.getStatus() == 0 ? "未使用" : userCoupon.getStatus() == 1 ? "已使用" : "已过期");
        dto.setUsedTime(userCoupon.getUsedTime() != null ? userCoupon.getUsedTime().toString() : null);
        return dto;
    }

    @Override
    @Transactional
    public void confirmVerify(Long merchantId, String couponCode) {
        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCoupon::getCouponCode, couponCode);
        UserCoupon userCoupon = userCouponMapper.selectOne(wrapper);
        if (userCoupon == null) {
            throw new BusinessException("券码无效");
        }

        Coupon coupon = couponMapper.selectById(userCoupon.getCouponId());
        if (coupon == null || !coupon.getMerchantId().equals(merchantId)) {
            throw new BusinessException("该券不属于您的店铺");
        }
        if (userCoupon.getStatus() != 0) {
            throw new BusinessException("该券" + (userCoupon.getStatus() == 1 ? "已使用" : "已过期"));
        }

        userCoupon.setStatus(1);
        userCoupon.setUsedTime(LocalDateTime.now());
        userCouponMapper.updateById(userCoupon);
    }

    private CouponDTO toDTO(Coupon coupon) {
        CouponDTO dto = new CouponDTO();
        dto.setId(coupon.getId());
        dto.setMerchantId(coupon.getMerchantId());
        dto.setArticleId(coupon.getArticleId());
        dto.setTitle(coupon.getTitle());
        dto.setDescription(coupon.getDescription());
        dto.setType(coupon.getType());
        dto.setTypeDesc(CouponType.fromCode(coupon.getType()).getDesc());
        dto.setThreshold(coupon.getThreshold());
        dto.setDiscount(coupon.getDiscount());
        dto.setOriginalPrice(coupon.getOriginalPrice());
        dto.setTotalCount(coupon.getTotalCount());
        dto.setClaimedCount(coupon.getClaimedCount());
        dto.setRemainCount(coupon.getTotalCount() - coupon.getClaimedCount());
        dto.setLimitPerUser(coupon.getLimitPerUser());
        dto.setStartTime(coupon.getStartTime().toString());
        dto.setEndTime(coupon.getEndTime().toString());
        dto.setStatus(coupon.getStatus());
        if (coupon.getCreateTime() != null) {
            dto.setCreateTime(coupon.getCreateTime().toString());
        }
        return dto;
    }

    private UserCouponDTO toUserCouponDTO(UserCoupon userCoupon) {
        Coupon coupon = couponMapper.selectById(userCoupon.getCouponId());

        UserCouponDTO dto = new UserCouponDTO();
        dto.setId(userCoupon.getId());
        dto.setUserId(userCoupon.getUserId());
        dto.setCouponId(userCoupon.getCouponId());
        dto.setCouponCode(userCoupon.getCouponCode());
        dto.setStatus(userCoupon.getStatus());
        dto.setStatusDesc(userCoupon.getStatus() == 0 ? "未使用" : userCoupon.getStatus() == 1 ? "已使用" : "已过期");
        dto.setPayAmount(userCoupon.getPayAmount());
        dto.setUsedTime(userCoupon.getUsedTime() != null ? userCoupon.getUsedTime().toString() : null);
        if (userCoupon.getCreateTime() != null) {
            dto.setCreateTime(userCoupon.getCreateTime().toString());
        }

        if (coupon != null) {
            dto.setCouponTitle(coupon.getTitle());
            dto.setType(coupon.getType());
            dto.setTypeDesc(CouponType.fromCode(coupon.getType()).getDesc());
            dto.setThreshold(coupon.getThreshold());
            dto.setDiscount(coupon.getDiscount());
            dto.setOriginalPrice(coupon.getOriginalPrice());
            dto.setEndTime(coupon.getEndTime().toString());
        }

        return dto;
    }

    // ---- 秒杀相关 ----

    @Override
    public void flashClaimCoupon(Long userId, Long couponId) {
        // 1. Load coupon metadata from DB
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null || coupon.getStatus() != 1) {
            throw new BusinessException("优惠券不存在或已下架");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getStartTime())) {
            throw new BusinessException("优惠券尚未开始");
        }
        if (now.isAfter(coupon.getEndTime())) {
            throw new BusinessException("优惠券已过期");
        }

        // 2. Check purchase limit in Redis
        String limitKey = LIMIT_KEY_PREFIX + couponId + ":" + userId;
        String limitVal = redisUtil.getString(limitKey);
        int currentClaimed = limitVal != null ? Integer.parseInt(limitVal) : 0;
        if (currentClaimed >= coupon.getLimitPerUser()) {
            throw new BusinessException("已达领取上限（每人限" + coupon.getLimitPerUser() + "张）");
        }

        // 3. Atomic stock decrement via Redis DECR
        String stockKey = STOCK_KEY_PREFIX + couponId;
        Long remaining = redisUtil.decrement(stockKey);
        if (remaining == null || remaining < 0) {
            if (remaining != null && remaining < 0) {
                redisUtil.increment(stockKey);
            }
            throw new BusinessException("优惠券已被抢光");
        }

        // 4. Increment user's claim count
        redisUtil.increment(limitKey);
        redisUtil.expire(limitKey, 7, TimeUnit.DAYS);

        // 5. Generate coupon code and send to MQ
        String couponCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        CouponClaimMessage message = new CouponClaimMessage();
        message.setUserId(userId);
        message.setCouponId(couponId);
        message.setCouponCode(couponCode);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_COUPON,
                RabbitMQConfig.ROUTING_KEY_CLAIM,
                message);

        log.info("Flash claim queued: userId={}, couponId={}, code={}", userId, couponId, couponCode);
    }

    @Override
    public void syncStockToRedis(Long couponId) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) {
            throw new BusinessException("优惠券不存在");
        }
        int remaining = coupon.getTotalCount() - coupon.getClaimedCount();
        redisUtil.setString(STOCK_KEY_PREFIX + couponId, String.valueOf(Math.max(0, remaining)));
        log.info("Synced stock to Redis: couponId={}, remaining={}", couponId, Math.max(0, remaining));
    }

    private void initCouponStock(Long couponId, Integer totalCount) {
        redisUtil.setString(STOCK_KEY_PREFIX + couponId, String.valueOf(totalCount));
    }
}
