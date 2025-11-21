package com.abel.ecommerce.controller;

import com.abel.ecommerce.constant.RedisKeyConstants;
import com.abel.ecommerce.dto.request.OrderRequest;
import com.abel.ecommerce.dto.response.OrderResponse;
import com.abel.ecommerce.entity.Order;
import com.abel.ecommerce.entity.Product;
import com.abel.ecommerce.entity.SeckillMessage;
import com.abel.ecommerce.exception.InsufficientStockException;
import com.abel.ecommerce.exception.OrderNotFoundException;
import com.abel.ecommerce.service.SeckillService;
import com.abel.ecommerce.service.StockService;
import com.abel.ecommerce.utils.JwtTokenUtil;
import com.abel.ecommerce.utils.ResponseResult;
import com.abel.ecommerce.utils.ResultCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/seckill")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Seckill product", description = "Only for seckilling product")
public class SeckillController {

    private final StringRedisTemplate stringRedisTemplate;

    private final StockService stockService;

    private final SeckillService seckillService;

    @Operation(summary = "Create order for seckilling product", description = "Create a new order message for rabbitmq listener")
    @PostMapping("/{productId}")
    public ResponseResult<String> seckillProduct(
            @PathVariable Long productId,
            @Parameter(description = "User ID") @RequestParam Long userId,
            @Parameter(description = "Order data") @Valid @RequestBody OrderRequest request,
            HttpServletRequest httpRequest) {
        try {
            SeckillMessage seckillMessage = seckillService.doSeckill(userId, productId, 1);
            return ResponseResult.ok(ResultCode.SUCCESS);
        }
        catch (OrderNotFoundException e) {
            log.error("Order cannot be found", e);
            return ResponseResult.error(ResultCode.ORDER_NOT_EXIST.getCode(), e.getMessage());
        }
        catch (Exception e) {
            log.error("Exception: ", e);
            return ResponseResult.error(ResultCode.COMMON_FAIL.getCode(), e.getMessage());
        }
    }

}
