package kiwi;

public class ProducerDemo {
    public static void main(String[] args) {
        boolean isAsync = args.length == 0 || !args[0].trim().equalsIgnoreCase("sync");
        IpProducer producerThread = new IpProducer(KafkaProperties.TOPIC2, isAsync);
        producerThread.start();



    }
}
