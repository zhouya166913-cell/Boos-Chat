package com.zhiyinhui.bosschat;

import com.zhiyinhui.bosschat.common.config.CorsProperties;
import com.zhiyinhui.bosschat.common.config.LlmProperties;
import com.zhiyinhui.bosschat.common.config.AgentWorkspaceProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({CorsProperties.class, LlmProperties.class, AgentWorkspaceProperties.class})
@MapperScan({
        "com.zhiyinhui.bosschat.system.mapper",
        "com.zhiyinhui.bosschat.ai.mapper",
        "com.zhiyinhui.bosschat.survey.mapper"
})
public class BossChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(BossChatApplication.class, args);
    }
}
