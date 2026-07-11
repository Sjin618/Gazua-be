package com.acc.gazua;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"JWT_SECRET=this-is-dummy-secret-key-for-test-12345", // 대문자 버전도 혹시 모르니 같이 넣어줍니다.
		"JWT_EXPIRATION_ACCESS=3600000",
		"JWT_EXPIRATION_REFRESH=604800000"
})
class GazuaApplicationTests {

	@Test
	void contextLoads() {
	}

}
