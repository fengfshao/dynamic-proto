package com.fengfshao.dynamicproto;

import com.github.os72.protobuf.dynamic.DynamicSchema;
import com.github.os72.protobuf.dynamic.EnumDefinition;
import com.github.os72.protobuf.dynamic.MessageDefinition;
import com.github.os72.protobuf.dynamic.MessageDefinition.Builder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.DynamicMessage;
import io.protostuff.compiler.ParserModule;
import io.protostuff.compiler.model.Enum;
import io.protostuff.compiler.model.Field;
import io.protostuff.compiler.model.Message;
import io.protostuff.compiler.parser.FileReader;
import io.protostuff.compiler.parser.Importer;
import io.protostuff.compiler.parser.ProtoContext;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 运行时动态构建pb message的一种方案，无需任何protoc编译 <br>
 *
 * <pre>
 * 示例，对如下proto协议：
 * message PersonMessage {
 *  int32 id = 1;
 *  string name = 2;
 *  string email = 3;
 *  repeated string address = 4;
 * }
 *
 * 通过如下方式，可构造对应的DynamicMessage，得到对应的字节数组
 *
 * Map<String, Object> fieldValues = new HashMap<>();
 * fieldValues.put("id", 1);
 * fieldValues.put("name", "jihite");
 * fieldValues.put("email", "jihite@jihite.com");
 * fieldValues.put("address",Arrays.asList("address1", "address2"));
 *
 * Message dynamicMessage = DynamicProtoBuilder
 *            .buildMessage("person.proto","PersonMessage",fieldValues);
 * </pre>
 *
 * @author fengfshao
 */
