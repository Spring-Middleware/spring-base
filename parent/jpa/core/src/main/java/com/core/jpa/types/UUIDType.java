package com.core.jpa.types;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;
import java.util.UUID;

public class UUIDType implements UserType<UUID> {

    public int getSqlType() {

        return Types.VARCHAR;
    }

    public Class returnedClass() {

        return UUID.class;
    }

    public UUID nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session,
            Object ownner) throws HibernateException, SQLException {

        UUID uuid = null;
        try {
            String value = rs.getString(position);
            if (value != null) {
                uuid = UUID.fromString(value);
            }
            return uuid;
        } catch (Exception ex) {
            throw new SQLException("Invalid value for UUID ", ex);
        }
    }

    public void nullSafeSet(PreparedStatement st, UUID value, int index,
            SharedSessionContractImplementor session) throws HibernateException, SQLException {

        try {
            if (value == null) {
                st.setNull(index, Types.VARCHAR);
            } else {
                UUID uuid = (UUID) value;
                st.setString(index, uuid.toString());
            }
        } catch (Exception ex) {
            throw new SQLException(ex);
        }
    }

    @Override
    public boolean equals(UUID x, UUID y)
            throws HibernateException {

        return Objects.equals(x, y);
    }

    @Override
    public int hashCode(UUID x)
            throws HibernateException {

        return Objects.hashCode(x);
    }

    public UUID deepCopy(UUID value) throws HibernateException {

        return value == null ? null : UUID.fromString(value.toString());
    }

    public boolean isMutable() {

        return true;
    }

    public Serializable disassemble(UUID value) throws HibernateException {

        return (UUID) deepCopy(value);
    }

    public UUID assemble(Serializable cached, Object owner) throws HibernateException {

        return deepCopy((UUID) cached);
    }

    @Override
    public UUID replace(UUID original, UUID target, Object owner)
            throws HibernateException {

        return deepCopy(original);
    }

}
