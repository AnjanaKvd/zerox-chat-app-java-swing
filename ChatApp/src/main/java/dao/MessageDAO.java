package dao;

import model.Chat;
import model.Message;
import model.User;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;

public class MessageDAO {
    
    public void saveMessage(Message message) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            
            if (message.getId() == 0) {
                session.save(message);
            } else {
                session.update(message);
            }
            
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        }
    }
    
    public Message findById(int id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(Message.class, id);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public Message getLatestMessageForChat(int chatId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<Message> cr = cb.createQuery(Message.class);
            Root<Message> root = cr.from(Message.class);
            
            cr.select(root).where(cb.equal(root.get("chat").get("id"), chatId));
            cr.orderBy(cb.desc(root.get("timestamp")));
            
            Query<Message> query = session.createQuery(cr);
            query.setMaxResults(1);
            List<Message> results = query.getResultList();
            
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public int getUnreadMessageCountForUser(int chatId, int userId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT COUNT(m) FROM Message m WHERE m.chat.id = :chatId AND m.read = false AND m.sender.id != :userId";
            Query<Long> query = session.createQuery(hql, Long.class);
            query.setParameter("chatId", chatId);
            query.setParameter("userId", userId);
            
            Long count = query.getSingleResult();
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
    
    public List<Message> getMessagesForChat(int chatId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<Message> cr = cb.createQuery(Message.class);
            Root<Message> root = cr.from(Message.class);
            
            cr.select(root).where(cb.equal(root.get("chat").get("id"), chatId));
            cr.orderBy(cb.asc(root.get("timestamp")));
            
            Query<Message> query = session.createQuery(cr);
            return query.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
    }
    
    public void markMessagesAsRead(int chatId, int userId) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            
            String hql = "UPDATE Message m SET m.read = true WHERE m.chat.id = :chatId AND m.sender.id != :userId";
            Query<?> query = session.createQuery(hql);
            query.setParameter("chatId", chatId);
            query.setParameter("userId", userId);
            query.executeUpdate();
            
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        }
    }
} 