@SuppressWarnings("unchecked")
public class DynamicProtoBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicProtoBuilder.class);

    private static class InputStreamReader implements FileReader {

        private final InputStream in;

        public InputStreamReader(InputStream in) {
            this.in = in;
        }

        @Override
        public CharStream read(String name) {
            try {
                return CharStreams.fromStream(in);
            } catch (Exception e) {
                LOGGER.error("Could not read {}", name, e);
            }
            return null;
        }
    }

    public static class ProtoHolder {
        public static final ConcurrentHashMap<String, DynamicSchema> cache = new ConcurrentHashMap<>();

        public static void registerOrUpdate(byte[] protoBytes, String protoFileName)
                throws Exception {
            InputStream protoInputStream = new ByteArrayInputStream(protoBytes);
            DynamicSchema schema = parseProtoFile(protoInputStream);
            cache.put(protoFileName, schema);
        }

        public static void registerOrUpdate(InputStream protoInputStream, String protoFileName)
                throws Exception {
            DynamicSchema schema = parseProtoFile(protoInputStream);
            cache.put(protoFileName, schema);
        }
    }

    /**
     * 运行时解析proto文件，构造对应的DynamicSchema，用于后续构建DynamicMessage <br>
     *
     * @param protoInputStream proto协议输入流
     */
    private static DynamicSchema parseProtoFile(InputStream protoInputStream) throws Exception {
        Injector injector = Guice.createInjector(new ParserModule());
        Importer importer = injector.getInstance(Importer.class);

        ProtoContext context = importer.importFile(
                new InputStreamReader(protoInputStream), null);

        DynamicSchema.Builder schemaBuilder = DynamicSchema.newBuilder();

        context.getProto().getMessages().forEach(e -> {
            MessageDefinition msgDef = createMessageDefinition(e);
            schemaBuilder.addMessageDefinition(msgDef);
        });

        context.getProto().getEnums().forEach(e -> {
            EnumDefinition enumDef = createEnumDefinition(e);
            schemaBuilder.addEnumDefinition(enumDef);
        });
        protoInputStream.close();
        return schemaBuilder.build();
    }


    /**
     * 按照深度优先顺序，构造含有嵌套的MessageDefinition
     */
    private static MessageDefinition createMessageDefinition(Message message) {
        Builder builder = MessageDefinition.newBuilder(message.getName());
        for (Field f : message.getFields()) {
            if (!f.getType().isScalar() && !f.getType().isMessage()) {
                throw new UnsupportedOperationException("unsupported field type in proto.");
            }
            String label = f.isRepeated() ? "repeated" : "optional";
            builder.addField(label, f.getType().getName(), f.getName(), f.getIndex());
        }

        for (Message nestedMessage : message.getMessages()) {
            MessageDefinition nestedMsgDef = createMessageDefinition(nestedMessage);
            builder.addMessageDefinition(nestedMsgDef);
        }

        for (Enum e : message.getEnums()) {
            EnumDefinition enumDef = createEnumDefinition(e);
            builder.addEnumDefinition(enumDef);
        }

        return builder.build();
    }

    private static EnumDefinition createEnumDefinition(Enum e) {
        EnumDefinition.Builder builder = EnumDefinition.newBuilder(e.getName());
        e.getConstants().forEach(c -> {
            builder.addValue(c.getName(), c.getValue());
        });
        return builder.build();
    }

    /**
     * 将输入的字段根据pb的字段目标类型进行适配：
     * <pre>
     *  1. 标量值的自动转换
     *  2. 枚举类型的提取
     * </pre>
     *
     * @param fieldValue 传入的字段值
     * @param fd         pb字段引用
     * @return 符合pb类型的java类型字段值
     */
    private static Object getPBValue(Object fieldValue, FieldDescriptor fd, String protoName) {
        if (fieldValue == null) {
            return null;
        }
        FieldDescriptor.JavaType javaType = fd.getJavaType();
        switch (javaType) {
            case INT:
                if (fieldValue instanceof Integer) {
                    return fieldValue;
                } else {
                    return Integer.parseInt(String.valueOf(fieldValue));
                }
            case LONG:
                if (fieldValue instanceof Long) {
                    return fieldValue;
                } else {
                    return Long.parseLong(String.valueOf(fieldValue));
                }
            case FLOAT:
                if (fieldValue instanceof Float) {
                    return fieldValue;
                } else {
                    return Float.parseFloat(String.valueOf(fieldValue));
                }
            case DOUBLE:
                if (fieldValue instanceof Double) {
                    return fieldValue;
                } else {
                    return Double.parseDouble(String.valueOf(fieldValue));
                }
            case BOOLEAN:
                if (fieldValue instanceof Boolean) {
                    return fieldValue;
                } else {
                    return Boolean.parseBoolean(String.valueOf(fieldValue));
                }
            case STRING:
                if (fieldValue instanceof String) {
                    return fieldValue;
                } else {
                    return String.valueOf(fieldValue);
                }
            case ENUM:
                return fd.getEnumType().findValueByName(String.valueOf(fieldValue));
            case MESSAGE:
                Map<String, Object> fieldValues = (Map<String, Object>) fieldValue;
                return buildMessage(protoName, fd.getMessageType().getFullName(), fieldValues);
            default:
                // BYTE_STRING
                throw new UnsupportedOperationException(javaType.name() + " for " + fd.getName() + " not support yet!");
        }
    }

    /**
     * 动态构建proto message的接口
     *
     * @param messageName 生成的proto文件中的message名称
     * @param fieldValues 要填充的数据，字段名->字段值的映射<br>
     *                    字段名与proto协议一致，字段值的类型说明如下：
     *                    <ul>
     *                      <li>标量类型与java类型对应，如int32对应Integer，支持自适应解析转换，如String转int32</li>
     *                      <li>枚举类型对应枚举值的字符串</li>
     *                      <li>repeated类型对应java.util.ArrayList</li>
     *                      <li>嵌套的message字段为{@literal Map<String,Object>}</li>
     *                    </ul>
     * @return 生成的DynamicMessage
     */
    public static DynamicMessage buildMessage(String protoName, String messageName,
                                              Map<String, Object> fieldValues) {
        DynamicMessage.Builder msgBuilder =  Objects.requireNonNull(ProtoHolder.cache.get(protoName),
                        "use ProtoHolder#registerOrUpdate register your proto first!")
                .newMessageBuilder(messageName);
        Descriptor msgDesc = msgBuilder.getDescriptorForType();

        List<FieldDescriptor> fdList = msgDesc.getFields();

        fdList.forEach(fd -> {
            String fieldName = fd.getName();
            Object fieldValue = fieldValues.get(fieldName);
            if (fd.isRepeated()) {
                if (fieldValue != null) {
                    List<Object> values = (List<Object>) fieldValue;
                    for (Object ele : values) {
                        Object pbValue = getPBValue(ele, fd, protoName);
                        if (null != pbValue) {
                            msgBuilder.addRepeatedField(fd, pbValue);
                        }
                    }
                }
            } else {
                Object pbValue = getPBValue(fieldValue, fd, protoName);
                if (null != pbValue) {
                    msgBuilder.setField(fd, pbValue);
                }
            }
        });
        return msgBuilder.build();
    }

    public static DynamicMessage parseMessage(String protoName, String messageName, byte[] data) throws Exception {
        DynamicMessage.Builder msgBuilder = Objects.requireNonNull(ProtoHolder.cache.get(protoName),
                        "use ProtoHolder#registerOrUpdate register your proto first!")
                .newMessageBuilder(messageName);
        Descriptor msgDesc = msgBuilder.getDescriptorForType();
        return DynamicMessage.parseFrom(msgDesc, data);
    }
}
