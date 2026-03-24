package io.github.spring.middleware.kafka.api.data;

public class PublishResult<T,K> {

    private EventEnvelope<T> event;
    private K key;
    private String topic;
    private int partition;
    private long offset;


    public K getKey() {
        return key;
    }

    public void setKey(K key) {
        this.key = key;
    }

    public EventEnvelope<T> getEvent() {
        return event;
    }

    public void setEvent(EventEnvelope<T> event) {
        this.event = event;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public int getPartition() {
        return partition;
    }

    public void setPartition(int partition) {
        this.partition = partition;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public String getTraceId() {
        return event != null ? event.getTraceId() : null;
    }

    public String getEventId() {
        return event != null ? event.getEventId() : null;
    }
}
