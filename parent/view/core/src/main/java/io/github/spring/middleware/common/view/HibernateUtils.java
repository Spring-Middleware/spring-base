package io.github.spring.middleware.common.view;

import org.hibernate.Hibernate;
import org.hibernate.proxy.HibernateProxy;

public class HibernateUtils {

    public static <T> T unproxy(Object proxy) {

        if (proxy instanceof HibernateProxy) {
            HibernateProxy hibernateProxy = (HibernateProxy) proxy;
            return (T) getImplemantionClass(hibernateProxy)
                    .cast(hibernateProxy.getHibernateLazyInitializer().getImplementation());
        } else {
            return (T) proxy;
        }
    }

    public static Class getImplemantionClass(HibernateProxy hibernateProxy) {

        return hibernateProxy.getHibernateLazyInitializer().getImplementation().getClass();
    }

    public static boolean isInstance(Class<?> finalClas, Object proxy) {

        return Hibernate.getClass(proxy).isAssignableFrom(finalClas) &&
                Hibernate.getClass(proxy).getSimpleName().equals(finalClas.getSimpleName());
    }

    public static <E> E cast(Class<E> finalClass, Object proxy) {

        return finalClass.cast(unproxy(proxy));
    }



}
