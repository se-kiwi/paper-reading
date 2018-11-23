## [B part1] 搭建kafka集群

### 平台及工具

- OS: Centos 7
- Container: Docker (使用Docker compose编排)

### `docker-compose`

```yml
version: '2'
services:
  zookeeper:
    image: wurstmeister/zookeeper
    ports:
      - "2181:2181"
  kafka:
    build: .
    ports:
      - "9092"
    environment:
      KAFKA_ADVERTISED_HOST_NAME: 192.168.1.107
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
  kafka-manager:  
    image: sheepkiller/kafka-manager
    environment:
        ZK_HOSTS: 192.168.1.107
    ports:
      - "9000:9000"
```

服务介绍：

1. zookeeper

    负责协调集群内部一致性，对外部暴露2181端口

2. kafka

    每个kafka服务是一个broker，在每个容器内部使用9092端口，如果只有一个broker，则对外暴露9092端口；如果有多个容器，则随机暴露某些端口。

    在这里没有指定image，而是使用当前目录下的Dockerfile构建镜像。

3. kafka-manager

    kafka-manager是一个可视化监控和管理kafka集群的工具。

### 运行方法

启动集群
```bash
docker-compose up -d
```
设置broker数目为`N`
```bash
docker-compose scale kafka=N
```

### 结果展示

**docker容器运行情况**
![docker-ps](pic/docker-ps.png)

**监控界面**
![manager](pic/manager.png)

**topic管理界面**
![topic-manager](pic/topic-manager.png)

### 参考文档

- [https://github.com/wurstmeister/kafka-docker](https://github.com/wurstmeister/kafka-docker)

## [D] 测试kafka性能

### 搭建测试环境

使用Jmeter做producer，发送message，同时开一个consumer，只是简单的获取message并丢弃。

步骤：

1. 编译jmeter的kafka插件([repo](https://github.com/BrightTag/kafkameter))，这个插件不是官方的，而且版本比较老，但是凑合着能用

2. 下载Jmeter 5.0，将上一步编译的`jar`文件放在`$JMETER_HOME/lib/ext`中

3. 启动Jmeter，编写测试脚本，指定broker、partition、key、msg等参数

4. 运行命令
    ```bash
    .\jmeter -n -t kafka.jmx -l kafka_report -e -o report-web
    ```

### 测试结果

|线程数|请求数|throughput(rec/s)|network(KB/s)|
|:---:|:---:|:---:|:---:|
|10|100|10869.57|212.30|
|50|1000|126968.00|2479.84|
|50|10000|141884.22|2771.18|
|100|10000|158077.77|3087.46|
|100|50000|140714.27|2748.33|
|100|100000|109791.18|2144.36|

由于测试环境为虚拟机，性能很有限，kafka表现出的性能不太好，每秒只能处理3MB左右的数据。