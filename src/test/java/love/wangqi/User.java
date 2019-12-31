package love.wangqi;

import love.wangqi.stream.StreamInput;
import love.wangqi.stream.StreamOutput;
import love.wangqi.stream.Streamable;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

/**
 * @author: wangqi
 * @description:
 * @Version:
 * @date: Created in 2019/12/31 2:49 下午
 */
public class User implements Streamable {
    private int id;
    private String name;
    private short age;
    private List<Double> feature;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public short getAge() {
        return age;
    }

    public void setAge(short age) {
        this.age = age;
    }

    public List<Double> getFeature() {
        return feature;
    }

    public void setFeature(List<Double> feature) {
        this.feature = feature;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        id = in.readInt();
        name = in.readString();
        age = in.readShort();
        feature = DoubleStream.of(in.readDoubleArray()).boxed().collect(Collectors.toList());
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeInt(id);
        out.writeString(name);
        out.writeShort(age);
        out.writeDoubleArray(feature.stream().mapToDouble(Double::doubleValue).toArray());
    }
}
