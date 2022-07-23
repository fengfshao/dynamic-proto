package com.fengfshao.dynamicproto;

import com.fengfshao.dynamicproto.pb3.MultiplePerson;
import com.fengfshao.dynamicproto.pb3.MultiplePerson.MultiplePersonMessage;
import com.fengfshao.dynamicproto.pb3.NestedPerson.NestedPersonMessage;
import com.fengfshao.dynamicproto.pb3.SimplePerson.SimplePersonMessage;
import com.fengfshao.dynamicproto.pb3.SimplePersonV2;
import com.fengfshao.dynamicproto.pb3.SimplePersonV2.SimplePersonMessageV2;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Message;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 动态编译生成proto message测试
 *
 * @author fengfshao
 */
@SuppressWarnings("unchecked")
public class DynamicProtoBuilderTest {

    /**
     * 测试简单无嵌套message的生成
     */
    @Test
    public void buildSimpleMessage() throws Exception {
        DynamicProtoBuilder.ProtoHolder.registerOrUpdate(
                Thread.currentThread().getContextClassLoader()
                        .getResource("simple_person.proto").openStream(), "simple_person.proto");

        Map<String, Object> fieldValues = new HashMap<>();
        fieldValues.put("id", 1);
        fieldValues.put("name", "jihite");
        fieldValues.put("email", "jihite@jihite.com");
        fieldValues.put("address", Arrays.asList("address1", "address2", "address3"));

        Message dynamicMessage = DynamicProtoBuilder
                .buildMessage("simple_person.proto", "SimplePersonMessage", fieldValues);

        SimplePersonMessage parsed = SimplePersonMessage.parseFrom(dynamicMessage.toByteArray());
        Assert.assertEquals(1, parsed.getId());
        Assert.assertEquals("jihite", parsed.getName());
        Assert.assertEquals("jihite@jihite.com", parsed.getEmail());
        Assert.assertEquals(Arrays.asList("address1", "address2", "address3"), parsed.getAddressList());
    }

    /**
     * 测试含有枚举类型的Message
     */
    @Test
    public void buildMessageWithEnum() throws Exception {
        DynamicProtoBuilder.ProtoHolder.registerOrUpdate(
                Thread.currentThread().getContextClassLoader()
                        .getResource("simple_personv2.proto").openStream(), "simple_personv2.proto");

        Map<String, Object> fieldValues = new HashMap<>();
        fieldValues.put("id", 1);
        fieldValues.put("name", "jihite");
        fieldValues.put("email", "jihite@jihite.com");
        fieldValues.put("gender", "MALE");
        fieldValues.put("address", Arrays.asList("address1", "address2", "address3"));

        Message dynamicMessage = DynamicProtoBuilder
                .buildMessage("simple_personv2.proto", "SimplePersonMessageV2", fieldValues);

        SimplePersonMessageV2 parsed = SimplePersonMessageV2.parseFrom(dynamicMessage.toByteArray());
        Assert.assertEquals(1, parsed.getId());
        Assert.assertEquals("jihite", parsed.getName());
        Assert.assertEquals("jihite@jihite.com", parsed.getEmail());
        Assert.assertEquals(Arrays.asList("address1", "address2", "address3"), parsed.getAddressList());
        Assert.assertEquals(SimplePersonV2.Gender.MALE, parsed.getGender());
    }

    /**
     * 测试含有嵌套类型的Message
     */
    @Test
    public void buildNestedMessage() throws Exception {
        DynamicProtoBuilder.ProtoHolder.registerOrUpdate(
                Thread.currentThread().getContextClassLoader()
                        .getResource("nested_person.proto").openStream(), "nested_person.proto");

        Map<String, Object> fieldValues = new HashMap<>();
        fieldValues.put("id", 1);
        fieldValues.put("name", "jihite");
        fieldValues.put("email", "jihite@jihite.com");
        fieldValues.put("gender", "FEMALE");
        fieldValues.put("address", Arrays.asList("address1", "address2", "address3"));

        Map<String, Object> pet1 = new HashMap<>();
        pet1.put("name", "jone");
        pet1.put("age", "3");

        Map<String, Object> pet2 = new HashMap<>();
        pet2.put("name", "Q");
        pet2.put("age", "5");

        //fieldValues.put("pets", Arrays.asList(pet1, pet2));

        Message dynamicMessage = DynamicProtoBuilder
                .buildMessage("nested_person.proto", "NestedPersonMessage", fieldValues);

        System.out.println(dynamicMessage);
        NestedPersonMessage parsed = NestedPersonMessage.parseFrom(dynamicMessage.toByteArray());
        Assert.assertEquals(1, parsed.getId());
        Assert.assertEquals("jihite", parsed.getName());
        Assert.assertEquals("jihite@jihite.com", parsed.getEmail());
        Assert.assertEquals(NestedPersonMessage.Gender.FEMALE, parsed.getGender());
        Assert.assertEquals(Arrays.asList("address1", "address2", "address3"), parsed.getAddressList());
        Assert.assertEquals(NestedPersonMessage.Dog.newBuilder()
                        .setName("jone").setAge(3).build()
                , parsed.getPets(0));
        Assert.assertEquals(NestedPersonMessage.Dog.newBuilder()
                        .setName("Q").setAge(5).build()
                , parsed.getPets(1));

        Assert.assertEquals(Arrays.asList("address1", "address2", "address3"), parsed.getAddressList());

    }

