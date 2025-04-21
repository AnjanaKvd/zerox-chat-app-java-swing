package dao;

import model.Chat;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Predicate;
import java.util.List;

public class ChatDAO {
    
    public void saveChat(Chat chat) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            
            if (chat.getId() == 0) {
                session.save(chat);
            } else {
                session.update(chat);
            }
            
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        }
    }
    
    public Chat findById(int id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(Chat.class, id);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public List<Chat> getAllChats() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<Chat> cr = cb.createQuery(Chat.class);
            Root<Chat> root = cr.from(Chat.class);
            cr.select(root);
            cr.orderBy(cb.desc(root.get("startTime")));
            
            Query<Chat> query = session.createQuery(cr);
            return query.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
    }
    
    public List<Chat> getActiveChats() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<Chat> cr = cb.createQuery(Chat.class);
            Root<Chat> root = cr.from(Chat.class);
            
            Predicate endTimeIsNull = cb.isNull(root.get("endTime"));
            cr.select(root).where(endTimeIsNull);
            cr.orderBy(cb.desc(root.get("startTime")));
            
            Query<Chat> query = session.createQuery(cr);
            return query.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
    }
    
    public void endChat(int chatId, String logFile) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            
            Chat chat = session.get(Chat.class, chatId);
            if (chat != null) {
                chat.setEndTime(new java.util.Date());
                chat.setLogFile(logFile);
                session.update(chat);
            }
            
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        }
    }
    
    public void deleteChat(int chatId) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            
            Chat chat = session.get(Chat.class, chatId);
            if (chat != null) {
                session.delete(chat);
            }
            
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        }
    }
}
