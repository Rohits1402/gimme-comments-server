package io.github.rohits1402.gimmecomments;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestDatabase.class)
class GimmeCommentsServerApplicationTests {

    @Test
    void contextLoads() {
    }

}