    /**
     * 测试多Message定义
     */
    @Test
    public void buildMultipleMessage() throws Exception {
        DynamicProtoBuilder.ProtoHolder.registerOrUpdate(
                Thread.currentThread().getContextClassLoader()
                        .getResource("multiple_person.proto").openStream(), "multiple_person.proto");

        Map<String, Object> fieldValues = new HashMap<>();
        fieldValues.put("id", 1);
        fieldValues.put("name", "jihite");
        fieldValues.put("email", "jihite@jihite.com");
        fieldValues.put("gender", "FEMALE");
        fieldValues.put("address", Arrays.asList("address1", "address2", "address3"));

        Map<String, Object> pet1 = new HashMap<>();
        pet1.put("name", "jone");
        pet1.put("age", "3");

        Map<String, Object> pet2 = new HashMap<>();
        pet2.put("name", "Q");
        pet2.put("age", "5");

        fieldValues.put("pets", Arrays.asList(pet1, pet2));

        Message dynamicMessage = DynamicProtoBuilder
                .buildMessage("multiple_person.proto", "MultiplePersonMessage", fieldValues);

        MultiplePersonMessage parsed = MultiplePersonMessage.parseFrom(dynamicMessage.toByteArray());
        Assert.assertEquals(1, parsed.getId());
        Assert.assertEquals("jihite", parsed.getName());
        Assert.assertEquals("jihite@jihite.com", parsed.getEmail());
        Assert.assertEquals(MultiplePerson.Gender.FEMALE, parsed.getGender());
        Assert.assertEquals(Arrays.asList("address1", "address2", "address3"), parsed.getAddressList());
        Assert.assertEquals(MultiplePerson.Dog.newBuilder()
                        .setName("jone").setAge(3).build()
                , parsed.getPets(0));
        Assert.assertEquals(MultiplePerson.Dog.newBuilder()
                        .setName("Q").setAge(5).build()
                , parsed.getPets(1));

        Assert.assertEquals(Arrays.asList("address1", "address2", "address3"), parsed.getAddressList());
    }

    @Test
    public void parseMessage() throws Exception {
        DynamicProtoBuilder.ProtoHolder.registerOrUpdate(
                Thread.currentThread().getContextClassLoader()
                        .getResource("nested_person.proto").openStream(), "nested_person.proto");

        MultiplePersonMessage.Builder builder = MultiplePersonMessage.newBuilder();
        builder.setName("jihite")
                .setEmail("jihite@jihite.com")
                .setGender(MultiplePerson.Gender.FEMALE);

        builder.addPets(MultiplePerson.Dog.newBuilder().setName("Q").build());
        byte[] data = builder.build().toByteArray();

        DynamicMessage message = DynamicProtoBuilder.parseMessage("nested_person.proto",
                "NestedPersonMessage", data);
        String name = (String) message.getAllFields()
                .get(message.getDescriptorForType().findFieldByName("name"));
        Assert.assertEquals("jihite", name);
        String email = (String) message.getAllFields()
                .get(message.getDescriptorForType().findFieldByName("email"));
        Assert.assertEquals("jihite@jihite.com", email);
        List<DynamicMessage> pets = (List<DynamicMessage>) message.getField(
                message.getDescriptorForType().findFieldByName("pets"));
        Assert.assertEquals("Q",
                pets.get(0).getField(pets.get(0).getDescriptorForType().findFieldByName("name")));
    }
}