package kiwi;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.ProcessorContext;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Pattern;

public class PhonenumStreamProcess  {
    private static String inputTopic = KafkaProperties.TOPIC2;
    private static String outputTopic = KafkaProperties.TOPIC3;


    private static class AnnoymizePhoneTransformer implements Transformer<byte[], String, KeyValue<byte[], String>> {

        private static Pattern phonePattern =
                Pattern.compile("(?<head>^((13[0-9])|(15[^4])|(18[0,2,3,5-9])|(17[0-8])))(?<middle>[0-9]{4})(?<tail>[0-9]{4})");
//                Pattern.compile("(?<keep>[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.)(?<anonymize>[0-9]{1,3})");

        @Override
        @SuppressWarnings("unchecked")
        public void init(ProcessorContext context) {
            // Not needed.
        }

        @Override
        public KeyValue<byte[], String> transform(final byte[] recordKey, final String recordValue) {
            // The record value contains the IP address in string representation.
            // The original record key is ignored because we don't need it for this logic.
            String anonymizedPhone = anonymizePhone(recordValue);
            return KeyValue.pair(recordKey, anonymizedPhone);
        }

        private String anonymizePhone(String phone) {
//            return phonePattern.matcher(ipAddress).replaceAll("${head}XXXX");
            StringBuilder op  = new StringBuilder(phone);

            op.setCharAt(12,'x');
            op.setCharAt(13,'x');
            op.setCharAt(14,'x');
            op.setCharAt(15,'x');
            return op.toString();

        }

        @Override
        public void close() {
            // Not needed.
        }

    }

    public static void main(String[] args) {
        StreamsBuilder builder = new StreamsBuilder();

        Properties streamsConfiguration = new Properties();
        streamsConfiguration.put(StreamsConfig.APPLICATION_ID_CONFIG, "PhoneNumStreamProcess");
        streamsConfiguration.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaProperties.KAFKA_SERVER_URL + ":" + KafkaProperties.KAFKA_SERVER_PORT);
        streamsConfiguration.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.ByteArray().getClass().getName());
        streamsConfiguration.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        streamsConfiguration.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        KStream<byte[], String> input = builder.stream(inputTopic);
        KStream<byte[], String> uppercasedAndAnonymized = input
                .mapValues(v -> v.toUpperCase())
                .transform(AnnoymizePhoneTransformer::new);
        uppercasedAndAnonymized.to(outputTopic);

        KafkaStreams streams = new KafkaStreams(builder.build(), streamsConfiguration);
        final CountDownLatch latch = new CountDownLatch(1);

        // attach shutdown handler to catch control-c
        Runtime.getRuntime().addShutdownHook(new Thread("streams-shutdown-hook") {
            @Override
            public void run() {
                streams.close();
                latch.countDown();
            }
        });

        try {
            System.out.println("Start streaming");
            streams.start();
            latch.await();
        } catch (Throwable e) {
            System.exit(1);
        }
            System.exit(0);
        }}


