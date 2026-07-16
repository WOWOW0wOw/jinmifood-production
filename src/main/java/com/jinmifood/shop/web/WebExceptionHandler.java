package com.jinmifood.shop.web;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import java.util.NoSuchElementException;
import com.jinmifood.shop.service.PaymentException;
@ControllerAdvice
public class WebExceptionHandler {
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NoSuchElementException.class) String notFound(NoSuchElementException e,Model m){m.addAttribute("message",e.getMessage());return "error/404";}
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(PaymentException.class) String payment(PaymentException e,Model m){m.addAttribute("message",e.getMessage());m.addAttribute("code",e.getCode());return "store/payment-fail";}
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    @ExceptionHandler(MaxUploadSizeExceededException.class) String uploadTooLarge(Model m){m.addAttribute("message","이미지 용량이 너무 큽니다. 5MB 이하 파일을 선택해 주세요.");return "error/upload";}
}
