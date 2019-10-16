package eu.telecomsudparis.jnvm.jpa;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;

@TestMethodOrder(OrderAnnotation.class)
class JPATests {
    private static EntityManagerFactory factory;
    private EntityManager em;

    private final static long PERSISTENT_KEY=1;
    private final static String PERSISTENT_VALUE="Hello Persistence!";

    @BeforeAll
    static void setupAll() {
        factory = Persistence.
            createEntityManagerFactory("testJPA", System.getProperties());
    }

    @BeforeEach
    void setup() {
        em = factory.createEntityManager();
    }

    @Test
    @Order(1)
    void persist() {
        em.clear();
        SampleObject<String> so1 = new SampleObject<String>(PERSISTENT_KEY,
                                                            PERSISTENT_VALUE);
        em.persist(so1);
        SampleObject<String> so2 = em.find(SampleObject.class, PERSISTENT_KEY);

        Assertions.assertEquals(so1, so2);
    }

    @Test
    @Order(2)
    void retrieve() {
        SampleObject<String> so = em.find(SampleObject.class, PERSISTENT_KEY);

        Assertions.assertNotNull(so);
    }

    @AfterEach
    void tearDown() {
        em.close();
    }

    @AfterAll
    static void tearDownAll() {
        factory.close();
    }

}
