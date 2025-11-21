package com.abel.ecommerce.service;

import com.abel.ecommerce.entity.Product;

// TODO: Change a name like stockServiceFrom cache because you manage it in redis cache
public interface StockService {

    public Long deductStock(Long productId, Integer quantity);

    public void restoreStock(Long productId, Integer quantity);

    public int getStock(Long productId);

    public Product findProductById(Long id);
}
