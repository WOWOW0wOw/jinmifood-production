package com.jinmifood.shop.service;

import com.jinmifood.shop.domain.SmsVerification;
import com.jinmifood.shop.domain.VerificationPurpose;
import com.jinmifood.shop.repository.SmsVerificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SmsVerificationServiceTest {
    SmsVerificationRepository repository=mock(SmsVerificationRepository.class);
    SmsSender sender=mock(SmsSender.class);
    BCryptPasswordEncoder encoder=new BCryptPasswordEncoder(4);
    SmsVerificationService service;
    AtomicReference<SmsVerification> latest=new AtomicReference<>();

    @BeforeEach void setUp(){
        when(repository.findFirstByPhoneAndPurposeOrderByCreatedAtDesc(anyString(),any())).thenAnswer(i->Optional.ofNullable(latest.get()));
        when(repository.save(any())).thenAnswer(i->{latest.set(i.getArgument(0));return i.getArgument(0);});
        service=new SmsVerificationService(repository,encoder,sender);
    }
    @Test void sendsSixDigitCodeAndStoresOnlyHash(){
        service.send("010-1234-5678",VerificationPurpose.REGISTER);
        var code=ArgumentCaptor.forClass(String.class);verify(sender).sendVerificationCode(eq("01012345678"),code.capture());
        assertThat(code.getValue()).matches("[0-9]{6}");assertThat(latest.get().getCodeHash()).doesNotContain(code.getValue());assertThat(encoder.matches(code.getValue(),latest.get().getCodeHash())).isTrue();
    }
    @Test void rejectsImmediateResend(){
        service.send("01012345678",VerificationPurpose.REGISTER);
        assertThatThrownBy(()->service.send("01012345678",VerificationPurpose.REGISTER)).hasMessageContaining("60초");
        verify(sender,times(1)).sendVerificationCode(anyString(),anyString());
    }
    @Test void locksAfterFiveWrongCodes(){
        service.send("01012345678",VerificationPurpose.RESET_PASSWORD);
        for(int i=0;i<5;i++)assertThat(service.verify("01012345678",VerificationPurpose.RESET_PASSWORD,"wrong!")).isFalse();
        assertThatThrownBy(()->service.verify("01012345678",VerificationPurpose.RESET_PASSWORD,"wrong!")).hasMessageContaining("입력 횟수");
    }
    @Test void acceptsCorrectCodeOnlyOnce(){
        service.send("01012345678",VerificationPurpose.FIND_EMAIL);
        var code=ArgumentCaptor.forClass(String.class);verify(sender).sendVerificationCode(anyString(),code.capture());
        assertThat(service.verify("01012345678",VerificationPurpose.FIND_EMAIL,code.getValue())).isTrue();
        assertThatThrownBy(()->service.verify("01012345678",VerificationPurpose.FIND_EMAIL,code.getValue())).hasMessageContaining("만료");
    }
    @Test void rejectsNonMobileNumber(){assertThatThrownBy(()->service.send("02-123-4567",VerificationPurpose.REGISTER)).isInstanceOf(IllegalArgumentException.class);}
}
