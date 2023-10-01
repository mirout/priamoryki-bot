package com.priamoryki.discordbot.configs;

import com.priamoryki.discordbot.utils.sync.FileLoader;
import com.priamoryki.discordbot.utils.sync.YaDiskFileLoader;
import com.yandex.disk.rest.Credentials;
import com.yandex.disk.rest.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FileLoaderConfig {
    @Value("${db.path.path:data/servers.db}")
    public String dbLocalPath;
    @Value("${db.path.cloud:servers.db}")
    public String dbCloudPath;

    private final static String YADISK_TOKEN_ENV_NAME = "YADISK_TOKEN";

    public String getYaDiskToken() {
        return System.getenv(YADISK_TOKEN_ENV_NAME);
    }

    @Bean(value = "fileLoader", initMethod = "load")
    public FileLoader getFileLoader() {
        return new YaDiskFileLoader(
                dbLocalPath,
                dbCloudPath,
                new RestClient(new Credentials("me", getYaDiskToken()))
        );
    }
}
