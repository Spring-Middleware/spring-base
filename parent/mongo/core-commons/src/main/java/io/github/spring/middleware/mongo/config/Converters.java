package io.github.spring.middleware.mongo.config;

import com.mongodb.lang.NonNull;
import org.bson.types.Binary;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.Jsr310Converters;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

@Configuration
public class Converters {

    @Bean
    public MongoCustomConversions mongoCustomConversions() {

        ArrayList<Converter<?, ?>> coverters = new ArrayList<Converter<?, ?>>();
        coverters.addAll(Jsr310Converters.getConvertersToRegister());
        coverters.add(DateToTimestamp.INSTANCE);
        coverters.add(DateToSqlDate.INSTANCE);
        coverters.add(BinaryToUuidConverter.INSTANCE);
        return new MongoCustomConversions(coverters);

    }

    @ReadingConverter
    public enum DateToTimestamp implements Converter<Date, Timestamp> {

        INSTANCE;

        @Override
        public Timestamp convert(Date source) {

            return new Timestamp(source.getTime());
        }
    }

    @ReadingConverter
    public enum DateToSqlDate implements Converter<Date, java.sql.Date> {

        INSTANCE;

        @Override
        public java.sql.Date convert(Date source) {

            return new java.sql.Date(source.getTime());
        }
    }

    @ReadingConverter
    public enum BinaryToUuidConverter implements Converter<Binary, UUID> {

        INSTANCE;

        @Override
        public UUID convert(@NonNull Binary binary) {

            ByteBuffer byteBuffer = ByteBuffer.wrap(binary.getData());
            long high = byteBuffer.getLong();
            long low = byteBuffer.getLong();
            return new UUID(high, low);
        }
    }

    public enum UuidToBinaryConverter implements Converter<UUID, Binary> {

        INSTANCE;

        @Override
        public Binary convert(@NonNull UUID uuid) {

            ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
            bb.putLong(uuid.getMostSignificantBits());
            bb.putLong(uuid.getLeastSignificantBits());
            return new Binary(bb.array());
        }
    }



}