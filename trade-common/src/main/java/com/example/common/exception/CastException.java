package com.example.common.exception;

import com.example.common.constant.ShopCode;

/**
 * 异常抛出类
 */
public class CastException {
    public static void cast(ShopCode shopCode) {
        throw new CustomerException(shopCode);
    }
}
