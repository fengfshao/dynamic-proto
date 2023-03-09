## 项目说明

运行时动态构建protobuf message的一种方案，无需protoc编译出源代码，可由`.proto`协议直接生成Message及字节数组。 <br>
> 在与下游使用protobuf协议交互的数据处理场景中，动态构建pb+配置远程化，
> 可以提高程序的灵活性，避免字段变更时的代码改动与重新编译部署。

## 使用示例
```java
public class Demo {
    public static void main(String[] args) {
        
        // 注册proto协议
        DynamicProtoBuilder.ProtoHolder.registerOrUpdate(
                Thread.currentThread().getContextClassLoader()
                        .getResource("simple_person.proto").openStream(), "simple_person.proto");

        // 准备要填充的数据
        Map<String, Object> fieldValues = new HashMap<>();
        fieldValues.put("id", 1);
        fieldValues.put("name", "jihite");
        fieldValues.put("email", "jihite@jihite.com");
        fieldValues.put("address", Arrays.asList("address1", "address2", "address3"));

        // 生成Message
        Message message = DynamicProtoBuilder
                .buildMessage("simple_person.proto", "SimplePersonMessage", fieldValues);
        byte[] data = message.toByteArray();
    }
}
```
更多示例见单元测试
