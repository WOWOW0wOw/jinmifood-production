package com.jinmifood.shop.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Service
public class UploadService {
    private static final long MAX_FILE_SIZE=5L*1024*1024, MAX_PIXELS=25_000_000;
    private static final int MAX_DIMENSION=10_000;
    private static final Map<String,String> ALLOWED_TYPES=Map.of(
        "image/jpeg","jpg", "image/png","png", "image/webp","webp"
    );
    private final Path uploadRoot;

    public UploadService(@Value("${app.upload.dir:./uploads}") String uploadDir){
        this.uploadRoot=Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public String saveProductImage(MultipartFile file){
        if(file==null||file.isEmpty())return null;
        String extension=ALLOWED_TYPES.get(file.getContentType());
        if(extension==null)throw new IllegalArgumentException("상품 이미지는 JPG, PNG, WEBP 파일만 등록할 수 있습니다.");
        if(file.getSize()>MAX_FILE_SIZE)throw new IllegalArgumentException("상품 이미지는 5MB 이하만 등록할 수 있습니다.");
        try{
            byte[] bytes=file.getBytes();
            if(bytes.length==0||bytes.length>MAX_FILE_SIZE)throw new IllegalArgumentException("상품 이미지는 5MB 이하만 등록할 수 있습니다.");
            validateImage(extension,bytes);
            Files.createDirectories(uploadRoot);
            String filename="product-"+UUID.randomUUID()+"."+extension;
            Path target=uploadRoot.resolve(filename).normalize();
            if(!target.getParent().equals(uploadRoot))throw new IllegalArgumentException("올바르지 않은 파일명입니다.");
            Files.write(target,bytes,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE);
            return "/uploads/"+filename;
        }catch(IllegalArgumentException e){throw e;
        }catch(IOException e){
            throw new IllegalStateException("이미지를 저장하지 못했습니다. 업로드 폴더 권한을 확인해 주세요.",e);
        }
    }

    public void deleteProductImage(String imageUrl){
        if(imageUrl==null||!imageUrl.startsWith("/uploads/product-"))return;
        String filename=imageUrl.substring("/uploads/".length());
        if(!filename.matches("product-[0-9a-fA-F-]+\\.(?:jpg|png|webp)"))return;
        Path target=uploadRoot.resolve(filename).normalize();
        if(!target.getParent().equals(uploadRoot))return;
        try{Files.deleteIfExists(target);}catch(IOException ignored){}
    }

    private void validateImage(String extension,byte[] bytes){
        if("webp".equals(extension)){validateWebp(bytes);return;}
        try(var stream=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))){
            if(stream==null)throw invalidImage();
            var readers=ImageIO.getImageReaders(stream);if(!readers.hasNext())throw invalidImage();var reader=readers.next();
            try{
                reader.setInput(stream,true,true);String format=reader.getFormatName().toLowerCase(Locale.ROOT);
                if(("jpg".equals(extension)&&!format.contains("jpeg"))||("png".equals(extension)&&!format.contains("png")))throw invalidImage();
                validateDimensions(reader.getWidth(0),reader.getHeight(0));if(reader.read(0)==null)throw invalidImage();
            }finally{reader.dispose();}
        }catch(IllegalArgumentException e){throw e;}catch(IOException|RuntimeException e){throw invalidImage();}
    }

    private void validateWebp(byte[] bytes){
        if(bytes.length<30||!ascii(bytes,0,"RIFF")||!ascii(bytes,8,"WEBP")||le32(bytes,4)+8!=bytes.length)throw invalidImage();
        int offset=12,width=0,height=0;
        while(offset+8<=bytes.length){
            long chunkSize=le32(bytes,offset+4),end=(long)offset+8+chunkSize;if(end>bytes.length)throw invalidImage();
            if(ascii(bytes,offset,"VP8X")&&chunkSize>=10){width=1+le24(bytes,offset+12);height=1+le24(bytes,offset+15);}
            else if(ascii(bytes,offset,"VP8L")&&chunkSize>=5&&u8(bytes[offset+8])==0x2f){int b1=u8(bytes[offset+9]),b2=u8(bytes[offset+10]),b3=u8(bytes[offset+11]),b4=u8(bytes[offset+12]);width=1+(((b2&0x3f)<<8)|b1);height=1+(((b4&0x0f)<<10)|(b3<<2)|((b2&0xc0)>>6));}
            else if(ascii(bytes,offset,"VP8 ")&&chunkSize>=10&&u8(bytes[offset+11])==0x9d&&u8(bytes[offset+12])==1&&u8(bytes[offset+13])==0x2a){width=(u8(bytes[offset+14])|(u8(bytes[offset+15])<<8))&0x3fff;height=(u8(bytes[offset+16])|(u8(bytes[offset+17])<<8))&0x3fff;}
            offset=(int)(end+(chunkSize&1));
        }
        if(offset!=bytes.length||width==0||height==0)throw invalidImage();validateDimensions(width,height);
    }
    private void validateDimensions(int width,int height){if(width<1||height<1||width>MAX_DIMENSION||height>MAX_DIMENSION||(long)width*height>MAX_PIXELS)throw new IllegalArgumentException("이미지 해상도는 최대 2,500만 화소, 가로·세로 10,000픽셀 이하만 등록할 수 있습니다.");}
    private boolean ascii(byte[] b,int o,String v){if(o<0||o+v.length()>b.length)return false;for(int i=0;i<v.length();i++)if(u8(b[o+i])!=v.charAt(i))return false;return true;}
    private long le32(byte[] b,int o){return (long)u8(b[o])|(long)u8(b[o+1])<<8|(long)u8(b[o+2])<<16|(long)u8(b[o+3])<<24;}
    private int le24(byte[] b,int o){return u8(b[o])|u8(b[o+1])<<8|u8(b[o+2])<<16;}
    private int u8(byte b){return b&0xff;}
    private IllegalArgumentException invalidImage(){return new IllegalArgumentException("파일 내용이 올바른 이미지 형식이 아닙니다.");}
}
