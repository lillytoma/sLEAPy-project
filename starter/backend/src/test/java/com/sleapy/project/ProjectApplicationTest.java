package com.sleapy.project;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
class ProjectApplicationTest {

	@Test
	void contextLoads() {
	}

}
