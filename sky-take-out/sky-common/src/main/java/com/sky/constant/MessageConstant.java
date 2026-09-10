package com.sky.constant;

/**
 * 信息提示常量类
 */
public class MessageConstant {

    public static final String PASSWORD_ERROR = "密码错误";
    public static final String ACCOUNT_NOT_FOUND = "账号不存在";
    public static final String ACCOUNT_LOCKED = "账号被锁定";
    public static final String ALREADY_EXITS="已存在";
    public static final String UNKNOWN_ERROR = "未知错误";
    public static final String REQUEST_BODY_INVALID = "请求体格式错误,请检查提交的数据";
    public static final String USER_NOT_LOGIN = "用户未登录";
    public static final String CATEGORY_BE_RELATED_BY_SETMEAL = "当前分类关联了套餐,不能删除";
    public static final String CATEGORY_BE_RELATED_BY_DISH = "当前分类关联了菜品,不能删除";
    public static final String SHOPPING_CART_IS_NULL = "购物车数据为空，不能下单";
    public static final String ADDRESS_BOOK_IS_NULL = "用户地址为空，不能下单";
    public static final String LOGIN_FAILED = "登录失败";
    public static final String UPLOAD_FAILED = "文件上传失败";
    public static final String SETMEAL_ENABLE_FAILED = "套餐内包含未启售菜品，无法启售";
    public static final String PASSWORD_EDIT_FAILED = "密码修改失败";
    public static final String DISH_ON_SALE = "起售中的菜品不能删除";
    public static final String SETMEAL_ON_SALE = "起售中的套餐不能删除";
    public static final String DISH_BE_RELATED_BY_SETMEAL = "当前菜品关联了套餐,不能删除";
    public static final String ORDER_STATUS_ERROR = "订单状态错误";
    public static final String ORDER_NOT_FOUND = "订单不存在";
    public static final String DISH_ALREADY_STOP = "菜品已停售，无法加入购物车";
    public static final String SETMEAL_ALREADY_STOP = "套餐已停售，无法加入购物车";
    /** 地址不存在，或存在但不属于当前登录用户（统一提示，避免暴露他人数据是否存在） */
    public static final String ADDRESS_BOOK_NOT_FOUND = "地址不存在或无权操作";
    /** 下单时使用的收货地址不属于当前登录用户 */
    public static final String ADDRESS_BOOK_NOT_BELONG = "收货地址不存在或不属于当前用户";
    /** 上传图片格式不合法 */
    public static final String FILE_TYPE_NOT_ALLOWED = "仅支持上传 jpg/jpeg/png/gif 格式的图片";
    /** 上传文件过大 */
    public static final String FILE_TOO_LARGE = "上传文件大小不能超过 5MB";
    /** AOP 公共字段填充失败 */
    public static final String AUTO_FILL_FAILED = "公共字段自动填充失败";
    /** 报表模板文件缺失 */
    public static final String REPORT_TEMPLATE_NOT_FOUND = "报表模板文件不存在";
    /** 菜品不存在 */
    public static final String DISH_NOT_FOUND = "菜品不存在";
    /** 套餐不存在 */
    public static final String SETMEAL_NOT_FOUND = "套餐不存在";
    /** 员工不存在 */
    public static final String EMPLOYEE_NOT_FOUND = "员工不存在";

}
