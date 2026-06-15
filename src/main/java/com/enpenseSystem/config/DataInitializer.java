package com.enpenseSystem.config;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enpenseSystem.entity.SysUser;
import com.enpenseSystem.mapper.SysUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 应用启动时的数据初始化。
 *
 * <p>首次启动时自动创建预置管理员账号（用户名: admin，密码: admin123）。
 * 如果管理员已存在则跳过，不重复创建。</p>
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private static final String DEFAULT_ADMIN = "admin";
    private static final String DEFAULT_PASSWORD = "admin123";

    private final SysUserMapper sysUserMapper;

    public DataInitializer(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public void run(String... args) {
        Long count = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, DEFAULT_ADMIN));
        if (count != null && count > 0) {
            log.info("管理员用户已存在，跳过初始化");
            return;
        }

        SysUser admin = new SysUser();
        admin.setUsername(DEFAULT_ADMIN);
        admin.setPassword(BCrypt.hashpw(DEFAULT_PASSWORD));
        admin.setDisplayName("系统管理员");
        admin.setEnabled(true);
        sysUserMapper.insert(admin);

        log.info("已创建预置管理员用户: admin / admin123");
    }
}
