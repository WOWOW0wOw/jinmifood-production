package com.jinmifood.shop.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UploadServiceTest {
    @TempDir Path tempDir;

    @Test
    void rejectsFileWhoseContentsDoNotMatchImageType() {
        var service = new UploadService(tempDir.toString());
        var disguised = new MockMultipartFile("imageFile", "attack.jpg", "image/jpeg", "not an image".getBytes());

        assertThatThrownBy(() -> service.saveProductImage(disguised))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미지 형식");
    }

    @Test
    void acceptsDecodedPngAndUsesGeneratedFilename() throws Exception {
        var service = new UploadService(tempDir.toString());
        byte[] png = imageBytes("png");
        var image = new MockMultipartFile("imageFile", "product.png", "image/png", png);

        String url = service.saveProductImage(image);

        assertThat(url).matches("/uploads/product-[0-9a-f-]+\\.png");
        assertThat(tempDir.resolve(url.substring("/uploads/".length()))).exists();
    }

    @Test
    void rejectsJpegThatOnlyHasAValidHeader() {
        var service = new UploadService(tempDir.toString());
        byte[] fake={(byte)0xff,(byte)0xd8,(byte)0xff,0,1};
        var image=new MockMultipartFile("imageFile","fake.jpg","image/jpeg",fake);
        assertThatThrownBy(()->service.saveProductImage(image)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deletesOnlyGeneratedUploadPaths() throws Exception {
        var service=new UploadService(tempDir.toString());
        String url=service.saveProductImage(new MockMultipartFile("imageFile","product.png","image/png",imageBytes("png")));
        Path saved=tempDir.resolve(url.substring("/uploads/".length()));
        service.deleteProductImage(url);
        assertThat(saved).doesNotExist();
        Path unrelated=Files.writeString(tempDir.resolve("keep.txt"),"keep");
        service.deleteProductImage("/uploads/keep.txt");
        assertThat(unrelated).exists();
    }

    @Test
    void acceptsStructurallyValidWebp(){
        var service=new UploadService(tempDir.toString());
        byte[] webp=Base64.getDecoder().decode("UklGRjwAAABXRUJQVlA4IDAAAADQAQCdASoCAAIAAUAmJaACdLoB+AADsAD+8ut//NgVzXPv9//S4P0uD9Lg/9KQAAA=");
        String url=service.saveProductImage(new MockMultipartFile("imageFile","product.webp","image/webp",webp));
        assertThat(url).endsWith(".webp");
        assertThat(tempDir.resolve(url.substring("/uploads/".length()))).exists();
    }

    private byte[] imageBytes(String format) throws Exception {
        var out=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB),format,out);return out.toByteArray();
    }
}
