# Kafka Practice

## Producer and Consumer

### Producer——phone num producer

Producer负责随机生成电话号码并发送到Kafka的producer。

### Consumer

Consumer从某个topic中读出消息并打印。

### 配置：

在`KafkaProperties`中配置你自己的`Kafka`:

```java
public class KafkaProperties {
    public static final String TOPIC = "topic1";
    public static final String KAFKA_SERVER_URL = "localhost"; //你的kafka_server地址
    public static final int KAFKA_SERVER_PORT = 9002; //你的kafka_server端口
    public static final int KAFKA_PRODUCER_BUFFER_SIZE = 64 * 1024;
    public static final int CONNECTION_TIMEOUT = 100000;
    public static final String TOPIC2 = "telphone_input";
    public static final String TOPIC3 = "telphone_process_output";
    public static final String CLIENT_ID = "SimpleConsumerDemoClient";

    private KafkaProperties() {}
}

```

在`ProducerDemo`和`ConsumerDemo`中指定一致的`topic`

### 启动：

```bash
mvn compile 
mvn exec:java -Dexec.mainClass="kiwi.ProducerDemo" 
mvn exec:java -Dexec.mainClass="kiwi.ConsumerDemo" 
```

## Stream Processing

### Stream Processor:

从`producer`的`topic`中读出消息，并将其中消息变大写，电话号码中间位打码。

```java
...
 KStream<byte[], String> input = builder.stream(inputTopic);
        KStream<byte[], String> uppercasedAndAnonymized = input
                .mapValues(v -> v.toUpperCase())//所有消息内容变大写
                .transform(AnnoymizePhoneTransformer::new);//将消息内电话号码中间部分打码
        uppercasedAndAnonymized.to(outputTopic);//输出到outputtopic。
```

### 配置:

​	在`KafkaProperties`中配置你自己的`Kafka`:

```java
public class KafkaProperties {
    public static final String TOPIC = "topic1";
    public static final String KAFKA_SERVER_URL = "localhost"; //你的kafka_server地址
    public static final int KAFKA_SERVER_PORT = 9002; //你的kafka_server端口
    public static final int KAFKA_PRODUCER_BUFFER_SIZE = 64 * 1024;
    public static final int CONNECTION_TIMEOUT = 100000;
    public static final String TOPIC2 = "telphone_input";
    public static final String TOPIC3 = "telphone_process_output";
    public static final String CLIENT_ID = "SimpleConsumerDemoClient";

    private KafkaProperties() {}
}

```

`TOPIC2`为`producer`输入`topic`，`TOPIC3`为`stream process`的输出`topic`，也是`consumer`的输入`topic`.

### 启动：

```bash
mvn compile 
mvn exec:java -Dexec.mainClass="kiwi.ProducerDemo" 
mvn exec:java -Dexec.mainClass="kiwi.PhonenumStreamProcess" #stream process 要先于consumer启动
mvn exec:java -Dexec.mainClass="kiwi.ConsumerDemo" 
```

## 