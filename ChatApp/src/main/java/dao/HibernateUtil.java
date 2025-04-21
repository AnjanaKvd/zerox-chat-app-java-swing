package dao;

import model.User;
import model.Chat;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.util.Properties;

public class HibernateUtil {
    private static final SessionFactory sessionFactory;

    static {
        try {
            // Get environment variables
            String dbUrl = System.getenv().getOrDefault("DB_URL","jdbc:mysql://db-mysql-sgp1-25598-do-user-18305525-0.i.db.ondigitalocean.com:25060/chat_app");
            String dbUser = System.getenv().getOrDefault("DB_USERNAME", "doadmin");
            String dbPass = System.getenv().getOrDefault("DB_PASSWORD", "AVNS_IHK4Fv2v3wIvwF30A_3");

            // Validate presence
            if (dbUrl == null || dbUser == null || dbPass == null) {
                throw new RuntimeException("Database environment variables not set properly.");
            }

            // Set Hibernate properties
            Properties props = new Properties();
            props.setProperty("hibernate.connection.driver_class", "com.mysql.cj.jdbc.Driver");
            props.setProperty("hibernate.connection.url", dbUrl);
            props.setProperty("hibernate.connection.username", dbUser);
            props.setProperty("hibernate.connection.password", dbPass);
            props.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQL8Dialect");
            props.setProperty("hibernate.show_sql", "true");
            props.setProperty("hibernate.format_sql", "true");
            props.setProperty("hibernate.hbm2ddl.auto", "update");

            // Build configuration
            Configuration config = new Configuration();
            config.setProperties(props);
            config.addAnnotatedClass(User.class);
            config.addAnnotatedClass(Chat.class);

            // Build session factory
            sessionFactory = config.buildSessionFactory();
            System.out.println("✅ Hibernate initialized successfully.");
        } catch (Throwable ex) {
            System.err.println("❌ Hibernate SessionFactory creation failed: " + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }
}
