package study.querydsl.entity;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import static study.querydsl.entity.QHello.hello;

@SpringBootTest
@Transactional
@Commit
class HelloTest {

    @Autowired
    EntityManager em;

    @Autowired
    JPAQueryFactory query;

    @Test
    public void contextLoads() throws Exception {
        //given
        Hello h = new Hello();
        em.persist(h);

        //when
        Hello result = query
                .selectFrom(hello)
                .fetchOne();

        //then
        Assertions.assertThat(result).isEqualTo(h);
        Assertions.assertThat(result.getId()).isEqualTo(h.getId());
    }
}
