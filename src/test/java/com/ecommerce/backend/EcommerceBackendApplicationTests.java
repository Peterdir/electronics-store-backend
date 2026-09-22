package com.ecommerce.backend;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Vô hiệu hóa tạm thời để tránh lỗi CI do thiếu kết nối DB/Redis trên GitHub Actions")
class EcommerceBackendApplicationTests {

    @Test
    void contextLoads() {
    }

}
