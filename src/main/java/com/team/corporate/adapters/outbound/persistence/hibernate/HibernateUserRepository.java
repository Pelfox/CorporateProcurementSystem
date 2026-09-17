package com.team.corporate.adapters.outbound.persistence.hibernate;

import com.team.corporate.domain.models.User;
import com.team.corporate.domain.repository.UserRepository;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class HibernateUserRepository implements UserRepository {
    private final SessionFactory sessionFactory;

    public HibernateUserRepository(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public User add(User user) {
        Transaction transaction = null;

        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();

            session.persist(user);

            transaction.commit();
            return user;
        } catch (Exception e) {
            if (transaction != null && transaction.getStatus().canRollback()) {
                transaction.rollback();
            }
            throw new RuntimeException("При добавлении нового пользователя произошла ошибка:", e);
        }
    }

    @Override
    public List<User> getAll() {
        Transaction transaction = null;

        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();

            String sql = "FROM users";
            List<User> users = session.createQuery(sql, User.class).getResultList();

            transaction.commit();
            return users;
        } catch (Exception e) {
            if (transaction != null && transaction.getStatus().canRollback()) {
                transaction.rollback();
            }
            throw new RuntimeException("При получении пользователей произошла ошибка:", e);
        }
    }

    @Override
    public Optional<User> getById(UUID id) {
        Transaction transaction = null;

        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();

            User user = session.find(User.class, id);

            transaction.commit();
            return Optional.ofNullable(user);
        } catch (Exception e) {
            if (transaction != null && transaction.getStatus().canRollback()) {
                transaction.rollback();
            }
            throw new RuntimeException("При получении пользователей произошла ошибка:", e);
        }
    }

    @Override
    public User update(User user) {
        Transaction transaction = null;

        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();

            // TODO: Доделать метод update, перед этим определившись, что будет передаваться в update (User целиком или только обновляемые поля по одиночке)
            // TODO: Убрать из всех моделей (User, AuditLog, Order и т.д) геттер и сеттер на id, ибо возникает исключение PersistentObjectException.
            User userNew = session.merge(user);
            transaction.commit();
            return userNew;
        } catch (Exception e) {
            if (transaction != null && transaction.getStatus().canRollback()) {
                transaction.rollback();
            }
            throw new RuntimeException("При получении пользователей произошла ошибка:", e);
        }
    }

    @Override
    public void delete(UUID id) {
        Transaction transaction = null;

        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();

            User user = session.find(User.class, id);
            if (user != null) {
                session.remove(user);
            }
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null && transaction.getStatus().canRollback()) {
                transaction.rollback();
            }
            throw new RuntimeException("При получении пользователей произошла ошибка:", e);
        }
    }
}
