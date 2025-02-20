package com.core.jpa.types;

import com.core.converter.JsonConverter;
import org.apache.commons.beanutils.BeanUtils;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;
import java.util.Optional;

public abstract class JsonUserType<C extends Serializable> implements UserType<C> {

    protected abstract JsonConverter<C> getJsonConverter();

    @Override
    public int getSqlType() {

        return Types.VARCHAR;
    }

    public Class returnedClass() {

        return (Class) this.getClass().getAnnotatedSuperclass().getType();
    }

    public C nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session,
            Object owner) throws HibernateException, SQLException {

        C c = null;
        try {
            String json = rs.getString(position);
            if (json != null) {
                c = getJsonConverter().toObject(json);
            }
            return c;
        } catch (Exception ex) {
            throw new SQLException("Invalid json for  " + this.getClass(), ex);
        }
    }

    public void nullSafeSet(PreparedStatement st, C value, int index,
            SharedSessionContractImplementor session) throws HibernateException, SQLException {

        try {
            if (value == null) {
                st.setNull(index, Types.VARCHAR);
            } else {
                C c = value;
                String json = getJsonConverter().toString(c);
                st.setString(index, json);
            }
        } catch (Exception ex) {
            throw new SQLException(ex);
        }
    }

    @Override
    public boolean equals(C x, C y)
            throws HibernateException {

        return Objects.equals(x, y);
    }

    @Override
    public int hashCode(C x)
            throws HibernateException {

        return Objects.hashCode(x);
    }

    public C deepCopy(C value) throws HibernateException {

        return (C)Optional.ofNullable(value).map(v -> {
            try {
                return BeanUtils.cloneBean(v);
            } catch (Exception ex) {
                throw new HibernateException(ex);
            }
        }).orElse(null);
    }

    public boolean isMutable() {

        return true;
    }

    public Serializable disassemble(C value) throws HibernateException {

        return deepCopy(value);
    }

    public C assemble(Serializable cached, Object owner) throws HibernateException {

        return deepCopy((C)cached);
    }

    @Override
    public C replace(C original, C target, Object owner)
            throws HibernateException {

        return deepCopy(original);
    }
}
