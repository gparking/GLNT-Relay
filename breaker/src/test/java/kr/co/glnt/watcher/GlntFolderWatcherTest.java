package kr.co.glnt.watcher;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

public class GlntFolderWatcherTest {

    @Test
    @DisplayName("폴더 유무 확인")
    public void pathValidate() {
        // given
        String directoryPath = "C:/tmp/test";

        // when
        Path path = Paths.get(directoryPath);

        // then
        System.out.println(Files.exists(path));
    }


    @Test
    @DisplayName("폴더 유무 확인 후 폴더 생성")
    public void createDirectory() throws Exception {
        // given
        String tmpDirectoryPath = "C:/tmp/test";


        // when
        Path path = Paths.get(tmpDirectoryPath);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }


        // then
        assertThat(true, is(Files.isDirectory(path)));
    }





}