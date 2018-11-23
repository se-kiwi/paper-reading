## 测试kafka性能

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

|线程数|请求数/线程|throughput(rec/s)|network(KB/s)|
|:---:|:---:|:---:|:---:|
|10|100|10869.57|212.30|
|50|1000|126968.00|2479.84|
|50|10000|141884.22|2771.18|
|100|10000|158077.77|3087.46|
|100|50000|140714.27|2748.33|
|100|100000|109791.18|2144.36|

由于测试环境为虚拟机，性能很有限，kafka表现出的性能不太好，每秒只能处理3MB左右的数据。