package kr.co.glnt.relay.common;

import kr.co.glnt.relay.dto.CarInfo;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class CommonUtils {

    public static void deleteImageFile(String fullPath) {
        Path filePath = Paths.get(fullPath);
        if (filePath.toFile().exists()) {
            try {
                Files.delete(filePath);
            } catch (IOException e) {
                log.error("{} 파일 삭제 실패: {}", fullPath, e.getMessage());
            }
        }
    }
}
