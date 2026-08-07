package com.acc.gazua;

import com.acc.gazua.data.SearchRoom;
import com.acc.gazua.data.SearchStay;
import com.acc.gazua.data.SearchStayList;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
		"JWT_SECRET=this-is-dummy-secret-key-for-test-12345",
		"JWT_EXPIRATION_ACCESS=3600000",
		"JWT_EXPIRATION_REFRESH=604800000",
		"SERVICE_KEY=1234",
		"PORTONE_API_SECRET=1234"
})
class GazuaApplicationTests {

	//테스트가 실행되면서 모든 빈들이 메모리에 올라가게 되는데, 외부 API 요청 빈들도 실행됨
	//테스트에는 인증키를 가짜로 넣었기 때문에 권한 없음 오류가 생기면서 빌드가 안됨
	//그래서 가짜 Mock Bean 을 주입해서 실제 Bean 호출을 막아야 함
	@MockitoBean
	SearchRoom searchRoom;

	@MockitoBean
	SearchStayList searchStayList;

	@MockitoBean
	SearchStay searchStay;

	@Test
	void contextLoads() {
	}

}
