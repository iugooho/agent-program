package {{packageName}}.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置：注册分页插件。
 *
 * <p>约定：实体与 Mapper 放在 infrastructure/persistence 包下，Service 只依赖领域接口。
 * 新写的 Mapper 记得加 @Mapper 注解，或在启动类上用 @MapperScan 指定包名。</p>
 */
@Configuration
public class MyBatisPlusConfig {

    /**
     * MyBatis-Plus 拦截器（当前只注册分页插件）。
     *
     * @return 拦截器
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.{{mybatisDbType}}));
        return interceptor;
    }
}
