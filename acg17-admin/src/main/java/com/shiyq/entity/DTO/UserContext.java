package com.shiyq.entity.DTO;

/**
 * 当前请求的用户上下文信息
 * 请求开始时创建，结束时销毁
 */
public final class UserContext {
    private static final ThreadLocal<Integer> user = new ThreadLocal<>();

    public static void add(Integer userId) {
        user.set(userId);
    }

    public static void remove() {
        user.remove();
    }

    /**
     * @return 当前登录用户的用户ID
     */
    public static Integer getCurrentUserId() {
        return user.get();
    }

    /**
     * 获取当前登录用户ID；私有资源接口必须使用此方法，避免用户上下文缺失时退化为全量查询。
     */
    public static int requireCurrentUserId() {
        Integer userId = user.get();
        if (userId == null) {
            throw new IllegalStateException("当前请求没有用户上下文");
        }
        return userId;
    }
}
