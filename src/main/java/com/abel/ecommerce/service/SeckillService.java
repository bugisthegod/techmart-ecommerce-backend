package com.abel.ecommerce.service;

import com.abel.ecommerce.entity.Product;
import com.abel.ecommerce.entity.SeckillMessage;

public interface SeckillService {

    SeckillMessage doSeckill(Long userId,Long productId, int quantity);

}
