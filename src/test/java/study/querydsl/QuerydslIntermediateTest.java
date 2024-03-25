package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;

@SpringBootTest
@Transactional
public class QuerydslIntermediateTest {

    @Autowired
    EntityManager em;

    @Autowired
    JPAQueryFactory query;

    @BeforeEach
    public void before() {
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    public void simpleProjection() throws Exception {
        List<String> result = query
                .select(member.username)
                .from(member)
                .fetch();

        result.forEach(System.out::println);
    }


    @Test
    public void tupleProjection() throws Exception {
        List<Tuple> result = query
                .select(member.username, member.age)
                .from(member)
                .fetch();

        result.forEach(tuple -> {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);
        });
    }

    @Test
    public void findDtoByJPQL() throws Exception {
        List<MemberDto> result = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
                .getResultList();

        result.forEach(System.out::println);
    }

    @Test
    public void findDtoBySetter() throws Exception {
        List<MemberDto> result = query
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        result.forEach(System.out::println);
    }

    @Test
    public void findDtoByField() throws Exception {
        List<MemberDto> result = query
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        result.forEach(System.out::println);
    }

    @Test
    public void findDtoByConstructor() throws Exception {
        List<MemberDto> result = query
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        result.forEach(System.out::println);
    }

    @Test
    public void findUserDto() throws Exception {
        QMember memberSub = new QMember("memberSub");
        List<MemberDto> result = query
                .select(Projections.fields(MemberDto.class,
                        member.username.as("name"),
                        ExpressionUtils.as(JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub), "age"))
                )
                .from(member)
                .fetch();

        result.forEach(System.out::println);
    }

    @Test
    public void findUserDtoByConstructor() throws Exception {
        List<UserDto> result = query
                .select(Projections.constructor(UserDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        result.forEach(System.out::println);
    }

    @Test
    public void findDtoByQueryProjection() throws Exception {
        List<MemberDto> result = query
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        result.forEach(System.out::println);
    }

    @Test
    public void dynamicQueryBooleanBuilder() throws Exception {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    @Test
    public void dynamicQueryWhereParam() throws Exception {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    @Test
    public void bulkUpdate() throws Exception {

        //member1 = 10 -> DB 비회원 - 영속성 컨텍스트 member1
        //member2 = 20 -> DB 비회원 - 영속성 컨텍스트 member2
        //member3 = 30 -> DB member3 - 영속성 컨텍스트 member3
        //member4 = 40 -> DB member4 - 영속성 컨텍스트 member4

        long count = query
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();

        em.flush();
        em.clear();

        List<Member> result = query
                .selectFrom(member)
                .fetch();

        System.out.println(count);
        result.forEach(System.out::println);
    }

    @Test
    public void bulkAdd() throws Exception {
        long count = query
                .update(member)
                .set(member.age, member.age.add(1))
                .execute();

        long count2 = query
                .update(member)
                .set(member.age, member.age.multiply(2))
                .execute();

        System.out.println(count);
        System.out.println(count2);
    }

    @Test
    public void bulkDelete() throws Exception {
        long count = query
                .delete(member)
                .where(member.age.gt(18))
                .execute();
        System.out.println(count);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {
        BooleanBuilder builder = new BooleanBuilder();
        if (usernameCond != null)
            builder.and(member.username.eq(usernameCond));
        if (ageCond != null)
            builder.and(member.age.eq(ageCond));
        return query
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return query
                .selectFrom(member)
                .where(usernameEq(usernameCond), ageEq(ageCond))
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }

    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }
}
