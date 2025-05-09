package dao;

import model.Chat;
import model.User;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.nio.file.Files;

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
    
    /**
     * Deletes a chat completely, including its log file
     */
    public void deleteChat(int chatId) {
        Transaction transaction = null;
        String logFilePath = null;
        
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            
            Chat chat = session.get(Chat.class, chatId);
            if (chat != null) {
                // Get log file path before deleting
                logFilePath = chat.getLogFile();
                
                // Clear the subscriptions first
                String clearSubscriptionsHql = "DELETE FROM user_chat_subscriptions WHERE chat_id = :chatId";
                session.createNativeQuery(clearSubscriptionsHql)
                        .setParameter("chatId", chatId)
                        .executeUpdate();
                
                // Now delete the chat
                session.delete(chat);
                
                // Commit transaction
                transaction.commit();
            }
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            System.err.println("Database error deleting chat: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
        
        // Delete the log file outside the transaction
        if (logFilePath != null && !logFilePath.isEmpty()) {
            try {
                File logFile = new File(logFilePath);
                if (logFile.exists()) {
                    if (logFile.delete()) {
                        System.out.println("Chat log file deleted: " + logFilePath);
                    } else {
                        System.err.println("Failed to delete chat log file: " + logFilePath);
                        // Try alternative method
                        Files.deleteIfExists(logFile.toPath());
                    }
                } else {
                    System.err.println("Log file not found: " + logFilePath);
                }
            } catch (Exception e) {
                System.err.println("Error deleting log file: " + e.getMessage());
                e.printStackTrace();
                // Don't rethrow - we completed the DB transaction successfully
            }
        }
    }

    public List<Chat> getSubscribedChats(int userId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            User user = session.get(User.class, userId);
            if (user != null) {
                List<Chat> chats = new ArrayList<>(user.getSubscribedChats());
                return chats;
            }
            return java.util.Collections.emptyList();
        } catch (Exception e) {
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
    }

    public void subscribeUserToChat(int userId, int chatId) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            User user = session.get(User.class, userId);
            Chat chat = session.get(Chat.class, chatId);
            if (user != null && chat != null) {
                user.addSubscription(chat);
                session.update(user);
            }
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        }
    }

    public void unsubscribeUserFromChat(int userId, int chatId) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            User user = session.get(User.class, userId);
            Chat chat = session.get(Chat.class, chatId);
            if (user != null && chat != null) {
                user.removeSubscription(chat);
                session.update(user);
            }
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        }
    }

    /**
     * Gets just the IDs of chats a user is subscribed to
     */
    public List<Integer> getSubscribedChatIds(int userId) {
        List<Integer> chatIds = new ArrayList<>();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // Get the user with their subscribed chats
            User user = session.get(User.class, userId);
            if (user != null) {
                // Manual approach to avoid lazy loading issues
                String sql = "SELECT chat_id FROM user_chat_subscriptions WHERE user_id = :userId";
                Query<Object> query = session.createNativeQuery(sql);
                query.setParameter("userId", userId);
                
                // Convert results to integers manually
                List<Object> results = query.getResultList();
                for (Object obj : results) {
                    if (obj instanceof Number) {
                        chatIds.add(((Number) obj).intValue());
                    }
                }
            }
            return chatIds;
        } catch (Exception e) {
            e.printStackTrace();
            return chatIds; // Return empty list on error
        }
    }
}
