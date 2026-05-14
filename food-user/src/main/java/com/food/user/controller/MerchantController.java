package com.food.user.controller;

import com.food.common.core.constant.CommonConstant;
import com.food.common.core.result.R;
import com.food.user.dto.MerchantDTO;
import com.food.user.service.MerchantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/merchant")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    @PostMapping("/apply")
    public R<Map<String, String>> apply(@RequestHeader(CommonConstant.USER_ID_HEADER) Long userId,
                                        @RequestBody MerchantDTO dto) {
        return R.ok(merchantService.apply(userId, dto));
    }

    @GetMapping("/info")
    public R<MerchantDTO> getMerchantInfo(@RequestHeader(CommonConstant.USER_ID_HEADER) Long userId) {
        return R.ok(merchantService.getMerchantInfo(userId));
    }

    @PutMapping("/info")
    public R<Void> updateMerchantInfo(@RequestHeader(CommonConstant.USER_ID_HEADER) Long userId,
                                      @RequestBody MerchantDTO dto) {
        merchantService.updateMerchantInfo(userId, dto);
        return R.ok();
    }
}
