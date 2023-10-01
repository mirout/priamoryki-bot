package com.priamoryki.discordbot.utils.sync;

import com.yandex.disk.rest.DownloadListener;
import com.yandex.disk.rest.RestClient;
import com.yandex.disk.rest.exceptions.ServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Pavel Lymar
 */
public class YaDiskFileLoader implements FileLoader {
    private final Logger logger = LoggerFactory.getLogger(YaDiskFileLoader.class);

    private final String dbLocalPath;
    private final String dbServerPath;
    private final RestClient cloudApi;

    public YaDiskFileLoader(String dbLocalPath, String dbServerPath, RestClient cloudApi) {
        this.dbLocalPath = dbLocalPath;
        this.dbServerPath = dbServerPath;
        this.cloudApi = cloudApi;
    }

    @Override
    public void load() {
        try {
            logger.info("downloading from YaDisk");
            if (new File(dbLocalPath).delete()) {
                cloudApi.downloadFile(
                        dbServerPath,
                        new File(dbLocalPath),
                        new DownloadListener() {
                            @Override
                            public OutputStream getOutputStream(boolean b) {
                                return null;
                            }
                        }
                );
            } else {
                System.err.printf("Can't delete file %s%n", dbLocalPath);
            }
        } catch (IOException | ServerException e) {
            logger.warn(e.toString());
        }
    }

    @Override
    public void upload() {
        try {
            logger.info("uploading to YaDisk");
            cloudApi.uploadFile(
                    cloudApi.getUploadLink(dbServerPath, true),
                    false,
                    new File(dbLocalPath),
                    new DownloadListener() {
                        @Override
                        public OutputStream getOutputStream(boolean b) {
                            return null;
                        }
                    }
            );
        } catch (IOException | ServerException e) {
            e.printStackTrace();
        }
    }
}
