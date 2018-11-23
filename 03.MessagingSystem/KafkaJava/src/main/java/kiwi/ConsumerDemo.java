package kiwi;

public class ConsumerDemo {
    public static void main(String[] args) {


        IpConsumer consumerThread = new IpConsumer(KafkaProperties.TOPIC3);
        consumerThread.start();
    }
}
