package com.zhiyinhui.bosschat.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bossChatOpenApi() {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes(
                        "Sa-Token",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("satoken")
                                .description("登录成功后返回的 token，直接粘贴 token 值即可")
                ))
                .info(new Info()
                        .title("企业管理助手 API")
                        .version("0.1.0")
                        .description("当前阶段已包含认证、用户管理、智能体管理、智能体对话与流式回复等基础能力。"));
    }
}
