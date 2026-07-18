package com.dvFabricio.VidaLongaFlix;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "whatsapp.enabled=false",
        "admin.email=admin@vidalongaflix.com",
        "admin.password=AdminTest@123",
        "spring.cloud.aws.region.static=us-east-1",
        "spring.cloud.aws.credentials.access-key=test",
        "spring.cloud.aws.credentials.secret-key=test"
})
class VidaLongaFlixApplicationTests {

    @Test
    void contextLoads() {
    }

}
